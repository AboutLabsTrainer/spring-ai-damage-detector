# Slice 03: The batch finishes despite Jev failures

**Story:** US-03 · **Job:** J1 · **Persona:** P1 Petra Lindner (illustrative) · **Estimate:** 1 day (reference class: per-item error handling plus a report field in an existing pipeline)

**Execution order:** 01 → 02 → **03** → 05 → 04. The refusal feedback that was first planned here moved to slice 05 (D24, user decision Q2, 2026-11-06).

## Goal
A transient Jev failure is retried with increasing waits. If the tool gives up, that image becomes REVIEW with the error as its description, and the batch continues and counts the errors.

## IN
- UC-1 (user-confirmed 2026-10-30): transient failures (timeout, connection error, HTTP 408/429/5xx, unreadable answer) are retried with increasing waits, default 3 attempts in total, configurable (DDD-7). Other HTTP 4xx errors are not retried: `Jev call failed after 1 attempt: HTTP <code> <reason>`.
- Q3 (user-confirmed 2026-11-06, amends UC-1): a 429 with `Retry-After` at or below `max-backoff` waits that long. Above it, the tool gives up at once with `Jev call failed after N attempt(s): HTTP 429 Too Many Requests (Retry-After <n> s exceeds max-backoff)` (DDD-6).
- Error → REVIEW, description `Jev call failed after N attempts: <reason>`, copy plus `.txt` in `review/`.
- Jev error count in the console line, report.md and report.json (per-image error flag).
- All-failed guard (D14, user-confirmed): the report is still written, the console names OPENROUTER_API_KEY and the network, exit 3.
- Evaluate mode counts errored images as not passed (at every swept threshold).
- INV-6 (D25, Q5): reports are written only at the end of a run, through a temp file and a rename. Exit 130, 143 or 137 means interrupted. The user reruns into a fresh output folder.

## OUT
- General refusals: arguments, folders, key, settings, unexpected exceptions (slice 05).
- Rerunning only the failed images, resuming an interrupted run, circuit breakers, rate-limit budgeting, parallel calls.
- Unreadable or corrupt local files (Jev rejects them, and that rejection is handled as a Jev error).

## Learning hypothesis
- **Disproves "retry plus REVIEW keeps a batch useful under real Jev conditions" if it fails:** a real run with injected outages still ends without a report, or the errored share is so high (> 20 % of images) that REVIEW becomes meaningless. That would point to a rate-limit or alpha-API problem that needs DESIGN attention.
- **Confirms if it succeeds:** unattended runs (including the 253-image evaluation) survive transient Jev failures.

## Acceptance criteria
1. Folder good/000.png (GOOD 0.91), good/042.png (503 on every attempt), bent_lead/000.png (BENT_LEAD) → 1 PASS, 1 SCRAP, 1 REVIEW, 1 Jev error. `review/good/042.png.txt` = `REVIEW: Jev call failed after 3 attempts: HTTP 503 Service Unavailable`.
2. 503 once, then BENT_LEAD → SCRAP, 0 Jev errors.
3. An image that fails on every attempt is never PASS.
4. 401 on every call for 4 images → 4 REVIEW, the console says `0 of 4 images judged by Jev - check OPENROUTER_API_KEY and network`, exit 3.
5. 429 with `Retry-After: 120` and a 10 s limit → REVIEW at once with `Jev call failed after 1 attempt: HTTP 429 Too Many Requests (Retry-After 120 s exceeds max-backoff)`. The remaining images are still inspected.
6. The API key appears in no error text, log or report.
7. A run interrupted before the last image leaves no partial report.md or report.json and ends with exit 130, 143 or 137.
8. **Production data:** a real run on 10 images from `images/` (5 good, 5 defects), with the network switched off for about 10 s during the run, completes with report.md and at least 1 Jev error. Record it below.

## Dependencies
- Walking skeleton. Independent of slices 01/02, but should land before long unattended runs and before slice 04 demos.

## Dogfood moment
Same day: Petra-style run on 10 real images with the network switched off for about 10 s.

## Evidence (fill after the run)
- Date: 2026-11-09. Real jar, 10 images copied (renamed) from `images/` (5 good, 1 bent_lead, 1 cut_lead, 1 damaged_case, 2 misplaced), real Jev through a local forwarding proxy; output under `out/` (not committed).
- Network loss: simulated at the local proxy (it was stopped for about 10 s mid-run, so the jar got "connection refused"), not at the host network.
- Summary line: `Inspected 10 images: 2 PASS, 1 SCRAP, 7 REVIEW (REVIEW share 70.0 %; 3 Jev errors in REVIEW), Jev cost $0.00103 (lower bound) -> out/slice03-run/report.md`. Exit 0, report.md and report.json written, the 3 errors read `Jev call failed after 3 attempts: connection error`.
- Cost: $0.00103 for this run (lower bound). Earlier trial runs, where the proxy only closed connections, were absorbed by the JDK client's own reconnects (0 Jev errors) and cost about $0.0015 each.
- The API key appears nowhere in the report, the log, stdout or stderr of this run.
- Exit 137 (manual `kill -9` on the real jar during a stalled Jev call, 8 s in): the process ended with 137 and the output folder held only `damage-detector.log`: no report.json, no report.md.
- Note: the 3 errors out of 10 are 30 %, above the 20 % hypothesis line, but the outage covered a third of the run by design.
