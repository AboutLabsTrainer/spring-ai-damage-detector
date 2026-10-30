# Slice 01: Evaluate the pass rule on the labelled images

**Story:** US-01 · **Job:** J2 · **Persona:** P3 Jonas Berger (illustrative) · **Estimate:** ≤1 day (reference class: one new CLI mode that reuses the existing batch pipeline plus one new report writer)

## Goal
`--evaluate` scores the existing pipeline against the folder labels of `images/` and shows false passes, good auto-pass rate and per-class recall.

## IN
- `--evaluate --input=<labelled folder> --output=<folder>`. The label is the first sub-folder: good, bent_lead, cut_lead, damaged_case, misplaced.
- Console summary line, `evaluation.md`, `evaluation.json` (per image: path, label, verdict, Jev choice, P(GOOD)).
- False-pass list, per-class table (images, not passed = recall, suspected class correct, passed), good auto-pass x/213.
- Unlabelled images are skipped, counted and not sent to Jev. A missing labelled structure gives a clear error before any Jev call.
- Non-zero exit when false passes > 0 at the configured threshold (D18, PO-proposed).
- MVTec AD attribution line in `evaluation.md`.

## OUT
- Threshold sweep (slice 02); retry on Jev failure (slice 03); Docker (slice 04).
- Held-out split by the SPIKE seed, repeated runs, scrap/review image copies in evaluate mode.

## Learning hypothesis
- **Disproves "0.86 gives zero false passes reproducibly" if it fails:** any of the 40 defects gets choice GOOD with P(GOOD) ≥ 0.86 on this fresh run. The SPIKE margin was 0.01 (max defect 0.85).
- **Confirms if it succeeds:** 0 false passes on a second, independent Jev pass over all 40 defects, plus the good auto-pass rate on the 187 good images the SPIKE never used (the SPIKE had 14/26 = 54 %).

## Acceptance criteria
1. A labelled folder with good/000.png (P(GOOD) 0.91), good/001.png (0.85), bent_lead/000.png (CUT_LEAD) and misplaced/000.png (GOOD 0.45) at t = 0.86 → `0 false passes`, good auto-pass 1/2, recall 1/1 for bent_lead and misplaced, and the bent_lead suspected class is counted as not correct.
2. A defect judged GOOD with P(GOOD) 0.87 at t = 0.86 → listed as a false pass with 0.87, non-zero exit.
3. No Jev request contains a file name, folder name or label (C11).
4. A folder without the labelled sub-folders → the message names the 5 expected sub-folders, non-zero exit, 0 Jev calls.
5. **Production data:** the real run over all 253 images in `images/` completes. Paste its summary line and the per-class table here (Evidence section) together with the run date and the cost (expected ≈ $0.04, ≈ 5 min).

## Dependencies
- Walking skeleton `89c9f4d`; OpenRouter key with credit; local `images/` (gitignored).
- Risk: before slice 03, a single Jev failure aborts the 253-image run. In that case, rerun it (≈ $0.04).

## Dogfood moment
Same day: Jonas runs it on `images/` and decides whether the showcase may state "0 false passes on all 253 images".

## Pre-slice SPIKE
None needed. The request shape and cost are known from the SPIKE.

## Evidence (fill after the run)
- Run date / summary line / per-class table: _pending_
