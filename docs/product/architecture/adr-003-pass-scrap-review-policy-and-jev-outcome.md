# ADR-003: PASS/SCRAP/REVIEW threshold policy and the sealed Jev outcome

## Status
Accepted with Option A (user, 2026-10-30). It extends the SPIKE pass rule (D20). 2026-10-30.

## Context
- C10 / INV-1: only images with no defect may pass, and an image whose Jev call failed must never be PASS.
- D20: PASS if and only if Jev chooses GOOD and P(GOOD) ≥ t; SCRAP if Jev chooses a defect; otherwise REVIEW. t = 0.86 by default.
- D13 adds failed calls, which become REVIEW with the error.
- D17: the sweep recomputes verdicts for t = 0.50…0.99 from one Jev pass.
- In the skeleton, `PassPolicy.judge(JevJudgement)` cannot express a failure. A failure aborts the batch through an exception.

## Decision
- Introduce a domain **sealed outcome**: `JevOutcome` is either `Judged(JevJudgement)` or `Failed(attempts, reason)`.
- The `Jev` port returns a `JevOutcome`. `PassPolicy` judges a `JevOutcome` with an exhaustive switch: `Failed` → REVIEW, `Judged` → the D20 rule. `Inspection` stores the outcome.
- With this design, the bug "a failed image passes" cannot be represented: no code path builds a PASS from `Failed`, and the compiler rejects a non-exhaustive switch.
- The sweep and all evaluation metrics are **pure functions** over the recorded `JevOutcome` per image. They re-apply `PassPolicy` at each t. The configured-t row and the headline use the same policy, so they agree by construction. Thresholds are handled as hundredths (0.50…0.99), and the configured t is always included even if it is off-grid.
- The threshold is never changed by the tool (D17). The user sets `--detector.pass-threshold`.

## Alternatives considered
1. **Keep exceptions and catch them in `BatchInspection`.** Each use case (batch and evaluate) would have to remember to catch the exception and build a REVIEW. That puts INV-1 in the hands of each caller, and one forgotten catch either reintroduces the abort or lets someone default to PASS. Rejected.
2. **Nullable judgement plus an error string on `Inspection`.** The compiler does not force callers to handle the error case, and `PassPolicy` could be called with null. Rejected.
3. **Retry flag inside the domain outcome** (`Failed(transient)`), so the adapter can decide retries from the outcome. Rejected: that leaks a transport concern into the domain. Transient/non-transient classification stays in `jev/` (ADR-004).

## Consequences
- Positive:
  - INV-1 and INV-4 (errors ≤ REVIEW) hold structurally.
  - D17 costs no extra calls.
  - The evaluation counts an errored image as not passed at every t, as US-02 requires.
- Negative:
  - Ripple refactor: `Inspection`, `BatchReport`, `ReportWriter` and the JSON shape change (a per-image `jevError` flag; choice and probabilities are absent for failed images).
  - The walking-skeleton scenario must keep passing, and its assertions stay unchanged. DISTILL extends the harness to capture stderr and the exit code: today it only calls `SpringApplication.run` in-process, but the new exit-code and INV-5 assertions need both.
