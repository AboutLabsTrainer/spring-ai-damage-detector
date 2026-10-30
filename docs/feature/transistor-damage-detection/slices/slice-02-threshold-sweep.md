# Slice 02: Threshold sweep and safe threshold

**Story:** US-02 · **Job:** J2 · **Persona:** P3 Jonas Berger (illustrative) · **Estimate:** ½ day (reference class: a pure recomputation over data already collected in slice 01)

## Goal
The evaluation shows, for every threshold from 0.50 to 0.99, the false passes and the good auto-pass rate, and names the lowest threshold with zero false passes.

## IN
- Sweep from the single Jev pass of slice 01 (no extra calls).
- `evaluation.json`: all thresholds in steps of 0.01. `evaluation.md`: 0.80–0.95 plus the configured t.
- The line `Lowest threshold with 0 false passes: <t>`, or `No threshold up to 0.99 reaches 0 false passes; route every GOOD to REVIEW`.

## OUT
- Changing the configured threshold automatically. The user sets `--detector.pass-threshold`.
- Confidence intervals, ROC plots, repeated runs.

## Learning hypothesis
- **Disproves "a single fixed threshold can separate defects from good parts with useful auto-pass" if it fails:** the lowest safe threshold auto-passes < 25 % of the 213 good images, or no safe threshold exists. In that case the showcase must present "every GOOD goes to REVIEW" or reopen the anomaly-detection fallback (DISCOVER D8).
- **Confirms if it succeeds:** the stakeholder can pick t from evidence, and the review share is known in advance.

## Acceptance criteria
1. Defect damaged_case/005.png GOOD 0.82; good/000.png 0.91, good/001.png 0.85, good/017.png 0.88 → row 0.80: 1 false pass, 3/3 auto-pass. Row 0.86: 0 false passes, 2/3. Lowest safe threshold 0.83.
2. A defect judged GOOD with P(GOOD) 1.00 → "no threshold up to 0.99 reaches 0 false passes".
3. 4 labelled images → exactly 4 Jev calls.
4. The row for the configured t equals the headline numbers of slice 01.
5. **Production data:** the sweep from the real 253-image run is recorded below, and the threshold decision is written down (keep 0.86 or a new value, with the good auto-pass it costs).

## Dependencies
- Slice 01 (evaluation data per image).

## Dogfood moment
Same day: Jonas reads the sweep from the slice-01 run and sets `detector.pass-threshold` for the demo.

## Evidence / decision (fill after the run)
- Lowest safe threshold: _pending_ · Decision: _pending_
