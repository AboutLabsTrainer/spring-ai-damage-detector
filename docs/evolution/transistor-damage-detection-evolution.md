# Evolution: transistor-damage-detection

Delivered 2026-11-09 on branch `feature/transistor-damage-detection` (no remote). Full record: `docs/feature/transistor-damage-detection/feature-delta.md` (Wave: DELIVER sections) and `slices/`.

## What shipped

- `--evaluate` on a labelled set with threshold sweep (US-01, US-02).
- Retry decorator and REVIEW fallback so a Jev failure never produces a PASS (US-03).
- One-line refusals with exit codes, bootstrap probes (US-05).
- Docker image and `scripts/container-check.sh`; CI workflow (US-04).
- Default pass threshold 0.87.

## Key decisions

- Modular monolith CLI; Jev via the OpenRouter Decisions API (ADR-001..006, `docs/product/architecture/`).
- Threshold raised 0.86 -> 0.87 after the sweep (user decision).
- Cucumber-only classes outside the PIT gate (user decision 2026-11-06); jqwik removed.
- Docker scenarios run in `container-check.sh`, not `verify`.

## Metrics

- 14/14 steps done; 653 + 89 tests green; 76 scenarios + walking skeleton, 10 `@container` green.
- PIT 95 % (246/259) on the agreed scope.
- Real run, 253 images at 0.87: 0 false passes, good auto-pass 34.3 %, cost $0.03724, 163-166 s.
- KPI baselines: `docs/product/kpi-contracts.yaml` (`baseline_measured`).

## Lessons

- Jev output varies run to run (good auto-pass 73-99 of 213), so thresholds need margin and re-measurement.
- Crafters logged phases at commit time (B3); the DES log does not prove phase order. Log each phase when it happens.
- Work after the roadmap steps (fixes, review revisions) falls outside step logging; plan a final step for it.
- A test-model defect (`RetryingJevAttemptLoopTest`) surfaced only in 04-01; check test models before implementing.
- Adversarial review found 4 test/log blockers after all steps passed; one revision pass fixed them.

## Artifacts

- Architecture brief: `docs/product/architecture/brief.md`; KPI contracts: `docs/product/kpi-contracts.yaml`.
- Not migrated: `design/`, `distill/` hold no files in the skill's destination map; the feature directory stays as delivery history.

## Retrospective (5 Whys)

Evidence: `feature-delta.md` DELIVER deviations (c, d), DESIGN peer-review log, execution log, commits 601d632, ae8d097..020c85e.

| # | Issue | Why chain (short) | Root cause | Countermeasure (next feature) |
|---|-------|-------------------|------------|-------------------------------|
| 1 | TDD phases logged at commit time in 7/14 steps | Log shows RED/GREEN/COMMIT stamped together -> crafters wrote the log after the work -> logging is a manual crafter duty -> nothing checks timestamps or order -> the log is treated as paperwork, not as a gate | The DES log has no mechanical check of phase order; compliance depends on crafter discipline (Sonnet crafters from 03-01 on, rigor switched mid-feature) | Add a pre-commit/step-close check that rejects a step whose RED, GREEN and COMMIT timestamps are not strictly increasing by at least a few seconds, and require the failing-test output line in the RED entry. |
| 2 | `RetryingJevAttemptLoopTest` model defect blocked 04-01 | Crafter could not make it green -> the test's model of the retry loop contradicted the design -> the test was authored in DISTILL before DESIGN's late revisions (DDD-30..37) settled the retry contract -> nobody ran it against a stub -> pre-authored tests are only reviewed by reading | Pre-authored tests were never executed against a reference implementation or checked for consistency with the final design before DELIVER | At the end of DISTILL, run each pre-authored test class once against a throwaway minimal implementation (or `@Disabled` skeleton plus a model walk-through) and list each test's model assumptions in the step file. Check them in the first roadmap step touching the class. |
| 3 | Adversarial review found weak tests after DISTILL's four-reviewer gate | Circular oracle, zero-assertion, tautology and test of an uncalled method slipped through -> reviewers read scenarios and test names, not assertion quality -> the gate checklist is about coverage and wording, not oracle independence -> weak tests only show up when mutated or when production callers are checked | The test gate has no mechanical test-quality probe and no independent-oracle rule; DISTILL had no running production code, so mutation and call-graph checks were impossible | Add three cheap greps/rules to the DISTILL gate and to CI: every test method has an assertion (ArchUnit or script), expected values are literals not computed by the code under test, and every production method used by tests has a production caller. Run PIT on the first two steps, not only at the end. |
| 4 | Secrets could be echoed (`Jev.toString` baseUrl, unknown CLI argument) | Both values were copied into output for convenience -> neither was classified as sensitive -> no secrets inventory existed in DESIGN (only the API key was treated as secret; baseUrl and raw args can carry credentials) -> echoing is the default way to write messages | Secret handling was defined by a single named value (the key), not by a data-flow rule covering all user-supplied strings that reach logs, messages or toString | In DESIGN, add a rule "user-supplied strings are never echoed verbatim" plus a scenario that runs the jar with a canary secret in every input (arg, base URL, env) and greps stdout, stderr and the reports for it. Keep it as a permanent test. |
| 5 | DESIGN rejected twice, then needed a revision cycle at the gate (DISCUSS needed attempt 2) | Reviews found critical/high gaps late -> the first drafts were reviewed as complete documents -> upstream decisions (D-numbers, Q&A) and design were written in one pass without a self-check against a defined checklist -> reviewers act as the first real check -> each wave starts review at full size | Review was the first quality check; authors did not self-check against the reviewers' criteria, and large drafts were reviewed in one batch | Before each review, the author runs the reviewer's checklist (copy it into the wave template) and requests review per section (aggregates, ports, error paths) rather than per document. Record finding categories per round to see which checks to move earlier. |

Note: the DISTILL round count is not in the logs read; item 5 rests on the DESIGN and DISCUSS records.
