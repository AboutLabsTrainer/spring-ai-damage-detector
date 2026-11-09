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
