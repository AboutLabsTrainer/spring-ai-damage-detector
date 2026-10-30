# Slice 03: The batch finishes despite Jev failures

**Story:** US-03 · **Job:** J1 · **Persona:** P1 Petra Lindner (illustrative) · **Estimate:** ≤1 day (reference class: per-item error handling plus a report field in an existing pipeline)

## Goal
A failed Jev call is retried with increasing waits. If it still fails, that image becomes REVIEW with the error as its description, and the batch continues and counts the errors.

## IN
- Retry with increasing waits, default 3 attempts in total, configurable.
- Error → REVIEW, description `Jev call failed after N attempts: <reason>`, copy plus `.txt` in `review/`.
- Jev error count in the console line, report.md and report.json (per-image error flag).
- All-failed guard (D14, PO-proposed): the report is still written, the console names OPENROUTER_API_KEY and the network, and the exit code is non-zero.
- Evaluate mode counts errored images as not passed (at every swept threshold).
- D23/INV-5: missing arguments, a missing input folder, an input folder without images, and any unexpected exception each give one actionable message with no stack trace and a non-zero exit code.

## OUT
- Rerunning only the failed images, circuit breakers, rate-limit budgeting, parallel calls.
- Unreadable or corrupt local files (Jev rejects them, and that rejection is handled as a Jev error).

## Learning hypothesis
- **Disproves "retry plus REVIEW keeps a batch useful under real Jev conditions" if it fails:** a real run with injected outages still ends without a report, or the errored share is so high (> 20 % of images) that REVIEW becomes meaningless. That would point to a rate-limit or alpha-API problem that needs DESIGN attention.
- **Confirms if it succeeds:** unattended runs (including the 253-image evaluation) survive transient Jev failures.

## Acceptance criteria
1. Folder good/000.png (GOOD 0.91), good/042.png (503 on every attempt), bent_lead/000.png (BENT_LEAD) → 1 PASS, 1 SCRAP, 1 REVIEW, 1 Jev error. `review/good/042.png.txt` = `REVIEW: Jev call failed after 3 attempts: HTTP 503 Service Unavailable`.
2. 503 once, then BENT_LEAD → SCRAP, 0 Jev errors.
3. An image that fails on every attempt is never PASS.
4. 401 on every call for 4 images → 4 REVIEW, the console says `0 of 4 images judged by Jev - check OPENROUTER_API_KEY and network`, non-zero exit.
5. The API key appears in no error text, log or report.
6. **Production data:** a real run on 10 images from `images/` (5 good, 5 defects) with a failure forced for part of the run (e.g. network briefly cut) completes with report.md and a non-zero error count. Record it below.

## Dependencies
- Walking skeleton. Independent of slices 01/02, but should land before long unattended runs and before slice 04 demos.

## Dogfood moment
Same day: Petra-style run on 10 real images with the Wi-Fi switched off for a few seconds.

## Evidence (fill after the run)
- Summary line: _pending_
