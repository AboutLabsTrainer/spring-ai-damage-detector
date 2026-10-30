# ADR-004: Jev failure resilience with a retry decorator and REVIEW fallback

## Status
Accepted with Option A (user, 2026-10-30). UC-1 and UC-4 were confirmed on 2026-10-30. Revised on 2026-10-30 for review iteration 2 and the Spring Boot 4.1 upgrade (ADR-006).

## Context
- **US-03 / D13:** a failed Jev call (timeout, connection error, HTTP error, unreadable answer) is retried with increasing waits, 3 attempts in total by default and configurable. If every attempt fails, the image becomes REVIEW with `Jev call failed after N attempts: <reason>`, and the batch continues. The errors are counted.
- **D14:** if 0 images were judged, the report is still written and the exit code is non-zero.
- **Out of scope (slice 03):** circuit breakers, rate-limit budgeting and parallel calls.
- **K5 guardrail:** ≤ 10 min per 253 images. The SPIKE mean was 1.2 s per image.
- **Platform:** Spring Boot 4.1 brings Spring Framework 7, which includes core retry support (`RetryTemplate`, `@Retryable`).
- **The alpha API may return structurally valid JSON with invalid content:** an unknown class, or a null or out-of-range probability. INV-1 needs a valid P(GOOD) before any PASS.

## Decision
- **A plain decorator on the `Jev` port.**
  - `RetryingJev` is the only `Jev` implementation. It wraps `OpenRouterJev`, which **does not implement `Jev`**.
  - `OpenRouterJev` performs exactly one attempt and returns a package-private sealed `jev/AttemptResult`: `Answered(JevJudgement)`, `Transient(reason)` or `Permanent(reason)`. The classification lives only in `jev/`, so the domain `JevOutcome` stays free of it (ADR-003, alternative 3).
  - `RetryingJev` maps the attempt result to a domain outcome:
    - `Answered` → `Judged`
    - `Transient` → back off and retry while attempts remain, otherwise `Failed(attempts, reason)`
    - `Permanent` → `Failed(attempts, reason)` at once
- **A valid answer is checked at attempt mapping, in `jev/`.**
  - `choice` must be one of `GOOD`, `MISPLACED`, `DAMAGED_CASE`, `CUT_LEAD`, `BENT_LEAD`.
  - `probabilities.GOOD` must be non-null, finite and within [0, 1].
  - Anything else becomes `Transient("unreadable answer")`. So does an empty or unparsable body, or a missing `answers.defect`. The attempt is retried, and if the problem persists the image becomes REVIEW.
  - A missing confidence becomes 0. A missing cost becomes 0 (unknown).
  - Required WireMock cases: null `probabilities`, P(GOOD) = 1.7, P(GOOD) = NaN or missing, `choice` = `SCRATCHED` (unknown class), missing `answers.defect`, a non-JSON body.
- **Which failures are retried:**
  - Transient (retried): connection errors, timeouts, HTTP 408, 429 and 5xx, an unreadable answer.
  - Permanent (not retried): every other 4xx.
- **Backoff:**
  - Exponential: `initial-backoff` (1s), doubling, capped at `max-backoff` (10s).
  - On a 429 whose `Retry-After` is at most `max-backoff`, the next attempt waits that long.
  - On a 429 whose `Retry-After` is above the cap, there is **no early retry**. The attempt counts as transient, but the decorator gives up immediately: `Failed(attempts, "HTTP 429 Too Many Requests (Retry-After <n> s exceeds max-backoff)")`.
- **Configuration:**
  - `detector.jev.retry.max-attempts` (3, ≥ 1), `.initial-backoff` (1s), `.max-backoff` (10s).
  - `detector.jev.connect-timeout` (10s) and `detector.jev.read-timeout` (**30s**). The composition root applies the timeouts to the Jev `RestClient` request factory. This does not depend on Boot's global `spring.http.client*` property names, which changed in Boot 4 (ADR-006).
- **Waiting is an injected sleeper capability.** The composition root always wires `RetryingJev(OpenRouterJev)` (ArchUnit rule 5).
- **Reason text** comes from the status code and reason phrase or the exception type, never from request data (INV-3).
- **Cost:**
  - Only `usage.cost` of the judged attempt is summed.
  - The cost of failed attempts (timeouts, errors, invalid answers) is **unknown and not counted**: OpenRouter may still bill them, but they carry no usable `usage.cost`.
  - When there were retries or Jev errors, the console line and report.md state that the reported cost is a lower bound.

## Alternatives considered
1. **Spring Framework 7 core retry** (`RetryTemplate` / `@Retryable` with `@EnableResilientMethods`, available after ADR-006). Rejected for the decorator body:
   - It is exception-driven, while the attempt result here is a value (`AttemptResult`). Using it would mean throwing in order to retry.
   - Honouring `Retry-After` and the above-the-cap give-up rule needs custom policy code anyway.
   - The decorator is about 30 lines and tested with an instant sleeper.
   - It can be revisited without touching the port.
2. **Spring Retry** (`spring-retry`, `@Retryable` + `@Recover`, AOP). Rejected: it is in maintenance mode and superseded by Framework 7 core retry, it is no longer a natural fit for Boot 4, and it adds 2 dependencies.
3. **RestClient `ClientHttpRequestInterceptor` with retry.** Rejected: it works below response mapping, so it cannot retry an unreadable or invalid answer, and it cannot return a domain `Failed`.
4. **Resilience4j Retry** (Apache 2.0). Rejected: circuit breaking is out of scope, and a library is not justified for a sequential single-client loop.
5. **Retry every HTTP error, including 4xx.** Rejected for 4xx other than 408/429: no gain, and at least 3 s wasted per image on a revoked key (UC-1, confirmed).

## Consequences
- Positive:
  - No new runtime dependency.
  - Deterministic tests.
  - INV-1 holds even for invalid answers.
  - A 401 run ends quickly with every image in REVIEW and exit 3.
- Negative:
  - The hand-written backoff needs unit tests: attempt count, cap, `Retry-After` handling, classification.
  - **Timeout budget (K5):**
    - One hanging image costs 3 × 30 s + 1 s + 2 s = 93 s instead of 1.2 s, about 91.8 s extra.
    - A clean run takes 253 × 1.2 s ≈ 304 s, so the remaining budget is ≈ 296 s.
    - Break-even is **3 hanging images**: 3 take ≈ 579 s and stay within K5, 4 take ≈ 671 s and break it.
    - A larger outage is a degraded run that still completes (K3).
    - With the old 120 s timeout, a single hanging image already broke K5 (≈ 11.1 min).
    - The US-03 example text becomes "timeout after 30 s" (UC-4, confirmed).
  - The reported cost is a lower bound on runs with failed attempts.
