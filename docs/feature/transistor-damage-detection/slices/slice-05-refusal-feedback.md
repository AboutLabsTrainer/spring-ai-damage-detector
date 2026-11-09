# Slice 05: A run that cannot work is refused at once

**Story:** US-05 · **Job:** J1 · **Persona:** P1 Petra Lindner (illustrative) · **Estimate:** 1 day (reference class: one central error-to-message mapping plus start-up checks in an existing CLI)

**Execution order:** 01 → 02 → 03 → **05** → 04. Slice numbers are stable, so this slice runs before slice 04.

## Goal
Every run that cannot work ends with one line that names the cause and the fix, exit 1, no stack trace and no Jev cost.

## IN
- The INV-5 rows owned by US-05:
  - missing `--input` or `--output`
  - unknown option
  - input folder does not exist
  - input folder has no images
  - output same as or inside input
  - output folder not writable
  - threshold out of range or not a number
  - Jev base URL cannot be used
  - no API key
  - any other unexpected exception
- Details of an unexpected exception go to `<output>/damage-detector.log` when that file exists.

## OUT
- INV-5 rows owned elsewhere: no labelled sub-folders and false pass (slice 01); no image judged and single Jev failure (slice 03).
- Container checks of the key and output rows (slice 04).
- Colour, localisation, a `--help` page, a dry-run mode.
- A paid or network check of Jev before the run. A 401 must still produce a report (D14).

## Learning hypothesis
- **Disproves "one central feedback point catches every failure path" if it fails:** a refusal case still shows a stack trace or exits silently, for example a binding error during start-up. That would show a failure path outside the planned catch points.
- **Confirms if it succeeds:** Petra can act on every failure from one line, and a typo can never start a paid run.

## Acceptance criteria
1. `--input=shift-2026-10-31` (missing folder) → one line `Input folder does not exist: …`, exit 1, 0 Jev calls.
2. `--evalute --input=images --output=out/eval` → the usage line, exit 1, 0 Jev calls.
3. `--input=images --output=images/out` → told to choose an output outside the input, exit 1, nothing written below `images`.
4. `--detector.pass-threshold=1.5`, `--detector.pass-threshold=abc` and an unusable `--detector.jev.base-url` → one line that mentions the setting, exit 1, 0 Jev calls.
5. No OPENROUTER_API_KEY → `No Jev API key: set the OPENROUTER_API_KEY environment variable`, exit 1.
6. An unexpected failure → `Inspection failed: <short cause>. See log for details.`, exit 1, no stack trace on the console.
7. **Production data:** by hand, on the real `images/` folder, run cases 1 to 3 and `--detector.pass-threshold=1.5`. Each gives one line and exit 1, and the OpenRouter activity page shows no new cost. Record the lines below.

## Dependencies
- Walking skeleton. Slice 01 already adds exit 2 and its own refusal, and slice 03 adds exit 3.
- Slice 04 depends on this slice, because the Docker failure modes rely on these refusals.

## Dogfood moment
Same day: Petra-style run of each mistake on the real `images/` folder.

## Pre-slice SPIKE
None needed. Refusal order and exit codes are fixed in DDD-9 and DDD-10.

## Evidence (fill after the run)
- Refusal lines and exit codes, 2026-11-09, real `images/` folder (253 images), real jar, key from `.env`:

  | Command | stderr line | Exit |
  |---|---|---|
  | `--input=images/nope-shift-2026-10-31 --output=out/hand-05` | `Input folder does not exist: images/nope-shift-2026-10-31` | 1 |
  | `--evalute --input=images --output=out/eval` | `Usage: java -jar damage-detector.jar [--evaluate] --input=<folder> --output=<folder> (unknown option --evalute)` | 1 |
  | `--input=images --output=images/out` | `Output folder images/out is the same as or inside input folder images: choose an output outside the input` | 1 |
  | `--input=images --output=out/hand-05b --detector.pass-threshold=1.5` | `Pass threshold must be a number from 0 to 1: set detector.pass-threshold (was 1.5)` | 1 |

- No new cost: key usage read from `https://openrouter.ai/api/v1/key` before the four runs 0.096630786, after 0.096630786 (equal).
- Nothing written below `images/`: 259 entries before and after. The first three runs wrote nothing; the threshold run left only `damage-detector.log` in its output folder (removed).
