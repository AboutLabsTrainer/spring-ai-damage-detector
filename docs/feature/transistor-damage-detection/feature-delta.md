# Feature Delta: transistor-damage-detection

Evidence keys E1-E8 are defined in `discover/wave-decisions.md` (Sources).
Discovery status: **showcase scope** (E6: the stakeholder confirmed this is a demo/showcase project, not a real production line). No customer interviews were run (0 of 5); for a showcase, customer problem validation is waived, not passed. The QS persona is illustrative. The technical findings (E2-E4) are real evidence. If the project is ever productized, Phase 1 must be reopened.

## Wave: DISCOVER / [REF] Persona ID

- **P1 - QS inspector, transistor production line** (illustrative showcase persona, UNVALIDATED; E6). Documents scrap: transistors that are misplaced, have a damaged case, a cut lead or a bent lead. The user states the images come from an automatic top-view camera (E2, single stakeholder statement). The dataset matches public MVTec AD (E3), so it is unverified whether a live line camera actually feeds QS.
- **P2 - Defect-record consumer** (illustrative). Uses the records for both quality reporting/audit and process feedback to the line (E6).
- Segmentation by job: "decide pass/scrap for each placed transistor and leave a record of why".

## Wave: DISCOVER / [REF] Opportunity statement

> Minimize the time QS spends, and the chance of a missed or wrongly categorized defect, when deciding whether a placed transistor passes and documenting each scrap case with its image and a textual reason.

Job map (seed, source E1/E2): Locate (camera image arrives) -> Confirm (pass vs defect class) -> Execute (record image + description) -> Conclude (record feeds reporting and line feedback, E6).
OST: the desired outcome and this opportunity are drafted, but **not scored**. There is no importance or satisfaction data, and I did not make up scores.

## Wave: DISCOVER / [REF] Validated assumptions

| ID | Assumption | Evidence | Confidence |
|----|-----------|----------|-----------|
| V1 | Labeled top-view images exist: consistent 1024x1024 PNG (verified from file headers and du). The user states they come from an automatic top-view camera (E2, single stakeholder statement). The dataset matches public MVTec AD (E3), so it is unverified whether a live line camera feeds QS | E2, E3 | Medium |
| V2 | The brief's 4 defect types map 1:1 to the labeled folders (damaged -> `damaged_case` is inferred) | E1, E2 | Medium |
| V3 | The defects can be seen in a top-view image (lead outside the hole, body shifted/rotated) | E2 (3 images inspected; some defects are subtle) | Medium |
| V4 | Jev offers a REST API with a typed `choice` over fixed options plus a calibrated confidence. A confidence threshold can route a case to human review | E4 (docs only, no live call yet) | Medium-High |

## Wave: DISCOVER / [REF] Invalidated assumptions

| ID | Assumption (from brief) | Evidence | Status |
|----|------------------------|----------|--------|
| X1 | "Jev / TypeSafe classifies the images" | E4: the docs describe only text or structured `state` input, with no image or multimodal input | REVERSED by E8a: Jev accepts images via OpenRouter (early feature, slower/less accurate). Tested in SPIKE |
| X3 | Spring can use a Jev SDK | E4: the docs list SDKs for Python and JS/TS only | Provisionally invalidated (no Java SDK documented as of 2026-10-29). Spring calls the REST API directly |

Unverified / conditional (not invalidated):

| ID | Assumption (from brief) | Evidence | Status |
|----|------------------------|----------|--------|
| X2 | The images in `images/` can be used freely for the product | E3, E7: the user confirmed the dataset is MVTec AD "transistor" (CC BY-NC-SA 4.0, non-commercial) | Resolved for the showcase: non-commercial use with MVTec AD attribution (E6, E7). Blocks any commercial product |

## Wave: DISCOVER / [REF] Dropped options

- *Reinstated:* **send the image straight to Jev** (OpenRouter supports it, E8a) — SPIKE variant A.
- **Use a Jev Java SDK.** Dropped pending vendor confirmation: no Java SDK is documented (X3).
- **Train a supervised per-class classifier as the main approach.** Dropped because there are only 10 samples per defect class (E3).
- *Deferred, not dropped:* anomaly detection trained on good images only (there are 213). This is the fallback if the vision-to-Jev probe fails.

## Wave: DISCOVER / [REF] Decision gate (G1-G4)

| Gate | Status | Reason |
|------|--------|--------|
| G1 Problem | **WAIVED (showcase)** | Showcase project, not a real line (E6). 0 of 5 interviews; no customer pain claimed. Reopen if productized |
| G2 Opportunity | **WAIVED (showcase)** | The OST is not scored; the showcase goal is to demonstrate the capability, not to win a market (E6) |
| G3 Solution | **NOT STARTED** | No prototype exists. The feasibility probe has not run, so task completion and accuracy are unmeasured |
| G4 Viability | **CONDITIONAL PASS (showcase-scoped)** | Licence confirmed and fits non-commercial use (E7). Cloud APIs OK, residency deferred (E6). False-pass target = zero (E7). Vision model = cheapest Claude, `claude-haiku-5-5` (E7). Jev's cost per call is still unknown; the SPIKE measures cost per image |

**Go/no-go:** *go* for DISCUSS with showcase scope, with the feasibility SPIKE (G3) as the first technical step. G1/G2 are waived for the showcase, not validated.
**Peer review:** approved with conditions (attempt 2, 2026-10-29); conditions N1-N3 applied.

## Wave: DISCOVER / [REF] Constraints established

- C1 Spring Boot, best practices. C2 Runs in Docker. C3 Extensible toward a DB and microservices. All three are stakeholder-stated constraints, not validated assumptions (E1).
- C4 Input is a top-view 1024x1024 PNG. The user states it comes from an automatic camera (E2, single stakeholder statement). A live line camera is unverified (E3).
- C5 Jev takes text or structured state over REST with a Bearer API key, and documents no Java SDK. The key must be kept outside the code (E4).
- C6 Good parts pass. Each defect must be recorded with its image and a textual description (E1).
- C7 The sample data is MVTec AD "transistor", CC BY-NC-SA 4.0 (confirmed, E7). Non-commercial showcase use only; attribute MVTec AD.
- C10 Only images with no defect may pass. The demo target is zero false passes; when in doubt, route to review or reject (E7).
- C11 The folder structure of `images/` is a property of the demo data, not an input. The classifier must never see the folder name or path; labels are used only as ground truth for evaluation (E7).
- C12 Batch mode: process all images under the input folder in one run (E7).
- C13 (SUPERSEDED by E8b: local Qwen `qwen/qwen3.6-35b-a3b` in LM Studio; Claude subscription not usable in apps) Vision model: the cheapest current Claude model, `claude-haiku-5-5` ($0.10 / $0.50 per 1M input/output tokens up to 100K-token prompts; skill model table cached 2026-10-06). Image input support to be confirmed via the Models API in the SPIKE. The Message Batches API (50% cost) fits batch mode (E7).
- C9 Showcase/demo scope: cloud APIs (vision LLM, Jev) are allowed for now; data residency is deferred to a later iteration (E6).
- C8 The evaluation set is small: 10 per defect class. 10/10 gives a two-sided 95% Clopper-Pearson CI lower limit of about 69% (one-sided 95% bound about 74%), so accuracy claims will be weak (E3).

## Wave: DISCOVER / [REF] Pre-requisites

**Answered (E6):** showcase project, not a real line; records serve both reporting and line feedback; cloud APIs are fine for the demo, data residency deferred.

**Answered (E7):** zero false passes (only good parts pass); batch mode; cheapest Claude model for vision (`claude-haiku-5-5`); dataset confirmed as MVTec AD.

**Open questions** (none block the SPIKE):
1. Jev pricing per call (not in the docs read so far).
2. If Haiku cannot reach zero false passes, may the SPIKE try a stronger model (e.g. `claude-sonnet-5-5`), or should the demo route more cases to review instead? (stakeholder decision)

**Recommended next evidence** (smallest first):
- **Mom Test (deferred until productization):** the three past-behavior questions for 1-3 real QS people stay in `discover/wave-decisions.md` history; not needed for the showcase.
- **Technical probe (SPIKE):** run all 40 defect images and 40 random good images through `claude-haiku-5-5` (vision) -> structured JSON (body position/rotation, case intact, per lead: present / full length / straight / in hole) -> Jev `choice` {GOOD, MISPLACED, DAMAGED_CASE, CUT_LEAD, BENT_LEAD}. Measure:
  - per-class recall
  - **false-pass rate**
  - the share routed to human review at the chosen confidence threshold
  - cost and latency per image

  Include a baseline where the vision LLM classifies directly, to show whether Jev adds value.
  Hypothesis decision rule (zero false passes set by the stakeholder, E7; the 80%/50% recall thresholds stay provisional). Also report the false-reject rate (good images not auto-passed); it does not decide the outcome but shows how much review work the demo creates. Input to the model is image bytes only, never the path (C11):
  - Definition: *recall* per defect class = (correct class + routed to human review) / class size. The same definition applies to every rule below. A *false pass* = a defect image auto-passed as GOOD without a review route.
  - TRUE if there are zero false passes **and** every defect class reaches at least 80% recall.
  - FALSE if there is any false pass, any class has recall below 50%, or Jev gives no gain over the baseline.
  - INCONCLUSIVE otherwise: extend the sample (real line images or more defect samples) and rerun before deciding.

<!-- markdownlint-disable MD024 -->

## Wave: DISCUSS / [REF] Persona ID

Date 2026-10-30, revised 2026-11-06 (final wave gate, cycle 1). Density: lean (Tier-1 only). Evidence: one stakeholder plus SPIKE measurements; personas are illustrative (E6), with no customer interviews.

- **P1 QS inspector**: Petra Lindner (illustrative), `docs/product/personas/qs-inspector.yaml`. She runs the batch, gets told at once when a run cannot start, and works through REVIEW/SCRAP. Job J1.
- **P3 Showcase presenter**: Jonas Berger (illustrative), `docs/product/personas/showcase-presenter.yaml`. He proves the pass rule is safe and runs the demo anywhere. Jobs J2 and J3.
- P2 (defect-record consumer, DISCOVER) is still in the SSOT. No story in this wave is written for P2.

## Wave: DISCUSS / [REF] JTBD one-liner

> When I run the detector on a folder of transistor images, I want proof that no defect passes, every non-pass documented with a reason even if Jev glitches, and a one-command start anywhere, so I can show and trust the result. (J1 + J2 + J3 in `docs/product/jobs.yaml`; not scored, stakeholder priority J2 > J1 > J3.)

## Wave: DISCUSS / [REF] Glossary

- **Jev:** the decision model `typesafe/jev-1.13` from TypeSafe, reached through OpenRouter. It looks at one transistor image and answers GOOD or a defect class, with probabilities. It is the only judge in this product.
- **Docker:** allowed in persona language in US-04 (Q9), because Jonas runs and shares the tool as a Docker image. Elsewhere, stories stay free of tool names.

## Wave: DISCUSS / [REF] Locked decisions

DISCOVER used D1–D11, so this wave continues the numbering. All decisions below are user-confirmed. D14 and D18 started as PO proposals and were confirmed on 2026-10-30.

- **D12 Qwen is removed from scope entirely.** The user said, 2026-10-30: "I want to get rid of Qwen." Descriptions stay Jev-based (`Suspected X (p=…); P(GOOD)=…`). There is no LM Studio dependency.
- **D13 Jev call failure.** The tool retries transient failures with increasing waits (default 3 attempts in total, configurable). If it gives up, the image becomes REVIEW, its description carries the error, and the batch continues. The report counts these errors (user decision 2026-10-30). An errored image can never be PASS (C10). Amended by UC-1 (2026-10-30): other HTTP 4xx errors are not retried. Amended by Q3 (2026-11-06): a 429 whose `Retry-After` is above the longest allowed wait gives up at once.
- **D14 All-failed guard (user-confirmed).** If no image in the run got a Jev judgement, the tool still writes the report. The console says `0 of N images judged by Jev` and points to the key and the network, and the exit code is 3 (UC-2).
- **D15 Evaluation is a user-facing flag on the same jar: `--evaluate`.** The label is the first sub-folder below `--input` (`good`, `bent_lead`, `cut_lead`, `damaged_case`, `misplaced`). Labels are used only for scoring and are never sent to Jev (C11). Images without a label are skipped and are not sent to Jev.
- **D16 Recall definition.** Recall per defect class = images of that class NOT passed (SCRAP or REVIEW) / class size. The "suspected class correct" share is reported separately. This follows the SPIKE table and clarifies the DISCOVER wording (see Changed Assumptions).
- **D17 Threshold sweep from one pass.** The sweep uses one Jev call per image and recomputes the verdicts for t = 0.50…0.99 in steps of 0.01. The tool never changes the threshold. The user sets `--detector.pass-threshold=<t>`.
- **D18 Evaluation exit code (user-confirmed).** The exit code is 2 (UC-2) when at least one false pass occurs at the configured threshold, so a script can stop a demo build. The run also prints one stderr line (Q7).
- **D19 Docker is a user-visible contract only.** It means `docker run` with mounted folders and the key from the environment at run time. The image contains no key and no `.env`. Dockerfile and image design are handed to DEVOPS.
- **D20 The pass rule is unchanged.** PASS iff Jev chooses GOOD and P(GOOD) ≥ t. SCRAP iff Jev chooses a defect. Otherwise REVIEW. t stays at 0.86 unless slice-02 evidence moves it. Slice-02 evidence (2026-11-09): user raised the default to 0.87.
- **D21 WS strategy B** (see below).
- **D22 Slice order** is evaluate, then sweep, then Jev resilience, then refusal feedback, then Docker: 01 → 02 → 03 → 05 → 04. Slice numbers stay stable, so slice 05 runs before slice 04. The order follows learning leverage (slice 01 can invalidate 0.86), and Docker comes last because its failure modes rely on the refusals of slice 05.
- **D23 Every error or exception gives the user feedback.** User decision 2026-10-30: "Please include feedback in case of errors or exceptions". This also confirms D14 and D18. Whenever a run cannot do what was asked, the user sees a short, actionable message on stderr that names the cause and the fix, and the exit code is non-zero. By default no Java stack trace is shown; the details go to the log. Per-image Jev failures stay visible as REVIEW plus an error count (D13). See INV-5.
- **D24 US-03 is split (Q2, 2026-11-06).** US-03 keeps Jev resilience: retry, REVIEW, error count, 0-judged guard. The new US-05 owns refusal feedback: the INV-5 rows, including the general rows. Each has its own slice.
- **D25 Interrupted runs (Q5, 2026-11-06).** Reports are written only at the end of a run, through a temp file and a rename. Exit 130, 143 or 137 means interrupted. The user reruns into a fresh output folder. See INV-6.
- **D26 Evaluation honesty (Q8, 2026-11-06).** `evaluation.md` states the run date, and states that the defect images were in the SPIKE sample, so the defect side shows reproducibility and is not held-out validation.

## Wave: DISCUSS / [REF] Scope Assessment

PASS, revised 2026-11-06. There are 5 stories in 5 slices and 1 external integration (Jev via OpenRouter), which already exists in the skeleton.

| Estimate | Days |
|---|---|
| Slice 01 evaluate | 1 |
| Slice 02 sweep | 0.5 |
| Slice 03 Jev resilience | 1 |
| Slice 05 refusal feedback | 1 |
| Slice 04 Docker | 1.5 |
| CI and PIT precursor (not a slice) | 0.5 |
| **Total** | **5.5** |

The Boot 4.1 upgrade (DDD-27, DELIVER step 1) comes on top and is not estimated here.

Two oversize signals now apply: more than 3 modules (DESIGN has `batch`, `evaluate`, `inspection`, `jev`, `cli`) and outcomes that could ship separately. The second one is already answered, because each outcome is its own slice. The total stays well under the 2-week signal, there are fewer than 10 stories, and the user confirmed the US-03 split (Q2). No further split is needed.

**Slice size exception:** slice 04 is 1.5 days, above the ≤1 day rule. Its brief ships in two ordered halves (image and demo commands, then `scripts/container-check.sh`), so each half is demoable.

**Precursor item (not a slice, no story):** `.github/workflows/ci.yml` with `verify` and `mutation`, plus PIT in `pom.xml` (DEVOPS Mutation testing strategy). Estimate 0.5 day. It lands as commits right after DELIVER step 1. It has no user-visible output, so it is not a slice.

## Wave: DISCUSS / [REF] Story map

Backbone (P1/P3 journey, `docs/product/journeys/qs-scrap-documentation.yaml` → `discuss_refinement`):

| Start a run | Judge each image | Record results | Check accuracy | Choose threshold |
|---|---|---|---|---|
| `java -jar` CLI *(WS, done)* | Jev on image bytes *(WS, done)* | report.md/json, scrap/, review/ *(WS, done)* | | |
| clear refusal before any cost *(US-05)* | retry, then REVIEW on error *(US-03)* | error count in report, no half-written report *(US-03)* | `--evaluate` metrics *(US-01)* | sweep + safe threshold *(US-02)* |
| `docker run` *(US-04)* | | | | |

- **Walking skeleton:** commit `89c9f4d`. It covers every activity except accuracy checking.
- **Slices** (briefs in `slices/`), in execution order: 01 evaluate (US-01, J2, K1/K5) → 02 threshold sweep (US-02, J2, K1/K2) → 03 Jev failure resilience (US-03, J1, K3) → 05 refusal feedback (US-05, J1, K6) → 04 Docker run (US-04, J3, K4). Slice numbers are stable; slice 05 runs before slice 04. All are ≤1 day except slice 04 (1.5 days, see Scope Assessment).
- **Priority rationale:** slice 01 tests the riskiest assumption first. The 0.86 threshold has a 0.01 margin and was fitted in-sample, and a rerun could show a false pass. Slice 02 turns that evidence into a decision. Slice 03 must land before long unattended runs; the 253-image evaluation (~5 min) is exposed to the abort bug until then. Slice 05 comes next because the Docker failure modes rely on its refusals. Slice 04 changes no behaviour, only packaging, so it carries the least learning risk.

## Wave: DISCUSS / [REF] User stories with elevator pitches

### US-01: See whether any defect passes on the labelled images

`job_id: J2` · slice: `slice-01-evaluate-labelled-set` · persona: P3 · estimate: 1 day

**Problem.** Jonas Berger presents the detector as safe: "no defect passes". The only evidence is the SPIKE run on 26 good and 40 defect images, and 0.86 was fitted on those same images with a 0.01 margin (highest defect P(GOOD) = 0.85). The probe script is deleted, so he has no way to re-check the claim or show it live.

#### Elevator Pitch
Before: Jonas cannot measure false passes or the good auto-pass rate with the product; the numbers exist only in the SPIKE notes.
After: run `java -jar target/damage-detector-0.1.0-SNAPSHOT.jar --evaluate --input=images --output=out/eval` → sees `Evaluated 253 labelled images at pass threshold 0.86: 0 false passes, good auto-pass 110/213 (51.6 %), defects not passed 40/40, Jev cost $0.03795 -> out/eval/evaluation.md` (counts illustrative), plus `evaluation.md` with a per-class table and a false-pass list.
Decision enabled: Jonas decides whether he can claim "zero false passes on all 253 MVTec AD images" in the showcase, or must move the threshold first (US-02).

**Domain examples**
1. *Happy path:* the full set `images/` (213 good, 10 each of bent_lead, cut_lead, damaged_case, misplaced). All 40 defects are SCRAP or REVIEW. `evaluation.md` shows `0 false passes`, recall 10/10 for each class, and the good auto-pass rate over all 213.
2. *Boundary:* on this run Jev gives `damaged_case/002.png` P(GOOD) = 0.87 with choice GOOD (hypothetical; in the SPIKE it was a false pass at t = 0.80). It appears under "False passes" as `damaged_case/002.png  P(GOOD)=0.87`, and the exit code is non-zero.
3. *Error:* Jonas points `--input` at `out/` by mistake. It has no `good/` or defect sub-folders, so he sees `No labelled images below out: expected sub-folders good, bent_lead, cut_lead, damaged_case, misplaced`. No Jev call is made and the exit code is non-zero.
4. *Edge:* `images/unsorted/017.png` (unknown folder) is listed as `skipped (no label): 1` and is not sent to Jev.

**UAT scenarios**
```gherkin
Scenario: Presenter sees zero false passes on the labelled set
  Given a labelled folder with good images good/000.png and good/001.png and defect images bent_lead/000.png and misplaced/000.png
  And Jev judges good/000.png GOOD with P(GOOD) 0.91, good/001.png GOOD with 0.85, bent_lead/000.png CUT_LEAD and misplaced/000.png GOOD with 0.45
  When Jonas runs the evaluation with pass threshold 0.86
  Then the evaluation shows 0 false passes, good auto-pass 1/2 and recall 1/1 for bent_lead and misplaced
  And the suspected class for bent_lead/000.png is counted as not correct

Scenario: A defect that passes is named as a false pass
  Given Jev judges damaged_case/002.png GOOD with P(GOOD) 0.87
  When Jonas runs the evaluation with pass threshold 0.86
  Then the evaluation lists damaged_case/002.png as a false pass with P(GOOD) 0.87
  And the run ends with a non-zero exit code

Scenario: Folder names are only used for scoring
  When Jonas runs the evaluation on the labelled folder
  Then no request to Jev contains an image's file name, its relative path, a folder label followed by a path separator such as "damaged_case/", or the input folder name
  And apart from the image data, every request is identical for every image

Scenario: A folder without labels is refused before any cost
  Given a folder with no good or defect sub-folders
  When Jonas runs the evaluation on it
  Then he is told which sub-folders are expected
  And no Jev call is made
```

**Acceptance criteria**
- [ ] `--evaluate --input --output` writes `evaluation.md` and `evaluation.json` and prints one summary line: labelled count, threshold, false passes, good auto-pass x/y (%), defects not passed x/y, Jev cost, report path.
- [ ] A false pass is a labelled defect image with verdict PASS. Each one is listed by relative path with its P(GOOD).
- [ ] The per-class table shows, for every defect class: images, not passed (recall, D16), suspected class correct, passed.
- [ ] The exit code is 2 if false passes > 0 at the configured threshold (D18, UC-2), and the run then also prints one stderr line (Q7). Otherwise the exit code is 0.
- [ ] No request to Jev contains an image's file name, its relative path, a folder label followed by a path separator (e.g. `damaged_case/`), or the input folder name. Apart from the image data, every request is identical for every image (C11, UC-3). The fixed class vocabulary is allowed. Unlabelled images are skipped, counted, and not sent.
- [ ] A missing or empty labelled structure gives a message naming the five expected sub-folders, exit 1, and 0 Jev calls.
- [ ] `evaluation.json` carries the run's `jevCost` and `elapsedSeconds`, and `evaluation.md` shows both (K5 evidence).
- [ ] `evaluation.md` states the run date and states that the defect images were in the SPIKE sample, so the defect side shows reproducibility and is not held-out validation (D26, Q8).
- [ ] Running the evaluation again into the same output folder replaces the earlier `evaluation.md` and `evaluation.json` as a whole. Other files in that folder stay.
- [ ] `evaluation.md` ends with the attribution `Sample data: MVTec AD "transistor", CC BY-NC-SA 4.0, non-commercial use` (C7).
- [ ] Production-data AC: a real run over all 253 images in `images/` completes, and its summary line and `evaluation.md` are kept as evidence in slice 01.

**Outcome KPIs:** K1 and K5. **Technical notes:** about $0.00015/image (~$0.04 per 253) and about 1.2 s/image (~5 min sequential). Jev results can differ from run to run, so the report carries the run date. All 40 defects were already in the SPIKE sample: the defect side checks reproducibility, it is not held-out validation. Only the 187 good images not used in the SPIKE are new evidence. The tool does not know which 26 good images the SPIKE used, so a held-out split is OUT. Until slice 03 lands, a Jev failure aborts the evaluation. **Dependencies:** skeleton 89c9f4d, an OpenRouter key with credit, and a local `images/` folder (gitignored).

### US-02: Pick a pass threshold that lets no defect through

`job_id: J2` · slice: `slice-02-threshold-sweep` · persona: P3 · estimate: 0.5 day

**Problem.** Jonas has to choose t. A lower t auto-passes more good parts but risks false passes; a higher t sends more good parts to REVIEW. In the SPIKE, t = 0.80 let 2 damaged cases pass and t = 0.90 passed only 4 of 26 good images. Re-running the whole evaluation for each candidate t costs money and time and adds Jev noise.

#### Elevator Pitch
Before: Jonas would need one paid evaluation run per candidate threshold and could not compare them on the same Jev answers.
After: run `java -jar target/damage-detector-0.1.0-SNAPSHOT.jar --evaluate --input=images --output=out/eval` → sees in `evaluation.md` a table `| Threshold | False passes | Good auto-pass |` from 0.80 to 0.95 and the line `Lowest threshold with 0 false passes: 0.86` (value illustrative).
Decision enabled: Jonas keeps 0.86, or runs batches with `--detector.pass-threshold=0.88` (for example), knowing the review share that choice costs.

**Domain examples**
1. *Happy path:* the highest defect P(GOOD) with choice GOOD is 0.85, so the line reads `Lowest threshold with 0 false passes: 0.86`, and the 0.86 row matches the US-01 headline numbers.
2. *Threshold moves (hypothetical):* `damaged_case/002.png` reaches 0.87 on this run. The line reads `0.88`, and the 0.86 row shows 1 false pass.
3. *No safe threshold (hypothetical):* a defect gets choice GOOD with P(GOOD) = 1.00. The line reads `No threshold up to 0.99 reaches 0 false passes; route every GOOD to REVIEW`.

**UAT scenarios**
```gherkin
Scenario: Presenter sees the trade-off between false passes and auto-passed good parts
  Given Jev judged defect damaged_case/005.png GOOD with P(GOOD) 0.82 and good images good/000.png 0.91, good/001.png 0.85, good/017.png 0.88
  When Jonas runs the evaluation
  Then the sweep row 0.80 shows 1 false pass and good auto-pass 3/3
  And the sweep row 0.86 shows 0 false passes and good auto-pass 2/3
  And the evaluation says the lowest threshold with 0 false passes is 0.83

Scenario: Presenter is told when no threshold is safe
  Given Jev judged misplaced/006.png GOOD with P(GOOD) 1.00
  When Jonas runs the evaluation
  Then the evaluation says no threshold up to 0.99 reaches 0 false passes

Scenario: The sweep costs no extra Jev calls
  Given a labelled folder with 4 images
  When Jonas runs the evaluation
  Then Jev is called exactly 4 times
```

**Acceptance criteria**
- [ ] `evaluation.json` has a sweep entry for every t from 0.50 to 0.99 in steps of 0.01. `evaluation.md` shows 0.80–0.95 plus the configured t.
- [ ] Each row shows false passes and good auto-pass x/y at that t. The row for the configured t equals the US-01 headline numbers.
- [ ] The report states the lowest swept t with 0 false passes, or states that none exists.
- [ ] Jev is called once per labelled image, whatever the sweep size (D17). The configured threshold is never changed by the tool.
- [ ] Production-data AC: the sweep from the real 253-image run is recorded, and the threshold decision (keep 0.86 or a new value) is written into the slice-02 brief.

**Outcome KPIs:** K1 and K2. **Technical notes:** a pure recomputation over stored P(GOOD) values. Images that errored (after slice 03) count as not passed at every t. **Dependencies:** US-01.

### US-03: The batch finishes even when Jev fails for some images

`job_id: J1` · slice: `slice-03-jev-failure-resilience` · persona: P1 · estimate: 1 day

Upstream wording: AC 1 is the UC-1 text (user-confirmed 2026-10-30). AC 2 amends UC-1 (user-confirmed 2026-11-06, Q3). Example 2 uses the UC-4 timeout (user-confirmed 2026-10-30). The refusal feedback that was part of this story moved to US-05 (D24).

**Problem.** Petra Lindner starts a batch over a shift's images and walks away. Today a single failed Jev call (timeout, HTTP 503, 429 rate limit) aborts the whole run: she gets no report.md, no scrap records, and has to start again and pay again.

#### Elevator Pitch
Before: one failed Jev call aborts the batch, and Petra gets no report at all.
After: run `java -jar target/damage-detector-0.1.0-SNAPSHOT.jar --input=shift-2026-10-30 --output=out/shift-2026-10-30` → sees `Inspected 40 images: 18 PASS, 9 SCRAP, 13 REVIEW (REVIEW share 32.5 %; 2 Jev errors in REVIEW), Jev cost $0.00570 -> out/shift-2026-10-30/report.md` (counts illustrative), and `review/good/042.png.txt` reads `REVIEW: Jev call failed after 3 attempts: HTTP 503 Service Unavailable`.
Decision enabled: Petra releases the PASS parts, documents the SCRAP parts, and looks at the 2 errored images herself (or reruns only those) instead of rerunning the whole batch.

**Domain examples**
1. *Transient blip:* Jev returns 503 for `bent_lead/003.png`, then answers on the 2nd attempt. The image gets its normal verdict (SCRAP) and is not counted as an error.
2. *Persistent failure:* `good/042.png` times out on all 3 attempts. It becomes REVIEW with `Jev call failed after 3 attempts: timeout after 30 s`. The batch goes on with `good/043.png`.
3. *Everything fails:* Petra's key was revoked (HTTP 401) and all 4 images fail. report.md lists 4 REVIEW with the error, the console says `0 of 4 images judged by Jev - check OPENROUTER_API_KEY and network`, and the exit code is 3.
4. *Rate limit with a long wait:* Jev answers `good/042.png` with 429 and `Retry-After: 120`, and the longest allowed wait is 10 s. The tool does not wait. The image becomes REVIEW with `Jev call failed after 1 attempt: HTTP 429 Too Many Requests (Retry-After 120 s exceeds max-backoff)`.
5. *Interrupted:* Petra presses Ctrl-C after 20 of 40 images. The exit code is 130 and no report.md exists in `out/shift-2026-10-30`. She reruns into `out/shift-2026-10-30-b`.

**UAT scenarios**
```gherkin
Scenario: Batch completes and counts a failed image
  Given an input folder with good/000.png, good/042.png and bent_lead/000.png
  And Jev answers good/000.png GOOD with P(GOOD) 0.91 and bent_lead/000.png BENT_LEAD
  And Jev fails with "503 Service Unavailable" on every attempt for good/042.png
  When Petra runs the batch inspection
  Then the report shows 1 PASS, 1 SCRAP, 1 REVIEW and 1 Jev error
  And good/042.png is copied to review with the description "Jev call failed after 3 attempts: HTTP 503 Service Unavailable"

Scenario: A brief Jev outage does not change the verdict
  Given Jev fails once with "503" and then judges bent_lead/003.png BENT_LEAD
  When Petra runs the batch inspection
  Then bent_lead/003.png is SCRAP and the report shows 0 Jev errors

Scenario: A failed image never passes
  Given Jev fails on every attempt for good/000.png
  When Petra runs the batch inspection
  Then good/000.png is not PASS

Scenario: Petra is warned when no image could be judged
  Given Jev rejects every call with "401 Unauthorized"
  When Petra runs the batch inspection on 4 images
  Then the report lists 4 REVIEW images with the error
  And she is told that 0 of 4 images were judged and to check OPENROUTER_API_KEY
  And the run ends with exit code 3

Scenario: A rate limit that asks for too long a wait does not hold up the batch
  Given Jev answers good/042.png with "429 Too Many Requests" and asks to wait 120 seconds
  And the longest allowed wait is 10 seconds
  When Petra runs the batch inspection
  Then good/042.png is REVIEW with the description "Jev call failed after 1 attempt: HTTP 429 Too Many Requests (Retry-After 120 s exceeds max-backoff)"
  And the remaining images are still inspected

Scenario: An interrupted run leaves no half-written report
  Given Petra's batch over 40 images is running
  When the run is interrupted before the last image
  Then the output folder holds no partial report.md or report.json
  And the run ends with exit code 130, 143 or 137
```

**Acceptance criteria**
- [ ] A failed Jev call is retried with increasing waits if it is transient (timeout, connection error, HTTP 408/429/5xx, unreadable answer), default 3 attempts in total, configurable. Other HTTP 4xx errors (e.g. 401, 403, 400) are not retried; the image becomes REVIEW with `Jev call failed after 1 attempt: HTTP <code> <reason>`.
- [ ] A 429 with `Retry-After` at or below the longest allowed wait waits that long. Above it, the tool gives up at once, and the image becomes REVIEW with `Jev call failed after N attempt(s): HTTP 429 Too Many Requests (Retry-After <n> s exceeds max-backoff)` (DDD-6, Q3).
- [ ] If the tool gives up, the image is REVIEW, its description is `Jev call failed after N attempts: <reason>`, and the remaining images are still processed.
- [ ] The console summary, report.md and report.json show the number of Jev errors. report.json marks each errored image.
- [ ] An errored image is never PASS. A success on retry gives the normal verdict and is not counted as an error.
- [ ] If 0 images were judged, the report is still written, the console names OPENROUTER_API_KEY and the network, and the exit code is 3 (D14, UC-2).
- [ ] No error text, log line or report contains the API key.
- [ ] An interrupted run ends with exit 130, 143 or 137 and leaves no report under its final name that is partial (INV-6, D25).
- [ ] Production-data AC: a real run on 10 images from `images/`, with the network switched off for about 10 s during the run, completes with a report and at least 1 Jev error.

**Outcome KPIs:** K3. **Technical notes:** retry keys are in DDD-7 (`detector.jev.retry.max-attempts`, `initial-backoff`, `max-backoff`). In evaluate mode, errored images count as not passed (US-01/US-02). The rule for a `Retry-After` that is not a whole number is pinned in DESIGN. **Dependencies:** skeleton; none on US-01/02.

### US-05: A run that cannot work is refused at once, with a clear reason

`job_id: J1` · slice: `slice-05-refusal-feedback` · persona: P1 · estimate: 1 day

**Problem.** Petra Lindner mistypes a folder, an option or a threshold, or her key is missing. The skeleton has no central error handling: a bad argument ends as an uncaught exception, not a message she can act on. A typo like `--evalute` can even start a paid batch run instead of the evaluation she meant. She cannot tell what went wrong, or whether anything was charged or written.

#### Elevator Pitch
Before: a wrong folder, option or setting ends in an uncaught Java exception, or in a run Petra did not intend.
After: run `java -jar target/damage-detector-0.1.0-SNAPSHOT.jar --input=shift-2026-10-31 --output=out/shift-2026-10-31` with the folder name mistyped → sees the single line `Input folder does not exist: shift-2026-10-31`, exit code 1, and no Jev cost.
Decision enabled: Petra fixes the named cause and starts again straight away, knowing nothing was charged or judged.

**Domain examples**
1. *Mistyped folder:* `--input=shift-2026-10-31` does not exist, so she sees the line above. There are 0 Jev calls.
2. *Typo in an option:* `--evalute --input=images --output=out/eval` is refused with the usage line. The 253 images are not sent to Jev.
3. *Output inside input:* `--input=images --output=images/out` is refused with `Output folder images/out is the same as or inside input folder images: choose an output outside the input`.
4. *Unusable threshold:* `--detector.pass-threshold=1.5` and `--detector.pass-threshold=abc` are refused with one line that mentions the threshold.
5. *Unexpected failure:* the disk fills up while the report is written. She sees `Inspection failed: <short cause>. See log for details.` and exit 1, with no stack trace.

**UAT scenarios**
```gherkin
Scenario: A mistyped input folder is refused before any cost
  Given the folder "shift-2026-10-31" does not exist
  When Petra starts the batch inspection on it
  Then she sees one line saying the input folder does not exist
  And the run ends with exit code 1 and no Jev call is made

Scenario: A mistyped option cannot start a paid run
  When Petra starts a run with the option "--evalute"
  Then she sees the usage line
  And the run ends with exit code 1 and no Jev call is made

Scenario: An output folder inside the input is refused
  When Petra starts a run with input "images" and output "images/out"
  Then she is told to choose an output outside the input
  And nothing is written below "images"

Scenario Outline: A setting that cannot work is refused before any image is judged
  When Petra starts a run with <setting>
  Then she sees one line that mentions <subject>
  And the run ends with exit code 1 and no Jev call is made

  Examples:
    | setting                          | subject       |
    | --detector.pass-threshold=1.5    | the threshold |
    | --detector.pass-threshold=abc    | the threshold |
    | --detector.jev.base-url=not a url | the Jev address |

Scenario: A missing API key stops the run before any work
  Given OPENROUTER_API_KEY is not set
  When Petra starts the batch inspection
  Then she is told to set OPENROUTER_API_KEY
  And the run ends with exit code 1

Scenario: An unexpected failure ends with one clear line
  Given the report cannot be finished because of an unexpected failure
  When Petra's run reaches that point
  Then she sees one line naming the short cause, without a stack trace
  And the details are in the log
  And the run ends with exit code 1
```

**Acceptance criteria**
- [ ] Every INV-5 row owned by US-05 prints exactly one stderr line that names the cause and the fix, gives exit 1, and shows no stack trace by default.
- [ ] A refused run makes 0 Jev calls and leaves the input folder unchanged.
- [ ] An unknown option is refused with the usage line, so a typo can never start a paid run (DDD-12).
- [ ] An output folder that is the same as or inside the input is refused before anything is written (DDD-22).
- [ ] A threshold outside 0 to 1 or not a number, and a Jev base URL that cannot be used, are refused before the first image is judged.
- [ ] An unexpected exception gives one line and exit 1. The details go to `<output>/damage-detector.log` when that file exists.
- [ ] Production-data AC: by hand, on the real `images/` folder, a mistyped folder, `--evalute`, `--output=images/out` and `--detector.pass-threshold=1.5` each give one line and exit 1, and OpenRouter shows no new cost. Record the lines in slice 05.

**Outcome KPIs:** K6. **Technical notes:** refusal order and exit codes are in DDD-9 and DDD-10. Exact texts for the threshold and base-URL rows are pinned in DESIGN. A refusal before the log file exists has no log, so its stderr line is the complete feedback (DDD-11). **Dependencies:** skeleton. It runs after slice 03, and before slice 04 because the Docker failure modes rely on these refusals.

### US-04: Run the inspection and the evaluation from Docker

`job_id: J3` · slice: `slice-04-docker-run` · persona: P3 · estimate: 1.5 days

**Problem.** Jonas wants to show the detector on a colleague's machine. Today that needs JDK 21, Maven, a built jar and a sourced `.env`. Copying a Docker image that contains his OpenRouter key would leak it.

#### Elevator Pitch
Before: the demo only runs where Java and Maven are installed and `.env` is sourced by hand.
After: run `mkdir -p out && docker run --rm --user "$(id -u):$(id -g)" --env-file .env -v "$PWD/images:/in:ro" -v "$PWD/out:/out" damage-detector:0.1.0 --input=/in --output=/out` → sees the same summary line as the jar (`Inspected 253 images: … -> /out/report.md`), and `./out` contains report.md, report.json, scrap/ and review/ that he can open and delete without sudo.
Decision enabled: Jonas decides he can demo on any Docker host and share the image without sharing his key.

**Domain examples**
1. *Happy path:* on a fresh Ubuntu laptop with Docker only, `mkdir -p out` and then the batch command above, on 5 images from `images/`, write `./out/report.md` with the same verdict counts as `java -jar` on the same stubbed Jev answers.
2. *Evaluate:* `mkdir -p out && docker run --rm --user "$(id -u):$(id -g)" --env-file .env -v "$PWD/images:/in:ro" -v "$PWD/out:/out" damage-detector:0.1.0 --evaluate --input=/in --output=/out/eval` writes `evaluation.md`.
3. *Error:* without `--env-file`, Jonas sees `No Jev API key: set the OPENROUTER_API_KEY environment variable`, the exit code is 1, and no Jev call is made.

**UAT scenarios**
```gherkin
Scenario: Presenter gets the usual report from a container
  Given the image folder holds good/000.png and cut_lead/000.png
  When Jonas runs the detector container with the folder mounted as input and ./out as output
  Then ./out contains report.md, report.json and the scrap copy of cut_lead/000.png with its description
  And the verdict counts equal those of the jar run on the same Jev answers

Scenario: The shared container image carries no API key
  When Jonas inspects the built detector image
  Then neither its configuration, its history nor its files contain an OpenRouter key or a .env file

Scenario: A missing key is reported before any work
  When Jonas runs the container without OPENROUTER_API_KEY
  Then he is told to set OPENROUTER_API_KEY and the run ends with exit code 1
```

**Acceptance criteria**
- [ ] The documented command (batch and `--evaluate`, each starting with `mkdir -p out`) accepts the same flags as the jar and writes the same set of files into the mounted output folder.
- [ ] Verdict counts and report structure equal the jar run for the same Jev answers.
- [ ] The key comes only from the run-time environment. The shared image holds no key and no `.env` in its configuration, its history or its files.
- [ ] Files written to the mounted output can be deleted by the host user without sudo when the documented command is used.
- [ ] A missing key gives the existing message and exit 1 before any image is processed.
- [ ] Production-data AC: the documented command runs on ≥5 real images from `images/` against real Jev and writes report.md.

**Outcome KPIs:** K4. **Technical notes:** the documented commands are in `## Wave: DEVOPS / [REF] Docker contract (slice 04)`, which also owns the Dockerfile, base image, user mapping and tag. `scripts/container-check.sh` is this story's scenario gate (Q4): it uses stubbed Jev for parity, and `docker image inspect`, `docker history` and `docker save` for the key scan. `images/` is 312 MB and gitignored, so it is mounted, never copied into the image. "Docker" is allowed persona language here (Q9). **Dependencies:** slice 05 first (refusals), slice 03 recommended first (long demo runs); Docker Engine on the demo host. The contract comes from DEVOPS (nw-platform-architect), and DELIVER builds it.

## Wave: DISCUSS / [REF] Acceptance criteria

The ACs are embedded per story above (US-01: 11, US-02: 5, US-03: 9, US-04: 6, US-05: 7). Cross-cutting invariants that hold for every story:
- **INV-1 (C10):** PASS only when Jev chose GOOD with P(GOOD) ≥ the configured threshold. An image whose Jev call failed is never PASS.
- **INV-2 (C11, UC-3):** no request to Jev contains an image's file name, its relative path, a folder label followed by a path separator (e.g. `damaged_case/`), or the input folder name. Apart from the image data, every request is identical for every image. The fixed class vocabulary is allowed, because it is the same for every image and carries no ground truth.
- **INV-3:** the API key never appears in the image, logs, console or reports.
- **INV-4:** PASS + SCRAP + REVIEW = total, and Jev errors ≤ REVIEW.
- **INV-5 (D23): errors and exceptions always give feedback.** Each case below gives the user one actionable message and shows no stack trace by default. Exit codes follow UC-2. Message texts are examples; DESIGN may reword them but must keep the cause and the fix.

  | Situation | User sees (example) | Exit | Owner |
  |---|---|---|---|
  | `--input` or `--output` missing | `Usage: java -jar damage-detector.jar [--evaluate] --input=<folder> --output=<folder> (missing --output)` | 1 | US-05 |
  | Unknown option (e.g. `--evalute`) | The usage line, naming the unknown option | 1 | US-05 |
  | Input folder does not exist | `Input folder does not exist: /home/jonas/imgs` | 1 | US-05 |
  | Input folder has no images | `No PNG/JPEG images below /home/jonas/empty` | 1 | US-05 |
  | Output same as or inside input | `Output folder <o> is the same as or inside input folder <i>: choose an output outside the input` | 1 | US-05 |
  | Output folder not writable | `Cannot write to output folder /out: permission denied` | 1 | US-05; US-04 in the container |
  | Threshold out of range or not a number | One line that mentions the threshold (text pinned in DESIGN) | 1 | US-05 |
  | Jev base URL cannot be used | One line that mentions the base URL (text pinned in DESIGN) | 1 | US-05 |
  | No API key | `No Jev API key: set the OPENROUTER_API_KEY environment variable` | 1 | US-05; US-04 in the container |
  | Any other unexpected exception | `Inspection failed: <short cause>. See log for details.` | 1 | US-05 |
  | Evaluate: no labelled sub-folders | `No labelled images below out: expected sub-folders good, bent_lead, cut_lead, damaged_case, misplaced` | 1 | US-01 |
  | Evaluate: false pass at the configured t (D18) | Summary line with `1 false passes`, list in evaluation.md, and one stderr line (Q7), e.g. `1 false passes at pass threshold 0.86: see out/eval/evaluation.md` | 2 | US-01 |
  | No image judged by Jev (D14) | `0 of 4 images judged by Jev - check OPENROUTER_API_KEY and network` (report still written) | 3 | US-03 |
  | Single image fails at Jev (D13) | No abort; REVIEW + `Jev call failed after 3 attempts: …` + error count | 0 | US-03 |

  Each story's acceptance tests cover the rows it owns.
- **INV-6 (D25): an interrupted run leaves no partial report.** Reports are written only at the end of a run, through a temp file and a rename. Exit 130, 143 or 137 means the run was interrupted, not that it has a result. The user reruns into a fresh output folder. Owner: US-03.

## Wave: DISCUSS / [REF] Outcome KPIs

Objective: the showcase claims only what the tool measures. That means zero false passes on all labelled images, an honest review share, and no lost batch.

| # | Who | Does what | Target | Baseline | Measured by |
|---|---|---|---|---|---|
| K1 (north star) | P3 presenter | sees false passes on the full labelled set (40 defects) at the chosen threshold | **0** | 0/40 at 0.86, in-sample (SPIKE) | `--evaluate` summary line / evaluation.json |
| K2 | P3 presenter | reports the good auto-pass rate on all 213 good images | reported in every evaluation; informative goal ≥ 50 %, not a gate | 14/26 = 54 % (SPIKE) | evaluation.md sweep row at the configured t |
| K3 | P1 inspector | gets a complete report from a batch with injected Jev failures | 100 % of runs complete; 0 errored images PASS | 0 % (one failure aborts) | acceptance test with 2 of 10 calls failing + slice-03 real run |
| K4 | P3 presenter | gets the jar-equivalent report from `docker run` | verdict counts identical in 100 % of compared runs; 0 key occurrences in the image | no container | stubbed-Jev parity test; `docker history`/`inspect` scan |
| K5 (guardrail) | P3 presenter | pays and waits for a full evaluation | ≤ $0.05 and ≤ 10 min per 253-image run | $0.00015/image, 1.2 s/image (SPIKE) | `jevCost` and `elapsedSeconds` in evaluation.json, shown in evaluation.md |
| K6 | P1 inspector | gets a one-line refusal for a run that cannot work | 100 % of the US-05 INV-5 rows: one line, exit 1, no stack trace, 0 Jev calls | not measured; the skeleton has no central error handling | acceptance tests per row + slice-05 hand run |
| G1 (guardrail) | n/a | Jev requests carrying a file name, path, folder label with separator or input folder name (UC-3) | 0 | 0 (skeleton test) | WireMock request assertion |

DEVOPS handoff: K4 and K5 need no runtime monitoring. They are measured per run from the report files.

## Wave: DISCUSS / [REF] Definition of Done

1. Every UAT scenario of the story is an executable acceptance test and is green: `./mvnw verify` for US-01, US-02, US-03 and US-05; `scripts/container-check.sh` for US-04, because its scenarios need Docker and `./mvnw verify` must stay Docker-free (Q4).
2. The production-data AC was run on real MVTec AD images, and its output is cited in the slice brief.
3. INV-1 to INV-6 are asserted by tests.
4. Jev stays stubbed in automated tests (no paid calls in CI). One real-Jev contract smoke per driven adapter is documented.
5. The console summary line, report.md and report.json stay consistent (same counts).
6. The README shows the exact command from the story's elevator pitch. For US-04 these are the DEVOPS demo commands, including the `outside` guard block, `mkdir -p out` and `--user "$(id -u):$(id -g)"`.
7. No secret is committed, logged or packaged.
8. The code is reviewed and refactored, with no new domain dependency on Spring (`domain/` stays framework-free).
9. The story can be demoed in one session with the elevator-pitch command.

## Wave: DISCUSS / [REF] Out-of-scope

- **Qwen / any local vision LLM / LM Studio.** Removed by D12; this is not an optional add-on.
- Database persistence of reports, a web UI or REST API, and splitting into microservices. C3 asks only for extensibility, and the hexagonal ports keep that open.
- Improving the class label (CUT vs BENT confusion, 20/40 exact). Descriptions stay "Suspected …".
- Good-only anomaly detection fallback, a stronger vision model, and two-signal pass rules.
- Automatic threshold tuning (the tool reports and the user decides), held-out split by the SPIKE seed, and repeated runs to measure Jev variance.
- Parallel or batched Jev calls, and per-image cost budgets or limits.
- Dockerfile, base image, registry, CI pipeline, image publishing (DEVOPS).
- Handing records over to P2 (reporting, line feedback). The product stops at report files plus scrap/ and review/ copies. Getting them to a consumer is a manual step outside this feature.
- Resuming an interrupted run. The user reruns into a fresh output folder (D25).
- Data residency and commercial use (blocked by CC BY-NC-SA, C7).

## Wave: DISCUSS / [REF] WS strategy

**B: real local adapters plus a fake for the costly external.** The skeleton already exists (`89c9f4d`). Acceptance tests start the real Spring Boot CLI on the real filesystem with Jev stubbed by WireMock, because Jev is a paid LLM API. Every new story extends this harness. Each slice's production-data AC uses real Jev on real images, run by hand and cited. US-04 adds a container-level run with the same stub, in `scripts/container-check.sh`. The first scenario that runs the real jar is enabled right after the Boot 4.1 upgrade (Q11). This is not D: there is no env-switching between real and fake in the automated suite.

## Wave: DISCUSS / [REF] Driving ports

| Port | Form | Story |
|---|---|---|
| Batch CLI | `java -jar target/damage-detector-0.1.0-SNAPSHOT.jar --input=<folder> --output=<folder>` | skeleton, US-03, US-05 |
| Evaluate CLI | `… --evaluate --input=<labelled folder> --output=<folder>` | US-01, US-02 |
| Threshold | `--detector.pass-threshold=<0..1>` (Spring property, default 0.87 since 2026-11-09, see D20) | US-02, US-05 |
| Retry settings | `detector.jev.retry.max-attempts` (default 3), `initial-backoff`, `max-backoff` (DDD-7) | US-03 |
| Container | `mkdir -p out && docker run --rm --user "$(id -u):$(id -g)" --env-file .env -v "$PWD/images:/in:ro" -v "$PWD/out:/out" damage-detector:0.1.0 [same flags]` | US-04 |
| Secret | env `OPENROUTER_API_KEY` | all |
| Outputs | stdout summary line, stderr refusal line, exit code (0, 1, 2, 3; 130/143/137 = interrupted), `report.md/json`, `scrap/`, `review/`, `evaluation.md/json` | all |

## Wave: DISCUSS / [REF] Pre-requisites

- The walking skeleton `89c9f4d` is green (`./mvnw verify`).
- An OpenRouter key with credit in a gitignored `.env`. A full evaluation costs about $0.04.
- A local `images/` folder (MVTec AD transistor, 253 PNG, gitignored, 312 MB).
- Docker Engine on the demo host (US-04). DEVOPS defines the contract, and DELIVER builds the image.
- The CI and PIT precursor (0.5 day, see Scope Assessment) lands right after DELIVER step 1.
- Jev image input via OpenRouter is an alpha/early feature (E8a), and its format may change. This is a risk to all slices.

## Wave: DISCUSS / [REF] DoR validation

| # | DoR item | Status | Evidence |
|---|---|---|---|
| 1 | Problem statement in domain language | PASS | Each story opens with a persona pain: the 0.01 margin, the threshold trade-off, a lost batch, a mistyped run, no Java on the demo host |
| 2 | Persona with specific characteristics | PASS | P1 Petra Lindner and P3 Jonas Berger (illustrative, YAML in `docs/product/personas/`) |
| 3 | ≥3 domain examples with real data | PASS | Real MVTec paths (`damaged_case/002.png`, `good/001.png` P(GOOD)=0.85), real error texts. Hypothetical values are marked |
| 4 | UAT Given/When/Then, 3–7 scenarios | PASS | US-01: 4, US-02: 3, US-03: 6, US-04: 3, US-05: 6 |
| 5 | AC derived from UAT | PASS | Every scenario maps to ≥1 AC. Each story adds a production-data AC |
| 6 | Right-sized | PASS | After the D24 split: US-01 1 day, US-02 0.5, US-03 1, US-05 1, US-04 1.5; 3–6 scenarios each. Slice 04 is a documented exception to ≤1 day per slice, in two ordered halves |
| 7 | Technical notes | PASS | Cost/latency, Jev variance, the in-sample defect set, DEVOPS contract, the abort risk before slice 03 |
| 8 | Dependencies tracked | PASS | US-02→US-01; US-04 after US-05, US-03 recommended; CI and PIT precursor; key, `images/`, Docker Engine |
| 9 | Outcome KPIs with targets | PASS | K1–K6 and G1 with numeric targets, baselines and measurement. K6 has no measured baseline |

JTBD traceability: US-01 and US-02 → J2, US-03 and US-05 → J1, US-04 → J3. No story is `@infrastructure`, and every slice has a user-visible story. **DoR status: PASSED by my own check after the cycle-1 revision.** The final wave gate needs to re-check it; its verdict before this revision was `needs_revision`.

## Wave: DISCUSS / [REF] Changed Assumptions

1. **Qwen removed (D12).**
   - DISCOVER `discover/wave-decisions.md` D11: "SUPERSEDED by E8b: vision step uses local Qwen (`qwen/qwen3.6-35b-a3b`, LM Studio), not `claude-haiku-5-5`."
   - DISCOVER C13 (wave-decisions): "Vision model: local Qwen `qwen/qwen3.6-35b-a3b` via LM Studio (OpenAI-compatible); Jev via OpenRouter Decisions API (superseded Haiku choice)".
   - SPIKE `spike/wave-decisions.md`: "Local Qwen (159 s/image) is too slow and too lenient as a gate; at most an optional, offline description writer for scrap records."
   - SPIKE `spike/findings.md` implication 5: "**Build the scrap record text from the vision JSON** (`summary` plus the per-lead notes). Treat CUT and BENT labels as unreliable for now."
   - **New assumption:** there is no Qwen in any role. The scrap description is Jev-based only (`Suspected X (p=…); P(GOOD)=…`). Rationale: the user decision of 2026-10-30, "I want to get rid of Qwen." Consequence: the textual description stays thin, and its class name is unreliable (20/40 exact). C6 "a textual description" is met minimally.
2. **Batch failure behaviour (D13).** SPIKE `spike/wave-decisions.md` known gaps: "one failed Jev call aborts the batch (no per-image retry/REVIEW fallback)". **New:** retry, then REVIEW with the error, continue, and count the errors (user decision 2026-10-30).
3. **Recall definition (D16).** DISCOVER feature-delta: "Definition: *recall* per defect class = (correct class + routed to human review) / class size." SPIKE findings column: "Recall (correct class or not passed)". **New:** not passed (SCRAP or REVIEW) / class size, with the correct-class share reported separately. Rationale: under D20, any non-GOOD choice is SCRAP, so pass/scrap safety does not depend on the class label.
4. **Threshold re-validation.** SPIKE `spike/wave-decisions.md`: "DESIGN/DISTILL must re-validate it on the remaining good images." **New:** this is a user-facing feature (`--evaluate`, US-01/02), not a one-off DESIGN/DISTILL task. All 40 defects are in-sample, so only the good side is held-out evidence.

## Wave: DISCUSS / [REF] Wave decisions summary

- **Decisions:** D12–D26 above. The user confirmed D14 and D18 on 2026-10-30 and extended them with D23/INV-5 (feedback on every error or exception). The `alternatives-considered` expansion was offered and not taken, so the output stays lean. The user approved going on to DESIGN.
- **Requirements:** 5 stories in 5 slices, execution order 01 → 02 → 03 → 05 → 04. Feature type: user-facing CLI (batch + evaluation) plus a container runtime. The walking skeleton exists.
- **Constraints carried forward:** C1, C2, C3, C7, C10, C11, C12; Jev via OpenRouter alpha; key from the environment only.
- **Upstream changes:** see Changed Assumptions (Qwen removed, batch failure behaviour, recall definition, threshold check as a feature).
- **Artifact location:** the legacy `discuss/` artifacts (user stories, story map, DoR validation, outcome KPIs, wave decisions) live in this file. There is no `discuss/` folder.
- **Handoff:** DESIGN (nw-solution-architect) gets all sections. DEVOPS (nw-platform-architect) gets US-04 and KPIs K4/K5.
- **Revision cycle 1 (final wave gate, 2026-11-06; user decisions Q2–Q5, Q7–Q9, Q11):**
  - US-03 split into US-03 and new US-05 with slice 05 (D24).
  - Retry-After give-up added to US-03 (Q3, amends UC-1).
  - UC-1, UC-2, UC-3 and UC-4 wording taken into the DISCUSS stories and invariants.
  - DoD 1 names `scripts/container-check.sh` for US-04 (Q4).
  - DEVOPS demo command used everywhere.
  - Interrupted runs added as INV-6 (Q5).
  - Exit 2 stderr line added (Q7).
  - Run date and SPIKE-sample statement added to `evaluation.md` (Q8).
  - INV-5 extended with four refusals, exit codes and owners.
  - K6 added. Slice 04 re-estimated to 1.5 days, and the CI and PIT precursor recorded at 0.5 day.
  - P2 hand-off marked out of scope.

## Wave: DESIGN / [REF] Options considered

Date 2026-10-30. Mode: PROPOSE. Density: lean. Scope: application. Paradigm: OOP (`CLAUDE.md`). Status: **Option A, confirmed by the user on 2026-10-30.**

- **A (recommended): extend the skeleton in place.** One jar is kept as a modular monolith with ports and adapters. `--evaluate` is a second use case that shares the per-image inspection path. Retry is a plain decorator on the `Jev` port, and a sealed `JevOutcome` makes "a failed image passes" impossible to represent. One central CLI error mapper handles all errors, and startup probes run before any paid call. No new runtime dependency (ArchUnit is test-only).
- **B: framework-driven variant of A.** Spring Retry with `@Retryable`/`@Recover` (+ spring-aspects/AOP) handles retry. Spring Boot `FailureAnalyzer` and `ExitCodeExceptionMapper` handle errors. Trade-off: less hand-written code, but there are 2 new dependencies, and Spring Retry is in maintenance mode. FailureAnalyzer prints multi-line blocks (which breaks INV-5's one-line rule), and an unreadable answer must still be mapped by hand.
- **C: separate evaluation tool plus Resilience4j.** A second module or main class joins `report.json` with folder labels, and Resilience4j handles retry. Trade-off: the production jar has no evaluation code. But it contradicts D15 (same jar, one command), needs a two-step workflow, and adds a library whose circuit-breaker value is out of scope.

Recommendation is **A**. It fits D15, adds the fewest dependencies, puts INV-1 in the type system, and keeps INV-5 at one test point.

## Wave: DESIGN / [REF] DDD list

Decision numbers use DDD-n to avoid clashing with D1–D23.

- **DDD-1:** A single Spring Boot non-web jar, structured as a modular monolith with ports and adapters: `domain/` (pure), `inspection/`, `probe/`, `batch/`, `evaluate/`, `jev/`, `cli/`, plus the root composition package. The evaluate mode is in the same jar (ADR-001).
- **DDD-2:** The sealed domain type `JevOutcome` is either `Judged(JevJudgement, attempts)` or `Failed(attempts, reason)`. A one-argument `Judged(judgement)` means 1 attempt. The attempt count only feeds the cost note (DDD-15). The `Jev` port returns an outcome and never throws for call failures. `PassPolicy` maps `Failed` to REVIEW with an exhaustive switch. `Inspection` stores the outcome. INV-1 and INV-4 therefore hold structurally (ADR-003).
- **DDD-3:** Batch and evaluate share one per-image inspection path: `inspection/Inspector` reads bytes via `ImageFolder`, asks `Jev`, then applies `PassPolicy`.
  - `ImageFolder` returns **relative path strings only** and knows nothing about labels.
  - A `Label` is resolved **only in `evaluate/LabelledEvaluation`**, as a pure function from relative path to optional label. It never reaches `inspection/` or `jev/` (C11).
  - ArchUnit rule 2 enforces that `inspection..` and `jev..` do not depend on `evaluate..`, `batch..`, `cli..` or `domain.Label`.
- **DDD-4:** Evaluation metrics and the threshold sweep are pure domain functions over the recorded outcome per image. They re-apply `PassPolicy(t)`:
  - Thresholds are handled in hundredths (0.50…0.99).
  - The configured t is always included in the sweep, even if it is off-grid.
  - The configured-t row equals the headline by construction (D17, US-02 AC).
- **DDD-5:** Retry is the `RetryingJev` decorator, which implements `Jev` and takes the package-private `jev/AttemptSource`. `OpenRouterJev` implements `AttemptSource` with a single attempt. Its constructor takes a `RestClient.Builder` and plain values (base URL, model, API key, connect timeout, read timeout), and `DetectorConfiguration` maps `DetectorProperties` to them. It adds no new dependency, and its `Sleeper` is injected (ADR-004).
- **DDD-6:**
  - Transient failures are retried: connection errors, timeouts, HTTP 408, 429 and 5xx, an unreadable or empty answer.
  - Other 4xx errors are not retried; the description then reads `Jev call failed after 1 attempt: HTTP 401 Unauthorized`.
  - A 429 with `Retry-After` at or below `max-backoff` waits that long. **Above the cap there is no early retry.** The attempt counts as transient, but the decorator gives up at once and the image goes to REVIEW: `Failed(attempts, "HTTP 429 Too Many Requests (Retry-After <n> s exceeds max-backoff)")` (Q3).
  - A `Retry-After` that is not a whole number of seconds (HTTP date, negative, malformed) is ignored, and the normal backoff applies.
  - Reason texts never contain request data or the key (INV-3).
  - See UC-1 and its amendment.
- **DDD-7:** Retry configuration keys: `detector.jev.retry.max-attempts` (default 3, ≥ 1), `detector.jev.retry.initial-backoff` (default 1s, doubling), `detector.jev.retry.max-backoff` (default 10s).
- **DDD-8:** One central, **pure** `cli/ErrorFeedback` mapping turns a `ProbeResult.Refused` or an unexpected exception into `Feedback(line, exitCode)`. It prints nothing. The two printers are `DetectorLauncher` (bootstrap and start-up failures) and `cli.DetectorCommand` (everything inside a run). The texts are pinned in DDD-33 (ADR-005).
- **DDD-9:** Exit codes are 0 OK, 1 refused/unexpected, 2 false pass at the configured t (D18), 3 zero images judged (D14), and 130/143/137 interrupted (D25). Precedence among 1, 2 and 3 is 1 > 3 > 2. Reports are written for codes 2 and 3, and each prints one stderr line (DDD-33). See UC-2.
- **DDD-10:** Wire, then probe, then use. Probes run in the bootstrap order defined in brief.md and ADR-005:
  - **Before Spring, in `DetectorLauncher.launch`:**
    1. arguments, including unknown options (`CliArguments.check`)
    2. input exists, and the output is not the same as or inside the input (`ImageFolder.checkRun`; real paths, no write)
    3. create `--output` (`OutputFolder.probe`)
    4. write probe (`OutputFolder.probe`)
    5. set the log file (`LogSink` property map)
  - **After Spring starts, in `DetectorCommand`:**
    6. the pass threshold is usable (`PassThreshold.probe`)
    7. the input has images, or in evaluate mode labelled images
    8. the API key is present, then the base URL is usable (the Jev-side check, injected by the root composition as a plain `probe.Probe`, so `cli` never depends on `jev`)

  Bean construction no longer validates anything. There is no network or paid Jev probe, because D14/US-03 require a written report on a 401.
- **DDD-11:** Logging:
  - The console log is off by default; `--debug` restores it.
  - Details and stack traces go **only** to `<output>/damage-detector.log` (user decision 2026-10-30). There is **no temp-dir fallback**.
  - The path is injected by the composition root: `LogSink` is a pure function returning the property map, and `DetectorLauncher` sets that map as SpringApplication default properties in bootstrap step 5, after the output folder exists and has passed its probe. It is not set in ambient `application.yml`. The logging system performs the only write.
  - A refusal in bootstrap steps 1–4 has no log file; its stderr line is the complete feedback.
- **DDD-12:** Unknown CLI options are refused with the usage line. Dotted properties and the Spring flags `debug` and `trace` are allowed. This guards against a typo turning `--evaluate` into a paid batch run.
- **DDD-13:** Labels:
  - The label is the first path segment below `--input`, matched exactly against `good`, `bent_lead`, `cut_lead`, `damaged_case`, `misplaced`. Deeper nesting is allowed.
  - Other images are skipped, counted and never sent.
  - At least 1 labelled image is required; otherwise the run is refused with exit 1 and 0 calls.
  - A defect class with 0 images keeps its table row, with all numbers 0.
- **DDD-14 (evaluation contract, ratified 2026-11-06, Q12):** Evaluate writes `evaluation.md`, `evaluation.json` and `damage-detector.log`, with no scrap/review copies (slice-01 OUT). It replaces earlier `evaluation.*` files as a whole. Field names follow the `EvaluationRecord` test oracle. They are **user-ratified (Q12), not independently designed**.
  - **Console summary line (stdout):** `Evaluated <n> labelled images at pass threshold <t>: <f> false passes, good auto-pass <g>/<G> (<p> %), defects not passed <d>/<D>, Jev cost $<c> -> <output>/evaluation.md`. After failed attempts: `Jev cost $<c> (lower bound) -> …`.
  - **`evaluation.json`:**

    | Field | Content |
    |---|---|
    | `runDate` | ISO date from the injected `Clock`, e.g. `2026-11-06` |
    | `passThreshold` | The configured t |
    | `jevCost` | Sum of `usage.cost` of judged attempts |
    | `costIsLowerBound` | `true` after any failed attempt |
    | `elapsedSeconds` | Wall time of the run from the injected `Clock`, one decimal |
    | `summary.{labelled, skipped, judged, jevErrors, falsePasses, goodPassed, goodTotal, defectsNotPassed, defectTotal}` | Whole numbers |
    | `classes[].{label, images, notPassed, suspectedCorrect, passed}` | One entry per defect class, in the order `bent_lead`, `cut_lead`, `damaged_case`, `misplaced` |
    | `falsePassImages[].{path, pGood}` | Each false pass at the configured t |
    | `lowestSafeThreshold` | Number, or `null` if no t up to 0.99 has 0 false passes |
    | `sweep[].{threshold, falsePasses, goodPassed, goodTotal}` | Every t from 0.50 to 0.99 in steps of 0.01, plus the configured t if it is off-grid. Ascending |
    | `images[].{path, label, verdict, description, jevError, jevChoice, probabilities}` | Labelled images only. For a failed image `jevError` is `true`, and `jevChoice` and `probabilities` are `null` |

  - **`evaluation.md`,** in this order:
    1. `# Transistor pass-rule evaluation`
    2. `Run date: <runDate>.`
    3. The headline: `Evaluated <n> labelled images at pass threshold <t>: <f> false passes, good auto-pass <g>/<G> (<p> %), defects not passed <d>/<D>.`
    4. `skipped (no label): <s>` (always shown, also when 0)
    5. `Jev errors: <e>; images judged: <j>.`
    6. `Jev cost: $<c>. Elapsed: <x> s.` After failed attempts the first sentence reads `Jev cost: $<c> (lower bound: failed attempts are not counted).`
    7. `## False passes`, then one line per false pass, `- <path>  P(GOOD)=<p>` with two decimals, or `None.`
    8. `## Per class`, with the table `| Class | Images | Not passed | Suspected class correct | Passed |` and rows such as `| bent_lead | 10 | 10 | 8 | 0 |`. "Not passed" is recall per D16, in plain numbers.
    9. `## Threshold sweep`, with the table `| Threshold | False passes | Good auto-pass |`, rows such as `| 0.86 | 0 | 110/213 |`, for 0.80 to 0.95 plus the configured t.
    10. `Lowest threshold with 0 false passes: <t>`, or `No threshold up to 0.99 reaches 0 false passes; route every GOOD to REVIEW`
    11. `## Evidence notes`, with the **Q8 disclosure (D26)**: `The defect images were in the SPIKE sample, so the defect side shows reproducibility and is not held-out validation. Jev results can differ from run to run.`
    12. The last line: `Sample data: MVTec AD "transistor", CC BY-NC-SA 4.0, non-commercial use`

    Numbers: costs `%.5f`, percentages one decimal, thresholds two decimals (more only for an off-grid configured t), P(GOOD) two decimals.
- **DDD-15 (batch contract, ratified 2026-11-06, Q12):** Field names follow the `BatchRecord` test oracle. They are **user-ratified (Q12), not independently designed**.
  - **Console summary line (stdout):** `Inspected <n> images: <a> PASS, <s> SCRAP, <r> REVIEW (REVIEW share <p> %; <e> Jev errors in REVIEW), Jev cost $<c> -> <output>/report.md`. The Jev error count is always shown, also when 0. After failed attempts: `Jev cost $<c> (lower bound) -> …`.
  - **`report.json`:**

    | Field | Content |
    |---|---|
    | `summary.{total, pass, scrap, review, judged, jevErrors}` | Whole numbers |
    | `summary.{reviewShare, passThreshold}` | As in the skeleton |
    | `summary.jevCost` | Sum of `usage.cost` of judged attempts. **Renamed from the skeleton's `totalCost`**, so that batch and evaluation use one word |
    | `summary.costIsLowerBound` | `true` after any failed attempt |
    | `images[].{path, verdict, description, jevError, jevChoice, probabilities, confidence, cost}` | All images. For a failed image `jevError` is `true`, and `jevChoice`, `probabilities`, `confidence` and `cost` are `null` |

  - **`report.md`:** the skeleton's layout, with these additions:
    - After the verdict table (`| PASS | n |`, `| SCRAP | n |`, `| REVIEW | n |`, `| Total | n |`) comes the line `Jev errors: <e>; images judged: <j>.`
    - The cost sentence is `REVIEW share: <p> %. Total Jev cost: $<c>.` After failed attempts it reads `Total Jev cost: $<c> (lower bound: failed attempts are not counted).` The words `lower bound` appear only then.
    - `## Not passed` keeps its table `| Image | Verdict | Description | Copy |`.
  - **`.txt` description** (`scrap/<path>.txt`, `review/<path>.txt`): one line, `<VERDICT>: <description>`.
  - **Description texts:**

    | Case | Text |
    |---|---|
    | PASS | `GOOD; P(GOOD)=<p>` |
    | SCRAP | `Suspected <class> (p=<p>); P(GOOD)=<p>` |
    | REVIEW, below t | `Leans GOOD but below the pass threshold <t>; P(GOOD)=<p>` |
    | REVIEW, failed | `Jev call failed after <n> attempts: <reason>`, with `1 attempt` in the singular |

  - **Cost rule:** the cost of failed attempts is **unknown and not counted**. After any failed attempt, including one followed by a success, the cost is marked as a lower bound (ADR-004).
- **DDD-16:** D14 also applies in evaluate mode: exit 3 if 0 images were judged.
- **DDD-17:** ArchUnit 1.4.x (test scope; exact patch verified in DELIVER step 1) enforces **8 rules**. All of them block the build, and the same 8 are listed in brief.md under "Enforcement":
  1. `domain..` depends only on `domain..`, `java.lang..`, `java.util..` and `java.time..` (allow-list).
  2. `inspection..` and `jev..` do not depend on `evaluate..`, `batch..`, `cli..` or `domain.Label`.
  3. Only `DetectorLauncher` and `cli.DetectorCommand` access `System.out` or `System.err`. `ErrorFeedback` stays pure.
  4. `ImageFolder`, `OutputFolder`, `RetryingJev` and `cli.PassThreshold` implement `probe.Probe`.
  5. Only `DetectorConfiguration` constructs `OpenRouterJev`. `OpenRouterJev` does not implement `Jev`, and `RetryingJev` is the only `Jev` implementation.
  6. **Writes happen only in `inspection.OutputFolder`.** This covers:
     - `Files.write`, `writeString`, `copy`, `move`, `createDirectory`, `createDirectories`, `createFile`, `createTempFile`, `delete`, `deleteIfExists`, `newOutputStream`, `newBufferedWriter`, `newByteChannel`;
     - constructing `FileOutputStream`, `FileWriter`, `RandomAccessFile` or `PrintWriter(File/String)`;
     - the `java.io.File` mutators `delete`, `mkdir`, `mkdirs`, `renameTo`, `createNewFile`, `setWritable`;
     - Jackson `writeValue(File|Path|OutputStream)`.
  7. **Reads happen only in `inspection.ImageFolder`.** This covers:
     - `Files.walk`, `list`, `find`, `lines`, `readAllBytes`, `readString`, `readAllLines`, `newInputStream`, `newBufferedReader`;
     - constructing `FileInputStream` or `FileReader`.

     **Path inspection** (`Path.toRealPath`, `Files.exists`, `notExists`, `isDirectory`, `isRegularFile`, `isSameFile`) happens only in `ImageFolder` and `OutputFolder`.
  8. Layered dependencies follow the allowed-edges table in brief.md. This includes "`batch` and `evaluate` never depend on each other".
- **DDD-18:** The Docker contract adds no code beyond DDD-10. It covers `OPENROUTER_API_KEY` from the run-time env, input mounted read-only, output mounted writable by the host user, and the same flags. The output probe turns a bad mount into `Cannot write to output folder /out: permission denied`.

Added after peer review iteration 1:

- **DDD-19:** `OpenRouterJev` returns the package-private sealed `jev/AttemptResult` (answered, transient, permanent) and does not implement `Jev`. `RetryingJev` is the only `Jev` implementation and maps an attempt result to a `JevOutcome` (ADR-004).
- **DDD-20:** The shared per-image path and all file access live in a new `inspection/` package:
  - `Inspector`
  - `ImageFolder`, the **single read port**: root = `--input`, read-only. It returns relative path strings and the bytes for one relative path.
  - `OutputFolder`, the **single write port**: root = `--output`. It offers `writeString` and `writeBytes` (the byte write serves image copies). Both are atomic, both create missing parent folders, and both reject paths that escape the root.

  There is no separate `InputImages` type (merged into `ImageFolder`). `ReportWriter` reads the source images for its copies through `ImageFolder`, never through a raw `Path`. `batch/` and `evaluate/` depend on `inspection/` and never on each other (ArchUnit rule 8).
- **DDD-21:** `probe/Probe` is the probe contract, and `probe/ProbeResult` its pure result (ok or refused). It is implemented by `ImageFolder`, `OutputFolder`, `RetryingJev` and `cli/PassThreshold`. Allowed edges into `probe`: `cli`, `inspection`, `jev` and root (brief.md allowed-edges table). The declared `--output` write universe per run type is in brief.md.
- **DDD-22:** The bootstrap refuses when `--output` is **the same as or inside** `--input` (resolves OQ-4).
  - The comparison uses real paths: the input via `toRealPath`; an output that does not exist yet via the `toRealPath` of its nearest existing ancestor plus the remaining segments, **normalized before the comparison**, so `..` in the missing part cannot hide the containment. It uses `isSameFile` for equality and component-wise `startsWith` for containment. This also covers symlinks and `..`.
  - The check writes nothing and runs before the output folder is created.
  - Message: `Output folder <o> is the same as or inside input folder <i>: choose an output outside the input`, with `<o>` and `<i>` as typed (DDD-33). Exit 1, 0 Jev calls.
- **DDD-23:** The Jev read timeout is 30 s, down from 120 s (resolves OQ-1). It is set as `detector.jev.read-timeout`, with `detector.jev.connect-timeout` at 10 s, and both reach `OpenRouterJev` as plain constructor values.
  - K5 holds for runs with **up to 3 hanging images**: about 579 s. 4 hanging images take about 671 s and break it.
  - K5 excludes `Retry-After` waits, which the provider sets (ADR-004).
  - UC-4 confirmed 2026-10-30.
- **DDD-24:** The C11 assertion is made precise (UC-3). The fixed class vocabulary in `DefectQuestion` is allowed. Per-image paths, file names, folder labels with a path separator, and the input folder name are not. Every request body is identical except the data URL.
- **DDD-25:** `--evaluate` takes no value. The US-01/US-02 AC texts are normative for the evaluation output formats.

Added after peer review iteration 2:

- **DDD-26 (valid Jev answer):** `jev/` checks every attempt before PASS can be considered.
  - `choice` must be one of {`GOOD`, `MISPLACED`, `DAMAGED_CASE`, `CUT_LEAD`, `BENT_LEAD`}.
  - `probabilities.GOOD` must be non-null, finite and within [0, 1].
  - Anything else becomes `Transient("unreadable answer")`, is retried, and ends in REVIEW if it persists. So does an empty or unparsable body or a missing `answers.defect`.
  - Required WireMock cases: null probabilities, P(GOOD) out of range (1.7), NaN or missing P(GOOD), unknown class (`SCRATCHED`), missing `answers.defect`, a non-JSON body (ADR-004).
- **DDD-27 (Spring Boot 4.1):** upgrade from 3.5.16 to Spring Boot 4.1.x as **the first DELIVER step** (ADR-006, user decision 2026-10-30).
  - DELIVER step 1 verifies and pins on Maven Central: the exact 4.1 patch, a compatible Cucumber JUnit Platform engine, WireMock standalone, and the ArchUnit 1.4.x patch.
  - Changes that matter here:
    - Spring Framework 7
    - Jackson 3 (`tools.jackson`; `ReportWriter` and `WalkingSkeletonSteps` migrate, see DDD-37)
    - the RestClient starter (`spring-boot-starter-restclient`) and per-module test starters
    - renamed HTTP client properties (the Jev timeouts move to `detector.jev.*`, and the `spring.http.client.*` keys are removed from `application.yml`)
    - JUnit 6 / Platform
- **DDD-28 (logging sink and bootstrap):** a new `LogSink` component in the root/composition package.
  - **Pure function:** `defaultPropertiesFor(realOutput)` returns the property map naming `<output>/damage-detector.log`. The logging system performs the only write, and that file is its whole universe.
  - `DetectorLauncher.launch` pre-parses the arguments with `cli/CliArguments` and runs bootstrap steps 1–4 (args, real-path check, create the output via `OutputFolder`, write probe). Then it sets `LogSink`'s map as SpringApplication default properties, before `SpringApplication.run`.
  - When the output does not exist yet, it is **created first** via `OutputFolder` in step 3; it is never buffered.
  - `application.yml` carries no log path.
- **DDD-29 (output universe and DISTILL assertions):**
  - `--output` creation is a declared delta: only the missing directories from the nearest existing ancestor down.
  - On-write directory creation is in the universe: `OutputFolder` creates missing parents on every write. On the skeleton path, which skips the bootstrap, this also creates the root (brief.md).
  - The write probe's files are removed at bootstrap step 4 on every normal path. A JVM kill in between may leave an empty probe file; that leftover is documented, harmless (it is not an image) and overwritten by the next run.
  - DISTILL asserts after **a full run and after each refusal run** that the **input tree hash is unchanged** and that the **output tree equals the declared universe** for that run type (brief.md table).
  - `Inspector` is classified as **pure read through the input port, with external effect = Jev calls** and zero filesystem delta.

Added in final wave gate revision cycle 1 (2026-11-06):

- **DDD-30 (`DetectorLauncher`):** `launch(args, capabilities...)` in the root package owns bootstrap steps 1–5, starts Spring, and returns the exit code without ending the JVM. `DamageDetectorApplication.main` is only `System.exit(DetectorLauncher.launch(args))`.
  - Bounded change; universe = the created output directories, the probe file, the log path, stderr, the exit code.
  - **The `ApplicationContextInitializer` varargs stay public, as a test seam.** The in-process driver is in another package; the varargs replace only `Sleeper` and `Clock` while steps 1–5 stay under test; `main` passes none; nothing on the command line reaches them. A test-only property or a second entry point would be worse (ADR-005).
- **DDD-31 (plan-returning refusal path):** every check that can refuse before a write returns a `ProbeResult` and has no effect: `CliArguments.check(args)` and `ImageFolder.checkRun(output)`. Both are **unbounded preservation**: they mutate nothing, and only the caller acts on the result.
- **DDD-32 (`PassThreshold`):** `detector.pass-threshold` is bound as raw text, so a value like `abc` cannot fail in Spring's binder. `cli/PassThreshold` is a pure value: its `probe()` refuses with `THRESHOLD_INVALID`, and `policy()` returns the `PassPolicy`. `DetectorCommand` hands that policy to the use case after the probes. The use cases take their `ImageFolder`, `OutputFolder` and `PassPolicy` per run. The default stays 0.86.
- **DDD-33 (closed refusal set and pinned texts):** the refusal set equals `ProbeResult.Reason` plus the new `THRESHOLD_INVALID`. This pins the INV-5 rows Luna left open.
  - The full table is in ADR-005 decision 1. New texts:
    - `THRESHOLD_INVALID`: `Pass threshold must be a number from 0 to 1: set detector.pass-threshold (was <value>)`
    - `JEV_BASE_URL_INVALID`: `Jev base URL cannot be used: set detector.jev.base-url to an http or https address`. A usable address is an absolute http or https URL with a host. The value is not echoed, because an address can carry credentials (INV-3).
    - `USAGE` problems: `missing --input`, `missing --output`, `unknown option <option>`, `--evaluate takes no value`
    - Exit 2 stderr line (Q7): `<n> false passes at pass threshold <t>: see <output>/evaluation.md`
  - Paths in refusal and summary lines are shown **as typed** (UC-5, user-confirmed 2026-11-06). This also closes DISTILL item 5.
  - **Unexpected exception, phase-dependent:** before bootstrap step 5, `Inspection failed: <short cause>.`; from step 5 on, `Inspection failed: <short cause>. See log for details.` The "See log" clause never appears when no log exists.
  - Other unusable settings, such as `detector.jev.retry.max-attempts=abc`, stay outside the set: they give the generic line and exit 1.
- **DDD-34 (atomic writes, Q5, D25, INV-6):**
  - Reports are written only at the end of a run.
  - Every `OutputFolder` write goes to `<name>.tmp` in the same folder and is renamed with `ATOMIC_MOVE`. On failure the temp file is deleted.
  - Publish order: copies and `.txt` first, `report.json`, then `report.md` last. Evaluate: `evaluation.json`, then `evaluation.md`.
  - An interrupt during this phase may leave a whole `report.json` without `report.md`. **A folder without the summary (`report.md` / `evaluation.md`) is incomplete.** This does not contradict INV-6, which forbids partial files: `report.json` is whole, so INV-6 stays as worded.
  - The write probe exercises the rename, so a filesystem without it refuses at step 4.
  - No signal handler is added: exits 130, 143 and 137 come from the JVM and the OS. The user reruns into a fresh output folder.
- **DDD-35 (`OpenRouterJev` construction):** construction never parses the base URL or validates the key. The `RestClient` is built from the builder at the first attempt, after the probe, because building a client can parse the base URL, and an unusable address must reach the probe as a refusal and not fail during wiring. That order also lets a test bind `MockRestServiceServer` to the builder after constructing the adapter, so the mock is not replaced by the adapter's timed request factory.
- **DDD-36 (`jev/AttemptSource`):** a package-private interface with one attempt and the configuration probe. `RetryingJev` takes it instead of `OpenRouterJev`, so the retry policy has a stub seam without HTTP. **Chosen (C2-4):** `RetryingJev`'s constructor is package-private, with a public factory taking an `OpenRouterJev` for `DetectorConfiguration`. `AttemptSource` stays package-private, and tests in `jev/` use the constructor directly. Implemented in DELIVER.
- **DDD-37 (test stack, Q1):**
  - jqwik is removed. Layer-1 specifications use JUnit `@ParameterizedTest` example tables.
  - `WalkingSkeletonSteps` migrates to `tools.jackson` in DELIVER step 1, which amends DISTILL's "stays as it is". Only its JSON import and mapper change, not its scenario. ADR-006's no-Jackson-2 rule stands, and DELIVER step 1 verifies the test classpath.

## Wave: DESIGN / [REF] Component decomposition

| Component | Path (under `src/main/java/de/aboutlabs/damagedetector/`) | Change type |
|---|---|---|
| Domain: `JevOutcome` (sealed: judged/failed) | `domain/` | CREATE NEW |
| Domain: `Label` (5 classes + folder names) | `domain/` | CREATE NEW |
| Domain: `LabelledOutcome` (path, label, outcome) | `domain/` | CREATE NEW |
| Domain: `Evaluation` + `ThresholdSweep` (pure metrics) | `domain/` | CREATE NEW |
| Domain: `PassPolicy` (judges `JevOutcome`) | `domain/PassPolicy.java` | EXTEND |
| Domain: `Inspection` (holds outcome; failure description) | `domain/Inspection.java` | EXTEND |
| Domain: `BatchReport` (jevErrors, judged) | `domain/BatchReport.java` | EXTEND |
| Domain port: `Jev` (returns `JevOutcome`) | `domain/Jev.java` | EXTEND |
| Domain: `JevJudgement`, `Verdict` | `domain/` | UNCHANGED |
| Batch use case (orchestration only) | `batch/BatchInspection.java` | EXTEND |
| Shared `Inspector` (extracted per-image path) | `inspection/` | EXTEND (extracted from BatchInspection) |
| `ImageFolder`: single read port, root-scoped, returns relative paths and bytes only, no labels; `checkRun`; probe | moved `batch/ImageFolder.java` → `inspection/` | EXTEND |
| `OutputFolder`: single write port, root-scoped; `writeString`, `writeBytes`, atomic; creates the output; write probe | `inspection/` | CREATE NEW |
| `DetectorLauncher` (bootstrap steps 1–5, starts Spring, returns the exit code) | root package | CREATE NEW |
| `LogSink` (pure: the property map naming `<output>/damage-detector.log`) | root package | CREATE NEW |
| `CliArguments` (plan-returning pre-parser shared by the bootstrap and `DetectorCommand`) | `cli/` | CREATE NEW |
| `PassThreshold` (parses the raw setting; probe; gives the `PassPolicy`) | `cli/` | CREATE NEW |
| Build: Spring Boot 4.1.x, Jackson 3, RestClient starter, ArchUnit, verified test libraries, no jqwik | `pom.xml` | EXTEND (DELIVER step 1) |
| `Probe` contract | `probe/` | CREATE NEW |
| `ProbeResult` (ok or refused, closed `Reason` set) | `probe/` | CREATE NEW |
| `AttemptSource`, `AttemptResult` (package-private) | `jev/` | CREATE NEW (counted with RetryingJev) |
| `Sleeper` (waiting capability) | `jev/` | CREATE NEW |
| `ReportWriter` (error count/flag, probe) | `batch/ReportWriter.java` | EXTEND |
| Evaluate use case `LabelledEvaluation` | `evaluate/` | CREATE NEW |
| `EvaluationWriter` | `evaluate/` | CREATE NEW |
| `OpenRouterJev` (single attempt, failure classification, answer validity, plain constructor values, key check moved to probe) | `jev/OpenRouterJev.java` | EXTEND |
| `RetryingJev` (implements `Jev`) | `jev/` | CREATE NEW |
| `DefectQuestion` | `jev/DefectQuestion.java` | UNCHANGED |
| CLI driving adapter `DetectorCommand` (was `BatchCommand`; both modes, probes, exit code) | `cli/` (moved from `batch/BatchCommand.java`) | EXTEND |
| `ErrorFeedback` (pure mapping to `Feedback(line, exitCode)`; prints nothing) | `cli/` | CREATE NEW |
| Composition root (`RetryingJev(OpenRouterJev)`, sleeper, `Clock`, `PassThreshold`; maps properties to plain values) | `DetectorConfiguration.java` | EXTEND |
| Properties (retry block, Jev timeouts, raw threshold) | `DetectorProperties.java` | EXTEND |
| `main` narrowed to `System.exit(DetectorLauncher.launch(args))` | `DamageDetectorApplication.java` | EXTEND |
| Config defaults (threshold, retry, Jev timeouts, console log off; **no log path**; `spring.http.client.*` removed) | `src/main/resources/application.yml` | EXTEND |
| Architecture rules test (ArchUnit) | `src/test/java/.../architecture/` | CREATE NEW |

## Wave: DESIGN / [REF] Driving ports

| Port | Contract | Story |
|---|---|---|
| Batch CLI | `--input=<folder> --output=<folder>` → summary line (DDD-15), exit 0/1/3 or interrupted 130/143/137, report files | WS, US-03, US-05 |
| Evaluate CLI | `--evaluate --input=<labelled> --output=<folder>` → evaluation summary line (DDD-14), `evaluation.md/json`, exit 0/1/2/3 or interrupted 130/143/137 | US-01, US-02 |
| Threshold | `--detector.pass-threshold=<0..1>` (default 0.87 since 2026-11-09, see D20; out of range or not a number → exit 1, one line, DDD-33) | US-02, US-05 |
| Retry | `--detector.jev.retry.max-attempts` (3), `.initial-backoff` (1s), `.max-backoff` (10s) | US-03 |
| Jev connection | `--detector.jev.base-url`, `.model`, `.connect-timeout` (10s), `.read-timeout` (30s) | US-03, US-05 |
| Secret | env `OPENROUTER_API_KEY` | all |
| Container | `docker run … -v <in>:/in:ro -v <out>:/out damage-detector:0.1.0 [same flags]` | US-04 (DEVOPS) |
| Errors | one stderr line + exit code, mapped by `ErrorFeedback`, printed by `DetectorLauncher` or `DetectorCommand`; log at `<output>/damage-detector.log` once it exists | all (INV-5) |

## Wave: DESIGN / [REF] Driven ports + adapters

| Port | Adapter(s) | Contract shape | Probe |
|---|---|---|---|
| `Jev` (bytes + media type → `JevOutcome`) | `RetryingJev` (takes `AttemptSource`) → `OpenRouterJev` → OpenRouter `POST /api/alpha/decisions` | External effect only, zero filesystem delta. `Sleeper` injected. Answer validity per DDD-26. `OpenRouterJev` takes plain constructor values and no `DetectorProperties` (DDD-5, DDD-35) | API key non-blank, then base URL absolute http/https (no paid call) |
| **Input read port (single)** | `ImageFolder`, root = `--input` | Pure read: relative path strings (sorted) and bytes per relative path; no labels. Used by `Inspector`, both use cases (listing) and `ReportWriter` (copy source). `checkRun(output)` is plan-returning | `checkRun`: input exists, output not same or inside. `probe()`: ≥ 1 image. In evaluate mode `LabelledEvaluation` checks for ≥ 1 labelled image instead |
| **Output write port (single)** | `OutputFolder`, root = `--output` | Bounded change, the only writer; `writeString` and `writeBytes`, atomic (DDD-34); rejects escapes from the root | Create the output, then write, rename and delete the probe file |
| Report sink | `ReportWriter` (via `OutputFolder` + `ImageFolder`) | Bounded change: `report.md`, `report.json`, `scrap/**`, `review/**` | Covered by the output probe |
| Evaluation sink | `EvaluationWriter` (via `OutputFolder`) | Bounded change: `evaluation.md`, `evaluation.json` | Covered by the output probe |
| Log file | Spring Boot logging, pointed at the file by `LogSink`'s property map | Logging is the single writer of `<output>/damage-detector.log`; `LogSink` itself is pure | Covered by the output probe (it runs first) |
| Time | `Clock` (run date, elapsed time), `Sleeper` (backoff) | Capability injection, as replaceable beans | n/a |

External integration: OpenRouter Decisions API (alpha). **Contract tests recommended:** consumer-driven contracts, using Spring Cloud Contract or Pact JVM recorded from the WireMock stubs, plus the manual real-Jev smoke per slice.

## Wave: DESIGN / [REF] Technology choices

| Tech | Version | License | Status |
|---|---|---|---|
| Java (OpenJDK) | 21 | GPLv2+CE | existing |
| Spring Boot (starter, Jackson, RestClient starter) | **4.1.x**, latest patch, verified and pinned in DELIVER step 1 (was 3.5.16) | Apache 2.0 | **upgrade** (ADR-006) |
| Spring Framework | 7.x (Boot-managed) | Apache 2.0 | via the upgrade |
| Jackson | 3.x (`tools.jackson`), Boot-managed | Apache 2.0 | upgrade; `ReportWriter` and `WalkingSkeletonSteps` migrate from `ObjectMapper` in DELIVER step 1 |
| JUnit `@ParameterizedTest` | Boot-managed | EPL 2.0 | layer-1 example tables (test). jqwik removed (Q1) |
| Cucumber + JUnit Platform | 7.x compatible with Boot 4.1's JUnit, verified in DELIVER step 1 (7.20.1 today) | MIT | existing (test) |
| WireMock standalone | 3.13.x or newer, verified in DELIVER step 1 (3.13.2 today) | Apache 2.0 | existing (test); faults for 503, timeout, malformed and invalid answers |
| ArchUnit `archunit-junit5` | 1.4.x, exact patch verified in DELIVER step 1 | Apache 2.0 | **new (test)** |
| Spring Retry / Resilience4j / Framework 7 core retry | n/a | n/a | not used (ADR-004) |

## Wave: DESIGN / [REF] Decisions table

| # | Decision | Alternatives rejected | ADR |
|---|---|---|---|
| DDD-1 | Modular monolith, evaluate in same jar | Separate eval tool; Spring Batch; microservices | ADR-001 |
| — | Jev via OpenRouter Decisions API, image in `state` (records the SPIKE decision) | Vision LLM + Jev on text; SDK sidecar; raw HttpClient | ADR-002 |
| DDD-2/4 | Sealed `JevOutcome`; pure metrics and sweep | Exceptions caught per use case; nullable judgement; retry flag in domain | ADR-003 |
| DDD-5/6/7/26 | Plain retry decorator, transient-only, valid-answer check | Framework 7 core retry; Spring Retry; RestClient interceptor; Resilience4j; retry all 4xx | ADR-004 |
| DDD-8–12/22/28/30–34 | Pure error feedback with named printers, `DetectorLauncher` bootstrap, closed refusal set, exit codes, atomic reports, log file in the output via `LogSink` | FailureAnalyzer/ExitCodeExceptionMapper; per-component printing; `ErrorFeedback` printing; key check in constructor; binding the threshold as a double; ambient log path; EnvironmentPostProcessor; temp-dir fallback; test-only property or second entry point | ADR-005 |
| DDD-27 | Spring Boot 4.1 | Stay on 3.5 unpatched; commercial extended support; 4.0 (OSS ends 2026-12-31) | ADR-006 |

## Wave: DESIGN / [REF] Reuse Analysis

HARD GATE. "Contract shape" follows the effect-isolation classification. The assertion is how the crafter or acceptance tests prove the contract.

| Existing component | Overlap with new work | Decision | Evidence / justification | Contract shape · universe · assertion |
|---|---|---|---|---|
| `batch/BatchCommand` | CLI parsing, summary line | EXTEND (moved to `cli/` and renamed `DetectorCommand`) | Already the `ApplicationRunner` driving adapter and the `--input/--output` parser; evaluate is one more flag | Bounded change · universe = stdout, stderr, exit code · acceptance tests assert the stderr line, exit code and Jev call count |
| `batch/BatchInspection` | Per-image judging needed by evaluate | EXTEND (per-image path extracted into `inspection/Inspector`) | `inspect()` already sends only bytes (C11). Today it calls `Files.readAllBytes` directly; after the extraction, reads go through `ImageFolder`, the single read port | `BatchInspection`: orchestration, zero direct IO. `Inspector`: **pure read via the input port; external effect = Jev calls**; zero filesystem delta · WireMock C11 assertion; input tree hash unchanged (DDD-29) |
| `batch/ImageFolder` | Image listing for both modes; single read port | EXTEND (moved to `inspection/`, root-scoped) | The recursive, stable-order PNG/JPEG walk and the missing-folder check exist. **It has no "no images" check today** (an empty folder returns an empty list); that refusal is new. It returns relative path strings only; label resolution moves to `evaluate/` | Pure read · universe = files below `--input`, zero delta · `checkRun(output)`: **unbounded preservation, plan-returning**, returns a `ProbeResult` and writes nothing · input tree hash unchanged after every run, including refusals; ArchUnit rules 2 and 7 |
| `batch/ReportWriter` | Jev error count/flag; copy source | EXTEND | Owns report.md/json and the copy layout; errors are additive fields. It writes via `OutputFolder` (string and bytes, atomic) and **reads the source images via `ImageFolder`** (today: `Files.copy(input.resolve(...))` on raw `Path`s). It renames `summary.totalCost` to `jevCost`. Migrates to Jackson 3 (ADR-006) | Bounded change · `report.*`, `scrap/**`, `review/**` · output tree = declared universe; ArchUnit rules 6 and 7 |
| `jev/OpenRouterJev` | Failure classification, single attempt | EXTEND (implements `AttemptSource`, no longer `Jev`; returns `jev/AttemptResult`) | Request/response mapping is proven (SPIKE). Its constructor changes to a `RestClient.Builder` plus plain values (base URL, model, key, connect and read timeouts), which removes the `DetectorProperties` import, a `jev` → root edge that ArchUnit rule 8 forbids. `DetectorConfiguration` does the mapping. The key check moves to the probe | External effect only · 1 HTTP call per invocation, zero filesystem delta · direct adapter test with `MockRestServiceServer`; WireMock request count |
| Acceptance harness `WalkingSkeletonAcceptanceTest`, `WalkingSkeletonSteps`, `walking-skeleton.feature` | Every new story's acceptance tests (WS strategy B) | EXTEND (DISTILL owns; `WalkingSkeletonSteps` only migrates to `tools.jackson` in DELIVER step 1, DDD-37) | It already starts the real app with CLI args, uses the real filesystem and WireMock for Jev, and asserts the request shape | Test · **not a bootstrap test**: its steps call `SpringApplication.run` directly. Stderr and exit code are covered through `DetectorLauncher` and the real-jar driver |
| Test fixtures `src/test/resources/transistor-images/` | Labelled-folder fixtures | EXTEND (DISTILL owns) | 2 real PNGs exist; labelled layouts are built from them in temp dirs | Test data |
| `jev/DefectQuestion` | none | UNCHANGED | Changing the texts changes the classifier | Constant data |
| `domain/PassPolicy` | Failure → REVIEW; sweep re-application | EXTEND | The single verdict rule (D20) must be reused by the sweep and the headline | Pure function · unit/property test: `Failed` is never PASS for any t |
| `domain/Inspection` | Failure description | EXTEND | Already owns the description per verdict | Pure value |
| `domain/BatchReport` | `jevErrors`, `judged` | EXTEND | Already owns the summary figures | Pure value · INV-4 asserted |
| `domain/Jev` port | Outcome instead of exception | EXTEND (signature) | Port stays bytes + media type only (C11) | Interface contract |
| `domain/JevJudgement`, `Verdict` | none | UNCHANGED | n/a | Pure values |
| `DetectorProperties` | Retry keys, Jev timeouts, raw threshold | EXTEND | Existing `@ConfigurationProperties("detector")` record with a redacted `toString`. `passThreshold` becomes text (DDD-32) | Pure value |
| `DetectorConfiguration` | Wiring decorator, sleeper, clock, `PassThreshold`; maps properties to plain constructor values | EXTEND | Existing composition root | Pure wiring (no IO at construction; the probes run later) |
| `DamageDetectorApplication` | Entry point only | EXTEND (narrowed) | Existing `main`; the bootstrap moves to `DetectorLauncher` | Bounded change · universe = process exit code: `main` is only `System.exit(DetectorLauncher.launch(args))` |
| `application.yml` | Threshold, retry and Jev-timeout defaults; console log off | EXTEND | Existing config (it already has the threshold, the Jev block and the key placeholder). The `spring.http.client.*` keys are removed in DELIVER step 1 | Pure configuration data · contract: defaults only, **no log file path**, the key only as `${OPENROUTER_API_KEY:}`, no secret value · test: `ApplicationDefaultsContractTest` |
| `pom.xml` | Boot 4.1 upgrade, ArchUnit, jqwik removal | EXTEND (DELIVER step 1) | Existing build; the parent and test-library versions are verified on Maven Central (ADR-006) | Build config · `./mvnw verify` green, including the walking skeleton |
| `java.time.Clock` | Run date, elapsed time | REUSE (JDK, no new type) | The JDK capability already fits; it only needs to be a replaceable bean | Capability · tests register a primary fixed clock |
| — | Bootstrap and exit code without ending the JVM | CREATE NEW `DetectorLauncher` (root) | `main` calls `System.exit`, so nothing existing can run the bootstrap in-process and return a code. The scaffold came from DISTILL; DESIGN now owns it (DDD-30) | Bounded change · universe = created output directories, probe file, log path (via the property map), stderr, exit code · output universe and input hash asserted after every run; `launch`'s initializers are a public test seam |
| — | Argument pre-parsing | CREATE NEW `CliArguments` (`cli/`) | `BatchCommand`'s option parsing needs Spring's `ApplicationArguments`, which do not exist before Spring starts | **Unbounded preservation, plan-returning:** `check(args)` returns a `ProbeResult` and writes nothing · example tables in `CliArgumentsProperties`; "refusal before writing" scenarios |
| — | Threshold refusal | CREATE NEW `PassThreshold` (`cli/`) | `PassPolicy` refuses by exception during wiring, and a non-number fails earlier, in the binder. Neither can reach `ErrorFeedback` as a refusal | Pure value · universe = none · table test over `1.5`, `-0.1`, `abc`, `0`, `1`, `0.86` |
| — | Waiting between attempts | CREATE NEW `Sleeper` (`jev/`) | No wait exists; a real `Thread.sleep` would make tests slow and untestable | Capability · universe = elapsed time only · tests register a recording sleeper |
| — | Probe result | CREATE NEW `ProbeResult` (`probe/`) | No value can carry a refusal with its reason and details; exceptions would force effects into the checks | Pure value · closed `Reason` set · `every_refusal_reason_has_a_line` |
| — | Labelled result | CREATE NEW `LabelledOutcome` (`domain/`) | `Inspection` must stay label-free, so a separate value pairs a path, a label and an outcome for scoring | Pure value |
| — | Failure-as-value | CREATE NEW `JevOutcome` | No existing type can express a failed judgement; `JevJudgement` must stay a successful answer | Pure sealed type |
| — | Retry | CREATE NEW `RetryingJev` + `jev/AttemptSource` + `jev/AttemptResult` | No retry exists; a decorator keeps `OpenRouterJev` single-purpose. `AttemptSource` gives the policy a stub seam without HTTP. `AttemptResult` (answered, transient, permanent) carries the classification without polluting the domain | External effect only (via `OpenRouterJev`), zero filesystem delta · unit test with a stub attempt source and an instant sleeper; WireMock scenario asserts the attempt count |
| — | Single write port | CREATE NEW `OutputFolder` (root = `--output`) | Today `ReportWriter` writes via raw `Path`s and `Files` directly, so the bounded-change contracts were conventions only. The read side reuses `ImageFolder` (no separate `InputImages`) | Bounded change, the only writer · `writeString` and `writeBytes`, atomic (temp file + rename), rejects escapes from the root · universe = the declared table in brief.md · ArchUnit rule 6 |
| — | Logging sink | CREATE NEW `LogSink` (root package) | No log file exists today (console logging only). The log path must be set before Spring starts and after the output has passed its probe, so no existing component can own it | **Pure function** returning the property map · the logging system performs the single write; universe of that write = `<output>/damage-detector.log` only; no temp-dir fallback · test: after a refused-at-step-6 run the output holds only the created dirs plus `damage-detector.log` |
| — | No-images / output-inside-input refusals, probe contract | CREATE NEW `probe/Probe` (+ the refusals inside the input/output probes) | No probe or empty-folder check exists | Interface. Implementations: pure checks, except the output probe (bounded: the probe file and its `.tmp`, deleted) |
| — | Ground-truth label | CREATE NEW `Label` | No label concept exists; it must be kept out of `jev..` | Pure value |
| — | Metrics and sweep | CREATE NEW `Evaluation`/`ThresholdSweep` | `BatchReport` describes one threshold without labels; the metrics need labels and many t | Pure function · unit tests with the US-01/02 examples |
| — | Evaluate use case | CREATE NEW `LabelledEvaluation` | A different output, exit rule and label handling from batch; it reuses `Inspector` | Orchestration with zero direct IO; label resolution is pure · effects only via `Inspector` and `EvaluationWriter` |
| — | Evaluation output | CREATE NEW `EvaluationWriter` | `ReportWriter` writes copies and the report.* contract; mixing in evaluation.* would merge two output contracts (slice-01 OUT: no copies) | Bounded change · `evaluation.*` only |
| — | Error-to-message mapping | CREATE NEW `ErrorFeedback` | Messages are scattered in exceptions today, and stack traces reach the console; INV-5 needs one place | **Pure mapping:** refusal or exception → `Feedback(line, exitCode)`. It prints nothing; the printers are `DetectorLauncher` and `DetectorCommand` (ArchUnit rule 3) · `ErrorFeedbackTest` |
| — | Architecture enforcement | CREATE NEW ArchUnit test | No enforcement exists | Test only |

Count (after gate revision cycle 1): **16 EXTEND (14 production incl. `pom.xml`, 2 test), 3 UNCHANGED, 1 REUSE (`Clock`), 17 CREATE NEW** (`JevOutcome`, `RetryingJev`+`AttemptSource`+`AttemptResult`, `OutputFolder`, `LogSink`, `DetectorLauncher`, `CliArguments`, `PassThreshold`, `Sleeper`, `probe/Probe`, `ProbeResult`, `Label`, `LabelledOutcome`, `Evaluation`/`ThresholdSweep`, `LabelledEvaluation`, `EvaluationWriter`, `ErrorFeedback`, the ArchUnit test). Each CREATE NEW is justified above as having no existing alternative. Declared `--output` write universe: see brief.md, "Declared write universe".

## Wave: DESIGN / [REF] Open questions

1. **OQ-1:** resolved by DDD-23 (30 s read timeout); UC-4 confirmed by the user 2026-10-30.
2. **OQ-2:** resolved: UC-1, UC-2 and UC-3 confirmed by the user 2026-10-30. DISTILL uses the UC wording in `design/upstream-changes.md` as the test oracle where it differs from DISCUSS.
3. **OQ-3:** resolved: log file `<output>/damage-detector.log` (user, 2026-10-30). Crafter to verify that logback emits no console status line when the file cannot be opened.
4. **OQ-4:** resolved by DDD-22 (refuse output the same as or inside input, compared on real paths).
5. **OQ-5:** closed by DEVOPS (see `## Wave: DEVOPS / [REF] OQ-5 resolution`).
6. **OQ-6:** the exact Spring Boot 4.1 patch and compatible Cucumber, WireMock and ArchUnit versions. Verified on Maven Central in DELIVER step 1 (ADR-006).
7. **OQ-7:** resolved: UC-5 (paths as typed) confirmed by the user 2026-11-06; the US-05 pitch shows the typed path.

## Wave: DESIGN / [REF] Changed Assumptions

1. **Which Jev errors are retried.** US-03 AC: "A failed Jev call (timeout, connection error, HTTP error, unreadable answer) is retried…". **New:** only transient errors are retried (timeouts, connection errors, HTTP 408/429/5xx, unreadable answer). Other 4xx errors fail after 1 attempt. Rationale: retrying a 401 cannot succeed and costs ≥ 3 s per image (K5). The US-03 technical note leaves this to DESIGN. Scenario outcomes are unchanged (UC-1).
2. **"Non-zero exit" is refined.** D14, D18 and INV-5 say "non-zero". **New:** 1 refused/unexpected, 2 false pass, 3 none judged (UC-2).
3. **Missing-key check timing.** In the skeleton it happens in the `OpenRouterJev` constructor (wiring, which brings a stack trace). **New:** it is a startup probe in the CLI, so the INV-5 one-line message holds.
4. **C11 test wording.** The US-01 scenario says: "no request … contains … a folder name such as "damaged_case" or "good"". **New:** the fixed class vocabulary is allowed; per-image names, paths and labels are not (UC-3).
5. **Jev read timeout.** The skeleton and the US-03 example use 120 s. **New:** 30 s, to keep K5 (UC-4).
6. **Framework version.** The SPIKE skeleton and DISCUSS assume Spring Boot 3.5.16. **New:** Spring Boot 4.1.x, as the first DELIVER step (ADR-006, user decision 2026-10-30). There is no story or AC change.
7. **Report writing.** The skeleton writes reports directly under their final names. **New:** only at the end of a run, through a temp file and a rename, with the `.md` file last (D25, Q5).
8. **Cost field name.** The skeleton's `report.json` uses `summary.totalCost`. **New:** `summary.jevCost`, the same word as `evaluation.json` (Q12).
9. **Paths in messages.** The skeleton's summary line shows the absolute path, and the US-05 pitch shows an absolute path for a relative input. **New:** paths as typed (UC-5).
10. **Test library.** DISTILL used jqwik. **New:** JUnit `@ParameterizedTest` (Q1).

## Wave: DESIGN / [REF] Wave decisions summary

- **Peer review:**
  - Iteration 1: rejected (3 critical, 5 high), fixed by DDD-19 to DDD-25.
  - Iteration 2: rejected (1 critical, 5 high, plus lower findings), fixed in one targeted pass by DDD-26 to DDD-29, ADR-006 and the brief rewrite. See the resolutions list below.
  - There is no third review; the orchestrator verifies.
  - Final wave gate: `needs_revision` (3 blockers, 4 high, 14 lower). Revision cycle 1 is applied by DDD-30 to DDD-37 and the amended DDD-14 and DDD-15; see the resolutions below. The gate then gave conditional approval, and its last conditions (C2-1 to C2-5, DEVOPS N1) are applied.
- **Key decisions:** DDD-1 to DDD-37, Option A (confirmed by the user 2026-10-30). ADR-001 to ADR-006 are in `docs/product/architecture/`.
- **Architecture summary:**
  - One Spring Boot 4.1 CLI jar, structured as a modular monolith with ports and adapters.
  - Two use cases (batch and evaluate) share one inspection path.
  - Single read port `ImageFolder` and single write port `OutputFolder`.
  - The `Jev` port returns a sealed outcome; retry is a decorator with an answer-validity check.
  - Metrics and the sweep are pure domain functions.
  - All errors go through one pure mapping, printed by two named printers. `DetectorLauncher` runs a pre-Spring bootstrap that checks the paths, creates the output and sets the log file before any paid call.
  - Reports are written only at the end of a run, atomically.
  - C4 L1, L2 and L3 are in `docs/product/architecture/brief.md`.
- **Tech stack:** Java 21, Spring Boot 4.1.x (Spring Framework 7, Jackson 3), Cucumber 7.x, WireMock 3.13.x or newer, ArchUnit 1.4.x (test). The exact versions are verified in DELIVER step 1. No new runtime library beyond the Boot 4 starters.
- **Constraints:** C1, C2, C3, C7, C10, C11, C12; INV-1 to INV-6; no DB, web or Qwen; sequential Jev calls; the key comes only from the env.
- **Upstream changes:** UC-1 to UC-4 in `design/upstream-changes.md`, confirmed by the user 2026-10-30, and the UC-1 amendment, confirmed 2026-11-06 (Q3). UC-5 (paths as typed) is a DESIGN pin still to be confirmed (OQ-7). See also Changed Assumptions.
- **DELIVER order:** step 1 is the Spring Boot 4.1 upgrade with the walking skeleton staying green. Then slices 01 → 02 → 03 → 05 → 04.
- **DELIVER removals in `OpenRouterJev`:** three things go once the `Jev` port returns `JevOutcome`: `implements Jev`, the `DetectorProperties` constructor, and `judge()`. DISTILL kept them so the skeleton still runs.
- **Handoff:**
  - **DISTILL (acceptance-designer) gets:**
    - the INV-5 rows plus exit codes
    - WireMock fault scenarios: 503 ×3, 503 then OK, 401, timeout, malformed body, the invalid answers of DDD-26, and 429 with `Retry-After` above the cap
    - labelled-folder fixtures
    - the C11 request assertion
    - the input-hash and output-universe assertions after full and refusal runs (DDD-29)
    - the output-same-as/inside-input refusal
  - **DEVOPS (platform-architect) gets:**
    - the US-04 contract: env key, `/in:ro`, `/out` writable by the host user, log at `/out/damage-detector.log`
    - a Java 21 base image for Boot 4.1
    - the contract-test annotation for the OpenRouter Decisions API
    - the 8 ArchUnit rules in `./mvnw verify`

### Review iteration 2 resolutions

| Finding | Fixed in |
|---|---|
| N1 (critical) logging sink | brief.md: component table (`LogSink`), "Bootstrap sequence and logging" (step 5, no temp fallback), "Declared write universe"; ADR-005 decisions 2.5 and 6 (`application.yml` contract) and alternatives 4–6; feature-delta DDD-11, DDD-28, Reuse rows `LogSink` (CREATE NEW) and `application.yml` (contract defined) |
| N2 labels only in `evaluate/` + ArchUnit | brief.md: component table (`ImageFolder` returns relative paths, `LabelledEvaluation` is the only label resolver), Driven ports, Enforcement rule 2; feature-delta DDD-3, DDD-20, Component decomposition (`ImageFolder` row), Reuse row `ImageFolder` |
| N3 `ReportWriter` via capabilities; one read port | brief.md: Driven ports ("Input read port (single)" = `ImageFolder`), C4 L3 (writers → folder); feature-delta DDD-20 (`InputImages` merged), Driven ports table, Reuse rows `ReportWriter` and `OutputFolder` |
| N4 rules 1/6/7, rule count | brief.md: Enforcement rules 1–8; feature-delta DDD-17 (same 8 rules) |
| N5 valid Jev answer | ADR-004 Decision ("A valid answer…") with the WireMock cases; brief.md "Integration patterns"; feature-delta DDD-26 |
| N6 real-path same-or-inside refusal | brief.md "Bootstrap sequence" step 2 and Earned Trust table; ADR-005 decision 2.2; feature-delta DDD-22 |
| N7 log bootstrap and a not-yet-existing output | brief.md "Bootstrap sequence" steps 1–5; ADR-005 decision 2 and alternatives 4–5; feature-delta DDD-28 (the output is created first via `OutputFolder`, never buffered) |
| N8 `Inspector` label + DISTILL assertions | brief.md component table and "Declared write universe" (DISTILL assertions); feature-delta DDD-29, Reuse row `BatchInspection` |
| N9 dependency edges incl. `probe` | brief.md "Allowed dependency edges" table; feature-delta DDD-21 |
| N10 cost of failed attempts | ADR-004 Decision ("Cost") and Consequences; brief.md "Integration patterns"; feature-delta DDD-15 |
| N11 Spring Boot 4.1 | ADR-006 (new); brief.md Technology stack, C4 L2; ADR-004 alternative 1; feature-delta DDD-27, Technology choices, Decisions table, Changed Assumption 6, OQ-6 |
| N12 break-even of 3 hanging images | ADR-004 Consequences; brief.md quality-attribute driver 6; feature-delta DDD-23 |
| N13 harness wording | ADR-003 Consequences |
| N14 ArchUnit version verified in DELIVER step 1 | brief.md Technology stack; ADR-006 Decision; feature-delta DDD-17, Technology choices, OQ-6 |
| N15 `Retry-After` above the cap | ADR-004 Decision ("Backoff"); brief.md "Integration patterns"; feature-delta DDD-6 |
| N16 output dir creation, probe-file leftover | brief.md "Bootstrap sequence" steps 3–4 and "Declared write universe"; ADR-005 decisions 2.3–2.4; feature-delta DDD-29 |
| N17, N18 | No such findings: the iteration-2 report numbering skipped from N16 to N19. Nothing to resolve (coordinator, 2026-11-06). |
| N19 UC confirmation | already resolved: `design/upstream-changes.md` (status confirmed 2026-10-30), OQ-2 |

### Final wave gate revision cycle 1 resolutions (2026-11-06)

| Finding | Fixed in |
|---|---|
| Blocker: `DetectorLauncher` unowned | DDD-10, DDD-28, DDD-30, Component decomposition, Reuse rows `DetectorLauncher` and `DamageDetectorApplication`; brief.md "Bootstrap sequence and logging"; ADR-005 decisions 2–3 |
| Blocker: `ErrorFeedback` pure or printing | DDD-8, DDD-17 rule 3, Reuse row; brief.md component table and Enforcement rule 3; ADR-005 decisions 1 and 7 |
| Blocker: Reuse hard gate | Reuse rows `CliArguments`, `Sleeper`, `ProbeResult` (plus `PassThreshold`, `LabelledOutcome`, `Clock`), DDD-31, Component decomposition, recount |
| High: closed refusal set | DDD-33, DDD-6 (non-numeric `Retry-After`); ADR-005 decision 1; ADR-004 Backoff |
| High: unexpected exception before step 5 | DDD-33; brief.md "Catch points and phases" and universe table; ADR-005 decision 1 |
| High: Jackson in tests | DDD-37, DDD-27; ADR-006; Reuse row harness |
| High: `OpenRouterJev` constructor | DDD-5, DDD-35, Reuse row; brief.md Driven ports; ADR-004 Decision |
| Q1 jqwik | DDD-37, Technology choices, Changed Assumption 10; brief.md Technology stack; ADR-006 |
| Q3 429 above the cap | DDD-6; `design/upstream-changes.md` UC-1 amendment; ADR-004; brief.md "Integration patterns" |
| Q5 atomic reports, interrupted runs | DDD-9, DDD-34; brief.md "Writing reports", exit codes, universe table; ADR-005 decisions 5–6 |
| Q7 exit 2 stderr line | DDD-33; brief.md exit codes; ADR-005 decision 1 |
| Q8 disclosure line | DDD-14 item 11 |
| Q12 report and evaluation contract | DDD-14, DDD-15 |
| M1 INV-5 texts | DDD-33; ADR-005 decision 1 |
| M5 log in the evaluate universe | DDD-14 |
| `LogSink` pure | DDD-11, DDD-28, Reuse row, Driven ports; brief.md component table and rule 6; ADR-005 decision 2.5 |
| `OutputFolder` byte write | DDD-20, Driven ports; brief.md Driven ports |
| Rule 7 path inspection | DDD-17 rule 7; brief.md Enforcement rule 7 |
| `RetryingJev` attempt-source interface | DDD-5, DDD-36; brief.md; ADR-004 |
| N17/N18 note | The iteration-2 table above |
| DDD-1 package list | DDD-1 |
| ADR-004: K5 and `Retry-After` | ADR-004 Consequences; DDD-23 |
| Containment check normalization | DDD-22; brief.md bootstrap step 2; ADR-005 decision 2.2 |
| Skeleton is not a bootstrap test; on-write directories | brief.md "Bootstrap sequence" and universe section; DDD-29; Reuse row harness |
| `spring.http.client.*` removal | DDD-27; ADR-005 decision 8; ADR-006; Reuse row `application.yml` |
| ADR-004 old US-03 wording | ADR-004 Context |
| DDD numbering | DDD-30 to DDD-37 appended, no renumbering |

### Scaffold and test changes for the acceptance designer

Production scaffolds:

1. `probe/ProbeResult.Reason`: add `THRESHOLD_INVALID`.
2. `cli/ErrorFeedback`: add `feedbackBeforeLog(Throwable)` (no "See log" clause) and `falsePasses(int count, double threshold, String reportPath)` returning exit 2. The class stays pure.
3. New scaffold `cli/PassThreshold` (implements `Probe`; `of(String raw)`, `policy()`).
4. `inspection/OutputFolder`: add `writeBytes(String relativePath, byte[] content)`. Both writes are atomic. `probe()` also exercises the rename. A getter for the real root path is needed by the launcher.
5. New `jev/AttemptSource` (package-private) and `jev/AttemptResult`. `RetryingJev`'s constructor takes `AttemptSource attempts` instead of `OpenRouterJev`.
6. `domain/JevOutcome.Judged`: add `int attempts`, keeping a one-argument constructor that means 1, so `EvaluationProperties` still compiles.
7. `DetectorLauncher`: its javadoc now owns steps 1–5 and printing. The signature stays. `LogSink`'s javadoc changes from "bounded change" to "pure function".
8. Moved `ImageFolder` (no stub today): `rootedAt(Path)`, `checkRun(String output)`, listing as relative strings, bytes per relative path, `probe()`.

Tests:

9. `JevAdapterFixture`: construct `OpenRouterJev` with plain values, and call `MockRestServiceServer.bindTo(builder)` **after** constructing it, so the mock is not replaced by the adapter's timed request factory (DDD-35).
10. Remove jqwik: rewrite `RetryingJevProperties`, `ErrorFeedbackProperties`, `CliArgumentsProperties`, `EvaluationProperties` and `LabelResolutionProperties` with `@ParameterizedTest`. Remove the dependency and `jqwik.version` from `pom.xml` and the setting from `junit-platform.properties`. Update the "jqwik" wording in the DISTILL sections.
11. `WalkingSkeletonSteps`: amend DISTILL's "stays as it is" (Test placement section). It migrates to `tools.jackson` in DELIVER step 1.
12. `ErrorFeedbackTest`: add rows for `JEV_BASE_URL_INVALID` and `THRESHOLD_INVALID` with the pinned texts, plus a `feedbackBeforeLog` test and an exit-2 line test.
13. `RetryingJevTest`: add cases for a non-numeric, a date and a negative `Retry-After` (normal backoff applies). Add that a success after a failure is `Judged` with `attempts` 2.
14. `OutputFolderTest`: add atomic-write cases: `writeBytes`, the root-escape rule on `writeBytes`, no `.tmp` left after a write, the temp file deleted after a failed write, and the old file whole when a replacement fails.
15. `EvaluationRecord`: assert the Q8 disclosure line, the `runDate` field, `costIsLowerBound`, and the `(lower bound)` wording. `BatchRecord`: add `summary.jevCost` and `summary.costIsLowerBound` (its existing assertions already fit). `Row.goodCount` stays; the writer maps it to `goodTotal`.
16. New scenarios:
    - Exit 2 prints `<n> false passes at pass threshold <t>: see <output>/evaluation.md`.
    - A success after a failed attempt shows the cost as a lower bound.
    - An interrupted run on the real jar (SIGINT or SIGTERM during a slow stub) leaves no `report.md` or `report.json` and ends with 130 or 143. This closes DISTILL's gap C7b.
    - The pinned texts replace "one line about the threshold" and "one line about the Jev address".
    - An output with `..` in its not-yet-existing part still counts as inside the input.
17. `DomainTypes.RunDeclaration`: add an interrupted-run declaration, which may hold one `*.tmp`.
18. `@Disabled` and feature comments still say slice 03 for refusal rows; those belong to slice 05 (D24).
19. `distill/upstream-issues.md`: items 1–5 are closed by DDD-14, DDD-15, DDD-6 and DDD-33; items 6 and 7 by Q7 and Q5; items 9 and 12 by DDD-30 and DDD-5; item 11 by the brief; items 14, 17 and 18 by Q1 and DDD-37.

## Wave: DEVOPS / [REF] Environment matrix

Date 2026-11-06. Density: lean (Tier-1 only, no menu). This wave is design only. The Dockerfile, `.dockerignore` and workflow YAML are built in DELIVER, as are the bodies of `scripts/container-check.sh` (a RED scaffold exists). Machine version: `environments.yaml` in this folder.

| Environment | Platform | Runs | Jev | Key source | Input / output | Purpose |
|---|---|---|---|---|---|---|
| `local-jar` | WSL2 (verified host), Linux | `java -jar target/damage-detector-*.jar` | Real; WireMock in `./mvnw verify` | `.env` exported into the shell | Host paths | Development, production-data ACs of slices 01–03 and 05 |
| `docker-run` | WSL2 with Docker Desktop (verified host), Linux with Docker Engine | `docker run … damage-detector:0.1.0` | Real; WireMock in `container-check` | `--env-file .env` or `-e OPENROUTER_API_KEY` | `/in` read-only, `/out` writable | Showcase, slice-04 production-data AC |
| `ci` | GitHub Actions `ubuntu-latest` (active once a remote exists) | `./mvnw verify`, `docker build`, `container-check` | **Stub only** | No secret. A throwaway canary value satisfies the key probe | Fixtures in `src/test/resources` | Gates. Proves mechanisms, not real accuracy |

No macOS or Windows-native coverage is claimed. The real-Jev numbers (K1, K2, K5) come only from hand runs in `local-jar` or `docker-run`.

## Wave: DEVOPS / [REF] CI/CD pipeline outline

One workflow, `.github/workflows/ci.yml`, with `permissions: contents: read`, no `secrets.*` reference, and PR concurrency cancellation. Actions are pinned by commit SHA when the file is created. Nothing is published: there is no registry and no release job.

| Trigger | `verify` | `mutation` | `image` |
|---|---|---|---|
| Pull request to `main` | yes | yes (changed classes) | yes |
| Push to `main` (merge) | yes | no (already gated on the PR) | yes |
| Push to a feature branch without a PR | no | no | no |
| Tag | no | no | no |

| Job | Stages | Blocks on |
|---|---|---|
| `verify` | Checkout with full history → secret scan of the history (gitleaks; version and SHA named in `ci.yml` when it is created, latest release seen today v8.30.1) → Temurin 21 with Maven cache → `./mvnw -B verify` (compile, unit, the 8 ArchUnit rules, Cucumber/WireMock acceptance) → upload test reports on failure | Any failing test or rule; any secret finding |
| `mutation` (needs `verify`) | See Mutation testing strategy | Kill rate < 80 % on changed classes |
| `image` (needs `verify`; added with the Dockerfile in slice 04) | `scripts/container-check.sh`, below | Any failed step |

`scripts/container-check.sh` runs the same way in CI and on the demo host:

1. **Isolated build context.** The script never writes to the working tree, and the real `.env` is never written, moved or copied.
   - Record the `sha256sum` of the real `.env`, if there is one. Keep it in a variable and never print it.
   - `CTX=$(mktemp -d)` (mode 700). A `trap` on `EXIT INT TERM` removes `$CTX` and the script's own containers, network and check images.
   - Copy tracked and untracked, non-ignored files, skipping deleted ones:

     ```bash
     git ls-files -co --exclude-standard -z \
       | while IFS= read -r -d '' f; do [ -e "$f" ] && printf '%s\0' "$f"; done \
       | tar --null --exclude='.env' --exclude='.env.*' -T - -cf - | tar -xf - -C "$CTX"
     ```

     `--exclude` must come before `-T`. `images/`, `out/`, `target/` and the real key therefore never reach the copy.
   - Plant a per-run canary key **in the copy only**: `$CTX/.env` and `$CTX/src/main/resources/.env`. CI has no real key, so a scan without a canary would pass even with a broken `.dockerignore`.
2. **Build from the copy:** `docker build` on `$CTX`, tagged `damage-detector:check-<run-id>`. A check never moves `:0.1.0`. Optional negative step: build once more with `--build-arg OPENROUTER_API_KEY=<canary>`. An undeclared argument leaves no trace, so a stray `ARG` in the Dockerfile fails the scan.
3. **K4 / INV-3 image scan.** Fail if any of these is found:
   - the canary, `sk-or-` or the bare name `OPENROUTER_API_KEY` in `docker image inspect` or `docker history --no-trunc`. The bare name catches an `ENV` or `ARG` line even without a value;
   - a file named `.env`, or the canary, in **any layer** of `docker save`. A flattened `docker export` would miss a file deleted in a later layer;
   - the canary inside `/app/app.jar`, unpacked first, because a jar is compressed and a plain grep cannot see into it. The bare name is not scanned here, because `application.yml` legitimately holds the `${OPENROUTER_API_KEY:}` placeholder.

   **Real-key scan, demo host only.** If a real `.env` exists, also build from the **untouched original** tree, which is the image that gets shared, and scan it for the real key. `docker build` only reads the tree. The key goes to `grep -F -f -` on stdin, never in an argument, and the scan prints only pass or fail. CI has no `.env` and skips this.
4. **Stub parity (K4).** Start `wiremock/wiremock:3.13.2@sha256:0d4ecb3e4dc8213fd7a4d37d6a78f6e6b553a6d2e15bd51b0999781282ac61b3` on a user-defined Docker network (digest read from Docker Hub; DELIVER re-checks it and keeps the version equal to `wiremock.version` in `pom.xml`), using the same mappings for both runs. Run the jar (`--detector.jev.base-url=http://localhost:<port>`) and the image (`--detector.jev.base-url=http://wiremock:8080`; dotted properties pass DDD-12). Compare the `report.json` summary (`total`, `pass`, `scrap`, `review`, `judged`, `jevErrors`) and the set of output files. Do not compare dates or absolute paths.
5. **Run-time INV-3.** The stub runs use the canary as the key, one of them with `--debug`. Fail if the canary appears in `/out` or in the captured stdout or stderr.
6. **Contract rows.** Assert a missing key gives exit 1 and the INV-5 line, a stub `401` on every call gives exit 3, a stub false pass under `--evaluate` gives exit 2, and every file in `./out` can be deleted by the invoking user.
7. **Last check, on every exit path.** Hash the real `.env` again and fail if it changed. If there was none, none may exist now.

**Local gates.** No hook framework: a single developer, and the commands above are the whole gate. Run `./mvnw verify` and `scripts/container-check.sh` by hand. **Until a GitHub remote exists, this local run is the only gate.**

**Security, sized to the risk.** Gitleaks plus the K4 scan cover the one asset at risk, the key. **Dependabot alerts are switched on when the public repo is created, before the first PR** (user decision Q6), so known-vulnerable dependencies in `pom.xml` are reported from the first push. They do not cover the pinned base images, which stay a manual patch check. SAST, DAST, SBOM and image CVE scanning are left out: there is no service surface and no published image. Revisit if the image is ever published.

## Wave: DEVOPS / [REF] Monitoring contracts

No external stack. A KPI is read from a run artifact or a CI check. CI proves the mechanism with a stub; only a real run proves the value. Full contract: `docs/product/kpi-contracts.yaml`.

| KPI | Target | Value measured from | Mechanism proved by | Signal when breached |
|---|---|---|---|---|
| K1 false passes (north star) | 0 | `evaluation.json` images with label ≠ `good` and verdict PASS; summary line; hand run, real Jev | `verify`: stub false-pass scenario | Exit code 2 + list in `evaluation.md` |
| K2 good auto-pass | Reported; ≥ 50 % informative, no gate | `evaluation.md` sweep row at the configured t | `verify`: sweep scenarios | None (not a gate) |
| K3 complete batch under Jev failures | 100 % complete, 0 errored PASS | `report.json` `jevErrors`, `judged`, per-image `jevError`; slice-03 real run | `verify`: 2 of 10 calls failing | Failing test; exit 3 if none judged |
| K4 container parity and no key | 100 % equal counts, 0 key occurrences | `container-check` steps 3–5 | `image` job | Job fails. A leak in a shared image needs the key rotated (see Deployment strategy) |
| K5 cost and time (guardrail) | ≤ $0.05 and ≤ 10 min per 253 images | `jevCost` and `elapsedSeconds` in `evaluation.json`, shown in `evaluation.md` (user decision 2026-11-06). Copied into the slice evidence | Not measurable in CI (a stub has no cost or latency) | Read by hand. The cost is a lower bound after retries (DDD-15) |
| K6 one-line refusal | 100 % of the US-05 INV-5 rows: one line, exit 1, no stack trace, 0 Jev calls | Stderr line, exit code and WireMock call count per row; slice-05 hand run | `verify`: acceptance row per refusal; `container-check` missing-key and forgotten-mount rows | Failing test |
| G1 no path or label in Jev requests (guardrail) | 0 | WireMock request assertion (UC-3) | `verify`: acceptance test + ArchUnit rule 2 | Failing test |

## Wave: DEVOPS / [REF] Deployment strategy

**Strategy: recreate.** A batch process has no running service to shift traffic from, so canary, blue-green and rolling do not apply. A release is a new image and the next run uses it.

**Rollback contract (defined first).**

- Every build gets two tags: the moving demo tag `damage-detector:0.1.0` and the immutable `damage-detector:0.1.0-<git-sha12>`. Rollback needs the second one, because rebuilding overwrites the first. A build from a dirty tree gets the suffix `-dirty`: such a tag is not immutable, so it is never a rollback target and is not shared. Keep at least the previous clean SHA tag on the host.
- **Rollback:** `docker tag damage-detector:0.1.0-<previous-sha> damage-detector:0.1.0`, then rerun. There is no data migration and no state.
- **Output:** reports are not migrated. Rerun into a fresh `--output` (about $0.04 for all 253 images).
- **Triggers:** `container-check` fails, an exit 1 on a command that worked before, or verdict counts differ from the jar. Exit 130, 143 or 137 is an interruption, not a rollback trigger.
- **Drill:** before slice 04 closes, build two SHAs, roll back, and run one fixture through the rolled-back tag.
- **Rollback does not fix a leak.** If a key reached an image, rotate it at OpenRouter first, then delete the image and every shared copy.

**Sharing the image without a registry:** `docker save damage-detector:0.1.0 | gzip > damage-detector-0.1.0.tar.gz`, then `docker load`. Alternatively clone and `docker build` on the target, which needs Docker and access to Maven Central, but no JDK.

## Wave: DEVOPS / [REF] Docker contract (slice 04)

Requirements for the files DELIVER creates. The "Verified" column records tests run on 2026-11-06 on the WSL2 host (Docker Desktop 29.9.2, project on the ext4 filesystem).

| Topic | Contract | Verified |
|---|---|---|
| Build | Multi-stage. Stage 1 runs `./mvnw -B -ntp -Dmaven.test.skip=true package` (tests already ran in `verify`), with a BuildKit cache mount on `/root/.m2`. Stage 2 copies only the repackaged jar to `/app/app.jar` | `mvnw` is tracked as executable. The JDK image has `curl` and `tar` but no `unzip`, so `mvnw` takes its `.tar.gz` fallback, and that URL resolves. **The build itself is not yet run** |
| Base images | Build: `eclipse-temurin:21.0.13_9-jdk-noble@sha256:114cabb73033ca4ce7ceec3deb62396bc1b716282bae051ed3c1d82e38fcab2d`. Run: `eclipse-temurin:21.0.13_9-jre-noble@sha256:0eda5830ed7b669abc246a8dc520ea8a23cab06f9a8d4b6964109e2f2394e46f` | Tags and digests read from Docker Hub. The JRE image was pulled and reports OpenJDK 21.0.13 LTS. **Flag:** re-check for a newer patch at slice 04. A pin means patches are manual, and Boot 4.1 on this image is proved only by `container-check` |
| Entry point | Exec form: `ENTRYPOINT ["java","-jar","/app/app.jar"]`, no `CMD`, so no arguments gives the usage refusal. It replaces the base `/__cacert_entrypoint.sh`, which only matters for `USE_SYSTEM_CA_CERTS` | Exit code propagation verified with a stub command (`exit 3` → host `rc=3`) |
| JVM flags | Do **not** set `JAVA_TOOL_OPTIONS`: the JVM prints `Picked up …` on stderr, which breaks the one-line rule of INV-5. Put any flag in the entry point | Known JVM behaviour, not tested here |
| User | `USER 1000:1000`, the base image's existing `ubuntu` user, so no `useradd` is needed. The documented commands add `--user "$(id -u):$(id -g)"` | See the next four rows |
| Writing to `/out` | The numeric UID passes through to the host. `--user` with the host UID writes files the host user owns and can delete | Verified. A root container left root-owned files the host user could **not** delete, which is why the image is non-root |
| Wrong UID | Fails safe: the write probe gives `Cannot write to output folder /out: permission denied`, exit 1 | Verified at shell level (UID 1234 on a UID 1000 folder: denied) |
| `./out` missing | Docker creates it **root-owned** and the run is refused. So every documented command starts with `mkdir -p out` | Verified |
| Unknown UID | `user.home` is `/` and `user.name` is `?`. Harmless, because the app writes only below `--output` and the JVM uses `/tmp` | Verified for the JVM. Confirm with the real app in `container-check`, which CI covers (runner UID differs from 1000) |
| Mounts | Do not create `/in` or `/out` in the image, and declare no `VOLUME`. A forgotten `-v` is then refused (`/` is not writable), instead of writing into a layer that `--rm` throws away | Follows from the rows above; asserted in `container-check` |
| Secret | `OPENROUTER_API_KEY` comes only from `--env-file .env` or `-e OPENROUTER_API_KEY` (name only, passed through from the shell). Never `-e KEY=value`, which puts it in shell history. Never `ENV`, `ARG` or `--build-arg` | `.env` format matters: `export KEY=…` is rejected by Docker (exit 125), and quotes become part of the value (a later 401). The current `.env` is valid: plain `KEY=value`, no CRLF |
| `.dockerignore` | Allow-list: `*`, then `!pom.xml`, `!mvnw`, `!.mvn`, `!src/main`. Repeat the explicit denials after it: `**/.env`, `**/.env.*`, `images/`, `out/`, `target/`, `.git/`. The MVTec test fixtures therefore stay out as well | Verified with a scratch build: only `pom.xml`, `mvnw`, `.mvn/` and `src/main/` entered the context; a root `.env`, a nested `.env`, `.env.local`, `images/`, `out/`, `target/` and `src/test/` did not. `container-check` keeps this proved with the canary |
| Metadata | `ARG GIT_SHA` feeds the label `org.opencontainers.image.revision`. No `EXPOSE`, no `HEALTHCHECK` (batch process) | n/a |
| Network | Outbound HTTPS to `openrouter.ai`. No ports published | Proved by the slice-04 production-data AC |

**Exit codes.** `docker run` returns the application's code unchanged: 0 OK, 1 refused or failed, 2 false pass, 3 none judged (precedence 1 > 3 > 2). Scripts must read **125–127** as a Docker failure (125 verified with a bad env file) and **130, 143 and 137** as interrupted (see Interrupt contract). None of these is a detector result. Stdout and stderr pass through unchanged, because `-t` is not used.

**Limit in Docker (DDD-22).** The real-path check inside the container sees `/in` and `/out` as unrelated mounts. It cannot detect that the **host** output folder lies inside the host input folder. Keep `./out` beside `./images`, never inside it. The `outside` guard in the demo commands checks this on the host, before `docker run`. It works only where it is used: a hand-typed `docker run` has no protection.

**Demo commands**

```bash
# Build: needs Docker only, no JDK or Maven on the host
SHA=$(git rev-parse --short=12 HEAD)
[ -z "$(git status --porcelain)" ] || SHA="$SHA-dirty"
docker build --build-arg GIT_SHA="$SHA" -t damage-detector:0.1.0 -t "damage-detector:0.1.0-$SHA" .

# Host-side guard: DDD-22 cannot see this from inside the container
outside() { case "$(realpath -m "$2")/" in "$(realpath "$1")"/*)
  echo "Refused: $2 is the same as or inside $1" >&2; return 1;; esac; }

# Batch inspection
outside images out && mkdir -p out &&
docker run --rm --user "$(id -u):$(id -g)" --env-file .env \
  -v "$PWD/images:/in:ro" -v "$PWD/out:/out" \
  damage-detector:0.1.0 --input=/in --output=/out
echo "exit code: $?"

# Evaluation (K5: jevCost and elapsedSeconds land in evaluation.json)
outside images out && mkdir -p out &&
docker run --rm --user "$(id -u):$(id -g)" --env-file .env \
  -v "$PWD/images:/in:ro" -v "$PWD/out:/out" \
  damage-detector:0.1.0 --evaluate --input=/in --output=/out/eval
echo "exit code: $?"   # 2 = false pass at the configured threshold
```

Verified with fake data: the guard refuses the same folder, a child folder and a symlinked path, and accepts a sibling such as `images-b`. The `-dirty` logic ignores `.env`, because it is gitignored.

Use a fresh output folder per run: old `scrap/` and `review/` files stay in place when a folder is reused. Windows paths below `/mnt/c` are untested, so keep the project on the WSL filesystem. The write probe refuses a mount that does not work.

## Wave: DEVOPS / [REF] Interrupt contract

Closes DISTILL gap C7b (user decision Q5; D25, INV-6, DDD-34). The tool adds no signal handler: the JVM and Docker set these codes, and the run prints no line of its own.

| Exit | Signal | Typical cause | Under `docker run` |
|---|---|---|---|
| 130 | SIGINT | Ctrl-C | Forwarded by the client, verified |
| 143 | SIGTERM | `kill` on the client, `docker stop` | Forwarded by the client, verified; `docker stop` verified |
| 137 | SIGKILL | `docker kill`, out of memory, `docker stop` after its grace period | `docker kill` verified |

- **Not a detector result.** These codes mean interrupted: no verdict, no KPI value, no rollback trigger.
- **Incomplete means no summary.** Reports are written only at the end, through a temp file and a rename, and `report.md` or `evaluation.md` comes last. A folder without that file is incomplete, whatever `scrap/`, `review/`, `report.json` or `*.tmp` it holds.
- **Recovery:** rerun into a fresh `--output`. There is no resume. Paid calls already made are lost, at most about $0.04 for all 253 images.
- **Signals:** `docker run` forwards signals to the container by default (`--sig-proxy=true`). The exec-form entry point makes the JVM PID 1, so it receives them.
- **Exception:** SIGKILL on the `docker run` client cannot be forwarded. The client dies, but **the container keeps running and keeps calling Jev** (verified). Check with `docker ps --filter ancestor=damage-detector:0.1.0`, then `docker kill <id>`.

"Verified" means tested on the WSL2 host on 2026-11-06 with a stand-in Java program as PID 1 under UID 1000, not with the real app.

**Manual demo check (slice 04 evidence).** Start the batch command on real images and press Ctrl-C after a few images. Expect `exit code: 130`, no `report.md`, and an empty `docker ps`. Then rerun into a fresh folder and expect `report.md`. This stays out of `container-check`, because it is timing-dependent and costs real calls. The stub-based interrupt scenario on the real jar stays with DISTILL (DESIGN handoff item 16).

## Wave: DEVOPS / [REF] Mutation testing strategy

**Scope (user decision 2026-11-06):** PIT is limited to classes with direct tests (domain, `jev/`, `cli/` logic, `OutputFolder`). Classes covered only by Cucumber scenarios are outside the 80 % gate, because PIT does not count Cucumber.

**Per-feature** (already in `CLAUDE.md`): PIT runs after refactoring in each delivery, on modified files only, with a kill rate gate of ≥ 80 %. This fits a codebase far below 50k LOC.

- **Scope:** production Java classes added or modified against the base branch: `git diff --name-only --diff-filter=AM origin/main...HEAD -- 'src/main/java/*.java'`, mapped to class names and passed as `-DtargetClasses`. No such file means the job passes without running PIT. A locally computed list is used rather than `scmMutationCoverage`, which would need a `<scm>` block.
- **Gate:** `-DmutationThreshold=80`. This counts unkilled and uncovered mutants, so a class without a fast test fails the gate.
- **Excluded classes:** wiring only: `DamageDetectorApplication`, `DetectorConfiguration`, `DetectorProperties`.
- **Tests used:** JUnit unit and adapter tests, including the `@ParameterizedTest` example tables that replace jqwik (Q1, DDD-37). Exclude the Cucumber suite and ArchUnit (`excludedTestClasses`), which would be slow or kill nothing. Consequence: `OpenRouterJev`, `RetryingJev` and `ErrorFeedback` need direct tests, with WireMock where a test needs HTTP. Acceptance coverage alone counts as uncovered.
- **Where:** locally by the crafter during DELIVER, and in the `mutation` job on PRs (`fetch-depth: 0`, 15-minute timeout, HTML report as artifact).
- **Build change in the CI and PIT precursor (0.5 day, right after DELIVER step 1):** PIT is not in `pom.xml`. Add `pitest-maven` and `pitest-junit5-plugin` (latest on Maven Central today: 1.30.2 and 1.2.3). **Compatibility with Boot 4.1's JUnit 6 is unverified**, so check it there. jqwik needs no check: it is removed.

## Wave: DEVOPS / [REF] Observability stack

| Signal | Tool | Notes |
|---|---|---|
| Logs | `<output>/damage-detector.log` (`/out/damage-detector.log` in Docker). `root: warn`, console off, `--debug` on demand | The log survives `--rm` because it is on the mount. Refusals at bootstrap steps 1–4 have no log file (ADR-005) |
| Metrics | `report.md/json`, `evaluation.md/json`, the summary line | Read per run. Nothing is scraped |
| Traces | None | One process, one sequential loop |
| Alerts | Exit code plus one stderr line (INV-5) | For the person or script that started the run |

No monitoring stack, by user decision. No SLO error budgets either: the KPI targets act as per-run gates, and there is no continuous service to budget. Continuous learning: no. DORA metrics are not tracked for a single-developer showcase with no production deployment.

## Wave: DEVOPS / [REF] Branching strategy

**GitHub Flow.** `main` stays releasable. Work happens on short-lived `feature/*` branches and merges by PR once `verify`, `mutation` and (from slice 04) `image` are green. CI triggers follow this: PR to `main` and push to `main` only. No `paths-ignore`, because a skipped required check stays pending and blocks the merge. Branch protection on `main`: required status checks, no force push. The single developer needs no required reviewer. The current branch `feature/transistor-damage-detection` becomes the first PR once a remote exists.

## Wave: DEVOPS / [REF] Coexistence matrix

| Must keep working | Risk | Contract |
|---|---|---|
| `java -jar` and `docker run` on the same `./out` | Files owned by another UID cannot be overwritten | `--user` with the host UID makes both the same owner |
| One `.env` for jar and Docker | `export` or quotes break `--env-file` | Plain `KEY=value`; load it for the jar with `set -a; . ./.env; set +a` |
| Batch and evaluate output | Both write `damage-detector.log` | Separate folders (`/out`, `/out/eval`, as documented) |
| Two runs at once on one output | Shared probe file, log and reports | Not supported. One run per output folder |
| Previous image tags | A rebuild moves `:0.1.0` | SHA tags stay; prune by hand and keep the previous one |
| Other containers on the host | Port, volume or network clashes | The detector publishes no port and uses no named volume. `container-check` removes its own network and containers |
| `./mvnw verify` without Docker | A Docker-based test would break `local-jar` | Container checks stay outside `verify` (Upstream Changes, item 3) |
| Git hook managers | None installed | Not applicable |

## Wave: DEVOPS / [REF] Pre-requisites

**From DESIGN, which the platform must satisfy:**
- DELIVER step 1 (Boot 4.1, ADR-006) lands before any CI run counts. Latest 4.1 patch on Maven Central today: 4.1.2 (seen only, to be verified and pinned in step 1, OQ-6).
- `./mvnw verify` runs with no key and no `images/` folder. Checked for the skeleton: the tests pass `--detector.jev.api-key=test-key` and load fixtures from the classpath.
- DDD-10 and DDD-18 probes exist before slice 04, because the Docker failure modes rely on them. Slice 05 lands first, because it adds the INV-5 refusal feedback (D24). Order: 01 → 02 → 03 → 05 → 04.
- `spring.http.client.*` keys are gone after step 1 (DDD-27). Jev timeouts and address come only from `detector.jev.*`, so `--detector.jev.base-url` stays the one stub switch.
- Reports are written at the end of a run, through a temp file and a rename (DDD-34). The write probe also exercises that rename on `/out`.

**For DELIVER to build:**
- **CI and PIT precursor (0.5 day, not a slice, right after DELIVER step 1):** `.github/workflows/ci.yml` with `verify` and `mutation`, PIT in `pom.xml`, and the gitleaks version and SHA.
- **Slice 04, half A (0.5 day):** `Dockerfile`, `.dockerignore`, README commands including the `outside` guard, one run on real images, the manual Ctrl-C check.
- **Slice 04, half B (1 day):** `scripts/container-check.sh` bodies, the `image` CI job, the rollback drill. Re-check the WireMock digest.

**On the demo host:** Docker Engine or Docker Desktop, a `.env` with a credited key, and a local `images/` folder.

**At remote creation, before the first PR:** switch on Dependabot alerts (Q6).

## Wave: DEVOPS / [REF] OQ-5 resolution

**Resolved: use neither Spring Cloud Contract nor Pact JVM.** Both need the provider to take part: Pact needs provider verification and Spring Cloud Contract needs a provider build. OpenRouter will do neither for an alpha API, so a consumer-side contract would only restate our own WireMock stubs and could not detect provider drift. The scheduled paid check is also out, because CI has no key (user decision).

The proportional contract has three parts:

1. **Golden response:** commit one real, unmodified Jev response as a test fixture and serve it through WireMock, so a parser change is tested against a real answer. Refresh it at each production-data run.
2. **Real smoke by hand:** each slice's production-data AC is the provider check (DoD 4), about $0.00015 per image. DISTILL tags and documents it.
3. **Fail-safe drift at run time:** DDD-26 already turns any changed answer into `unreadable answer`, then REVIEW, then a `jevErrors` count, and exit 3 if nothing was judged. Drift cannot produce a false pass.

Reopen this only if OpenRouter publishes a versioned schema or a provider-side contract.

## Wave: DEVOPS / [REF] Wave decisions summary

- **Key decisions:**
  - Recreate deployment; rollback through an immutable SHA tag.
  - Multi-stage image on pinned Temurin 21.0.13 (noble), `USER 1000:1000`, plus `--user` in every documented command.
  - One GitHub Actions workflow with `verify`, `mutation`, `image`; no publishing, no secrets.
  - K4 is proved with a canary planted in a temporary copy of the tree, across all layers and inside the jar. The real `.env` is never written and is checked by hash.
  - Interrupt contract: 130, 143 and 137 are no result; a folder without a summary is incomplete; rerun into a fresh output.
  - Dependabot alerts on at remote creation.
  - Per-feature PIT on changed classes, ≥ 80 %.
  - GitHub Flow.
  - OQ-5 closed without a contract-test tool.
- **Infrastructure summary:** environments `local-jar`, `docker-run`, `ci`. No orchestration, registry, cloud resources or monitoring stack. The product's own outputs are the observability.
- **Constraints:**
  - The key exists only at run time (INV-3), and CI uses a canary.
  - Real-Jev KPIs (K1, K2, K5) are hand-run.
  - CI starts only once a remote exists. The user will add a **public** GitHub repo (decision 2026-11-06): tracked MVTec fixtures are redistributed under CC BY-NC-SA 4.0 with attribution in `src/test/resources/transistor-images/README.md`; the showcase stays non-commercial.
  - One run per output folder.
  - The host output must not lie inside the host input. The `outside` guard checks this before `docker run`.
- **Upstream changes.** No architecture change is forced, so there is no `devops/upstream-changes.md`. These wording and scope refinements apply:
  1. **Documented `docker run` command.** US-04, the DISCUSS and DESIGN driving ports and the slice-04 brief omit `--user` and `mkdir -p out`. The commands above replace them for the README. The old form still works where the host UID is 1000 and `./out` exists, and is refused safely otherwise. **Closed (Q4/F4):** the stories now point to these commands.
  2. **Earned Trust row "Input/output relation" (brief.md).** It does not hold across Docker mounts (see Limit in Docker). Documented, no code change.
  3. **DoD 1 versus US-04.** The US-04 scenarios need Docker, so they run in `scripts/container-check.sh`, not in `./mvnw verify`. Testcontainers was rejected because `verify` would then need Docker. **Closed (Q4):** `scripts/container-check.sh` is the US-04 gate, and DoD 1 says so.
  4. **Contract tests.** The "Contract tests recommended" lines in brief.md and in DESIGN Driven ports are superseded by OQ-5 above.
  5. **K5 evidence.** The DISCUSS handoff says K4 and K5 are measured "from the report files". Under DDD-14, `evaluation.json` and `evaluation.md` hold no cost and no duration. Today K5 is on stdout plus `time`, which is what the monitoring contract uses. **Resolved 2026-11-06:** the user chose to add `jevCost` and `elapsedSeconds` to `evaluation.json` (DDD-14 amended); slice 01 implements this.
  6. **Ownership.** The slice-04 brief and US-04 name DEVOPS as the implementer. This wave delivers the contract, and DELIVER builds it.
  7. **Mutation tooling.** PIT joins the version checks of the CI and PIT precursor. jqwik is removed (Q1), so it needs no check.
- **Final gate, revision cycle 1 (conditionally approved: 0 blockers, 3 high, 5 medium, 4 low):**

  | Finding | Fixed in |
  |---|---|
  | H1 canary could touch the real `.env` | CI/CD outline steps 1–3 and 7; `environments.yaml`; `container-check.sh` header |
  | H2 no interrupt contract | Interrupt contract; Exit codes; item 7 in `distill/upstream-issues.md` (C7b closed) |
  | H3 no dependency alerts | Security paragraph; Pre-requisites |
  | M1 evaluate output path | `environments.yaml` `run_evaluate` |
  | M2 `out/` in `.gitignore` | Done upstream; removed from the DELIVER list |
  | M3 bare key name, stray `ARG` | CI/CD outline steps 2–3 |
  | M4 pins | CI/CD outline (`verify` job, step 4); Pre-requisites |
  | M5 jqwik | Mutation testing strategy (removed, Q1) |
  | L2 dirty builds | Demo commands; Rollback contract |
  | L3 output inside input on the host | Demo commands (`outside` guard); Limit in Docker |
  | L1, L4 | No change, as instructed |

## Wave: DISTILL / [REF] Scenario list with tags

Date 2026-11-06, revised in final wave gate cycle 1. Density: lean (Tier-1 only, no menu). The `.feature` files under `src/test/resources/features/transistor-damage-detection/acceptance/` are the SSOT. Every scenario below also carries `@pending` and its story tag. The walking skeleton (`walking-skeleton.feature`, `@walking_skeleton @driving_port @contract-shape:bounded-change`) is inherited, its wording unchanged, and green.

| Story | Scenario | Examples | Tags |
|---|---|---|---|
| US-01 | Presenter sees zero false passes on the labelled set | 1 | `@driving_port` `@real-io` `@kpi-K1` `@contract-shape:bounded-change` |
| US-01 | Presenter sees how many good parts pass automatically | 1 | `@driving_port` `@real-io` `@kpi-K2` `@contract-shape:pure-function` |
| US-01 | Presenter sees the recall of each defect class | 1 | `@driving_port` `@real-io` `@contract-shape:pure-function` |
| US-01 | A wrong suspected class does not count as correct | 1 | `@driving_port` `@real-io` `@contract-shape:pure-function` |
| US-01 | An evaluation writes nothing but its declared files | 1 | `@driving_port` `@real-io` `@contract-shape:unbounded-preservation` |
| US-01 | A defect that passes is named as a false pass | 1 | `@driving_adapter` `@real-io` `@error` `@kpi-K1` `@contract-shape:bounded-change` |
| US-01 | A false pass is also reported as a problem | 1 | `@driving_port` `@real-io` `@error` `@kpi-K1` `@contract-shape:bounded-change` |
| US-01 | The false-pass line sits exactly at the pass threshold | 3 | `@driving_port` `@real-io` `@error` `@contract-shape:pure-function` |
| US-01 | The class table separates recall from the suspected class | 1 | `@driving_port` `@real-io` `@error` `@contract-shape:pure-function` |
| US-01 | Folder names are only used for scoring | 1 | `@driving_port` `@real-io` `@kpi-G1` `@contract-shape:unbounded-preservation` |
| US-01 | An image without a label is skipped, counted and not sent to Jev | 4 | `@driving_port` `@real-io` `@error` `@contract-shape:bounded-change` |
| US-01 | An image in a deeper sub-folder keeps its label | 1 | `@driving_port` `@real-io` `@contract-shape:pure-function` |
| US-01 | A folder without labels is refused before any cost | 2 | `@driving_port` `@real-io` `@error` `@contract-shape:bounded-change` |
| US-01 | A class without images is still shown, with zero rows | 1 | `@driving_port` `@real-io` `@contract-shape:bounded-change` |
| US-01 | Evaluation result can be read from the command line | 1 | `@driving_adapter` `@real-io` `@adapter-integration` `@contract-shape:bounded-change` |
| US-01 | The evaluation keeps its cost and duration as evidence | 1 | `@driving_port` `@real-io` `@kpi-K5` `@contract-shape:bounded-change` |
| US-01 | The evaluation names its run date | 1 | `@driving_port` `@real-io` `@contract-shape:bounded-change` |
| US-01 | The evaluation says what its defect images can and cannot prove | 1 | `@driving_port` `@real-io` `@contract-shape:bounded-change` |
| US-01 | The evaluation names the source of the sample data | 1 | `@driving_port` `@real-io` `@contract-shape:bounded-change` |
| US-01 | Running the evaluation again replaces the earlier record | 1 | `@driving_port` `@real-io` `@contract-shape:bounded-change` |
| US-02 | Presenter sees the trade-off between false passes and auto-passed good parts | 1 | `@driving_port` `@real-io` `@kpi-K2` `@contract-shape:pure-function` |
| US-02 | A defect above the configured threshold moves the safe threshold | 1 | `@driving_port` `@real-io` `@error` `@kpi-K1` `@contract-shape:pure-function` |
| US-02 | Presenter is told when no threshold is safe | 1 | `@driving_port` `@real-io` `@error` `@kpi-K1` `@contract-shape:pure-function` |
| US-02 | Every threshold is safe when no defect is judged GOOD | 1 | `@driving_port` `@real-io` `@contract-shape:pure-function` |
| US-02 | The sweep costs no extra Jev calls | 1 | `@driving_port` `@real-io` `@kpi-K1` `@contract-shape:bounded-change` |
| US-02 | The row of the configured threshold equals the headline | 4 | `@driving_port` `@real-io` `@contract-shape:pure-function` |
| US-02 | The sweep record is complete and the readable table stays short | 1 | `@driving_port` `@real-io` `@contract-shape:bounded-change` |
| US-02 | The chosen threshold applies to the next batch | 1 | `@driving_port` `@real-io` `@contract-shape:bounded-change` |
| US-02 | The tool never moves the threshold on its own | 1 | `@driving_port` `@real-io` `@error` `@contract-shape:unbounded-preservation` |
| US-03 | Batch completes and counts a failed image | 1 | `@driving_port` `@real-io` `@error` `@kpi-K3` `@contract-shape:bounded-change` |
| US-03 | A brief Jev outage does not change the verdict | 1 | `@driving_port` `@real-io` `@error` `@contract-shape:bounded-change` |
| US-03 | A failure that may pass goes away is retried, then the image goes to REVIEW | 6 | `@driving_port` `@real-io` `@error` `@kpi-K3` `@contract-shape:bounded-change` |
| US-03 | A failure that cannot go away is not retried | 4 | `@driving_port` `@real-io` `@error` `@contract-shape:bounded-change` |
| US-03 | An image Jev does not answer in time goes to REVIEW and the batch goes on | 1 | `@driving_port` `@real-io` `@error` `@adapter-integration` `@contract-shape:bounded-change` |
| US-03 | An image whose connection breaks goes to REVIEW | 1 | `@driving_port` `@real-io` `@error` `@adapter-integration` `@contract-shape:bounded-change` |
| US-03 | An answer that cannot be trusted never becomes a verdict | 10 | `@driving_port` `@real-io` `@error` `@contract-shape:bounded-change` |
| US-03 | A garbled answer followed by a good one gives the normal verdict | 1 | `@driving_port` `@real-io` `@error` `@contract-shape:bounded-change` |
| US-03 | The batch of ten reports every image | 1 | `@driving_port` `@real-io` `@error` `@kpi-K3` `@contract-shape:bounded-change` |
| US-03 | The report says how many images Jev judged | 1 | `@driving_port` `@real-io` `@error` `@kpi-K3` `@contract-shape:bounded-change` |
| US-03 | A failed image carries no Jev choice | 1 | `@driving_port` `@real-io` `@error` `@kpi-K3` `@contract-shape:bounded-change` |
| US-03 | Petra is warned when no image could be judged | 1 | `@driving_adapter` `@real-io` `@error` `@kpi-K3` `@contract-shape:bounded-change` |
| US-03 | Petra is warned when Jev cannot be reached at all | 1 | `@driving_port` `@real-io` `@error` `@kpi-K3` `@contract-shape:bounded-change` |
| US-03 | The evaluation warns when no image could be judged | 1 | `@driving_port` `@real-io` `@error` `@contract-shape:bounded-change` |
| US-03 | The evaluation counts a failed image as not passed at every threshold | 1 | `@driving_port` `@real-io` `@error` `@contract-shape:bounded-change` |
| US-03 | The waits between attempts grow | 1 | `@driving_port` `@real-io` `@contract-shape:bounded-change` |
| US-03 | The waits stop growing at the longest allowed wait | 1 | `@driving_port` `@real-io` `@contract-shape:bounded-change` |
| US-03 | The number of attempts is configurable | 3 | `@driving_port` `@real-io` `@error` `@contract-shape:bounded-change` |
| US-03 | Jev's own request to wait is honoured up to the longest allowed wait | 2 | `@driving_port` `@real-io` `@contract-shape:bounded-change` |
| US-03 | A request to wait longer than allowed ends the attempts at once | 1 | `@driving_port` `@real-io` `@error` `@contract-shape:bounded-change` |
| US-03 | After a failed image the cost is shown as a lower bound | 1 | `@driving_port` `@real-io` `@error` `@contract-shape:bounded-change` |
| US-03 | A success after a failed attempt also shows the cost as a lower bound | 1 | `@driving_port` `@real-io` `@error` `@contract-shape:bounded-change` |
| US-03 | Without failed attempts the cost is exact | 1 | `@driving_port` `@real-io` `@contract-shape:bounded-change` |
| US-03 | After failed attempts the evaluation shows its cost as a lower bound | 1 | `@driving_port` `@real-io` `@error` `@contract-shape:bounded-change` |
| US-03 | An interrupted run leaves no finished report | 2 | `@driving_adapter` `@real-io` `@error` `@contract-shape:bounded-change` |
| US-03 | Batch result can be read from the command line | 1 | `@driving_adapter` `@real-io` `@adapter-integration` `@contract-shape:bounded-change` |
| US-03 | The API key never shows up in what the detector leaves behind | 3 | `@driving_adapter` `@real-io` `@error` `@contract-shape:unbounded-preservation` |
| US-03 | Details on the console change nothing about the result | 1 | `@driving_adapter` `@real-io` `@contract-shape:bounded-change` |
| US-03 | PNG and JPEG images are inspected and other files are ignored | 1 | `@driving_port` `@real-io` `@contract-shape:bounded-change` |
| US-03 | A folder that already holds files keeps them | 1 | `@driving_port` `@real-io` `@contract-shape:bounded-change` |
| US-04 | Presenter gets the usual report from Docker | 1 | `@container` `@real-io` `@kpi-K4` `@contract-shape:bounded-change` |
| US-04 | Presenter gets the evaluation from Docker | 1 | `@container` `@real-io` `@kpi-K4` `@contract-shape:bounded-change` |
| US-04 | The shared Docker image carries no API key | 1 | `@container` `@real-io` `@error` `@kpi-K4` `@contract-shape:unbounded-preservation` |
| US-04 | Checking the image leaves Jonas's own key file alone | 1 | `@container` `@real-io` `@error` `@contract-shape:unbounded-preservation` |
| US-04 | The API key leaves no trace of a Docker run | 1 | `@container` `@real-io` `@error` `@contract-shape:unbounded-preservation` |
| US-04 | A missing key is reported before any work | 1 | `@container` `@real-io` `@error` `@kpi-K6` `@contract-shape:bounded-change` |
| US-04 | Docker passes on the nothing-judged signal | 1 | `@container` `@real-io` `@error` `@contract-shape:bounded-change` |
| US-04 | Docker passes on the false-pass signal | 1 | `@container` `@real-io` `@error` `@contract-shape:bounded-change` |
| US-04 | Presenter can delete what Docker wrote | 1 | `@container` `@real-io` `@contract-shape:bounded-change` |
| US-04 | A forgotten output folder is refused | 1 | `@container` `@real-io` `@error` `@kpi-K6` `@contract-shape:bounded-change` |
| US-05 | A mistake on the command line is refused before anything is written | 11 | `@driving_adapter` `@real-io` `@error` `@kpi-K6` `@contract-shape:unbounded-preservation` |
| US-05 | An input folder without images is refused | 2 | `@driving_adapter` `@real-io` `@error` `@kpi-K6` `@contract-shape:bounded-change` |
| US-05 | A missing API key is refused before any cost | 3 | `@driving_adapter` `@real-io` `@error` `@kpi-K6` `@contract-shape:bounded-change` |
| US-05 | An output folder that cannot be written is refused | 3 | `@driving_adapter` `@real-io` `@error` `@kpi-K6` `@contract-shape:unbounded-preservation` |
| US-05 | An unusable pass threshold is refused | 3 | `@driving_adapter` `@real-io` `@error` `@kpi-K6` `@contract-shape:bounded-change` |
| US-05 | An unusable Jev address is refused before any cost | 2 | `@driving_adapter` `@real-io` `@error` `@kpi-K6` `@contract-shape:bounded-change` |
| US-05 | An unexpected failure gives one line and leaves the details in the log | 1 | `@driving_adapter` `@real-io` `@error` `@kpi-K6` `@contract-shape:bounded-change` |

Totals: 76 scenarios, 126 examples. Error or edge paths: 45 scenarios (59.2 %), 91 examples (72.2 %).

| Story | File | Scenarios | Examples | Error scenarios | Error examples |
|---|---|---|---|---|---|
| US-01 | `milestone-1-evaluate-labelled-set.feature` | 20 | 26 | 6 | 12 |
| US-02 | `milestone-2-threshold-sweep.feature` | 9 | 12 | 3 | 3 |
| US-03 | `milestone-3-jev-failure-resilience.feature` | 30 | 53 | 22 | 44 |
| US-05 | `milestone-5-refusal-feedback.feature` | 7 | 25 | 7 | 25 |
| US-04 | `milestone-4-docker-run.feature` | 10 | 10 | 7 | 7 |

**Specifications outside Cucumber** (JUnit 5 with `@ParameterizedTest`; pending with `@Disabled`; PIT counts these, not Cucumber). 583 test cases:

| Class | Layer | Covers |
|---|---|---|
| `jev/OpenRouterJevAdapterTest` | adapter, `MockRestServiceServer` | Request shape, status classification, DDD-26 answer validity, the golden response |
| `jev/RetryingJevTest` | adapter, fake sleeper | Backoff, cap, `Retry-After` at, above and without a whole number, `Judged` attempts, probe |
| `jev/RetryingJevAttemptLoopTest` | layer 1 | Every reply script up to three long, for every limit up to four, against a model of the attempt loop |
| `cli/ErrorFeedbackTest` | layer 1 | Closed error set with pinned texts, `feedbackBeforeLog`, exit-2 line, precedence 3 over 2 |
| `cli/CliArgumentsTest`, `cli/PassThresholdTest` | layer 1 | Unknown options, dotted properties, threshold refusals |
| `domain/EvaluationTest` | layer 1 | INV-1, configured row equals headline, monotonic sweep, lowest safe threshold, over 8 named sets and 8 thresholds |
| `evaluate/LabelResolutionTest` | layer 1 | First-segment exact match (DDD-13) |
| `inspection/OutputFolderTest` | real temp folder | Root escape, atomic writes, probe with rename, real root, leftover probe file |
| `LogSinkTest`, `ApplicationDefaultsContractTest` | layer 1 | Log path; defaults. The key-and-log guard is enabled and runs in every verify |

## Wave: DISTILL / [REF] WS strategy

Inherited skeleton, Architecture of Reference plus `docs/architecture/atdd-infrastructure-policy.md` (`--policy=inherit`): real CLI and folders, Jev faked because it is a paid external.

## Wave: DISTILL / [REF] Adapter coverage table

| Adapter | `@real-io` scenario | Covered by |
|---|---|---|
| `OpenRouterJev` + `RetryingJev` | YES | "An image Jev does not answer in time…", "An image whose connection breaks…" (WireMock on a real port); `jev/*Test`. Real Jev: production-data AC per slice, by hand (OQ-5) |
| Real sleeper | YES | "The API key never shows up…", 503 rows (real jar, short waits) |
| `ImageFolder` | YES | "PNG and JPEG images are inspected and other files are ignored" (real files) |
| `OutputFolder` | YES | "A folder that already holds files keeps them", "An output folder that cannot be written is refused", "An interrupted run leaves no finished report"; `OutputFolderTest` |
| `ReportWriter` | YES | "Batch completes and counts a failed image" |
| `EvaluationWriter` | YES | "An evaluation writes nothing but its declared files" |
| `LogSink` | YES | "An unexpected failure gives one line and leaves the details in the log" |
| `Clock` | N/A | A capability, not an adapter with I/O. A fixed clock is the confirmed policy (Q10); "The evaluation names its run date" uses it |
| Container image | YES | `milestone-4-docker-run.feature` through `scripts/container-check.sh` |

Zero MISSING.

## Wave: DISTILL / [REF] Scaffolds

RED scaffolds, marker `// SCAFFOLD: true`, methods throw `AssertionError("Not yet implemented -- RED scaffold")`, none registered as a bean. Only classes that tests import are scaffolded, so `Inspector`, `DetectorCommand`, `EvaluationWriter` and the moved `ImageFolder` have no stub: they are reached through driving ports only.

- Root: `DetectorLauncher` (DDD-30), `LogSink`
- `cli/`: `CliArguments`, `ErrorFeedback`, `PassThreshold`
- `probe/`: `Probe`, `ProbeResult`
- `inspection/`: `OutputFolder`
- `jev/`: `RetryingJev`, `Sleeper`, `AttemptSource`, `AttemptResult`
- `domain/`: `JevOutcome`, `Label`, `LabelledOutcome`, `Evaluation`, `ThresholdSweep`
- `evaluate/`: `LabelledEvaluation`
- `scripts/container-check.sh`: checks and finding names fixed, each body RED. Runs without Docker for now

**One existing class changed: `jev/OpenRouterJev`, additively.** `RetryingJev` takes an `AttemptSource` and the fixture needs plain values (DDD-35, DDD-36), so it gained `implements AttemptSource`, a plain-value constructor, `attempt` and `probe`, each marked `// SCAFFOLD: true`. Its existing constructor and `judge` are unchanged, so the skeleton still runs on them. DELIVER removes those two and `implements Jev`.

## Wave: DISTILL / [REF] Test placement

- **Features:** `src/test/resources/features/transistor-damage-detection/acceptance/milestone-N-*.feature`, one per slice, beside the skeleton, so its runner finds them. Slice 05 runs before slice 04.
- **Glue:** `src/test/java/de/aboutlabs/damagedetector/acceptance/steps/`; composition and domain types in `.../acceptance/support/` (`DetectorWorld`, `DomainTypes`). `WalkingSkeletonSteps` keeps its scenario and its steps. Its JSON import and mapper migrate to `tools.jackson` in DELIVER step 1 (DDD-37).
- **State delta:** `src/test/java/de/aboutlabs/damagedetector/common/StateDelta.java` (`setTo`, `unchanged`, `everyEntry`, as far as used).
- **Direct tests:** in the package of the class under test, named `*Test`, because surefire does not include other names.
- **Runners:** in-process scenarios in `WalkingSkeletonAcceptanceTest` (surefire). `@driving_adapter` scenarios in `DrivingAdapterAcceptanceIT` (failsafe), because the repackaged jar exists only after `package`. Tag filters live in `pom.xml` as `acceptance.inprocess.tags` and `acceptance.realprocess.tags`.
- **Enabling a scenario:** remove its `@pending`. `@Disabled` on a direct test class comes off per slice.

## Wave: DISTILL / [REF] Driving Adapter coverage

| Entry point | Protocol | Scenario |
|---|---|---|
| Batch CLI | Real jar: arguments, stdout, stderr, exit code | "Batch result can be read from the command line" |
| Evaluate CLI | Real jar | "Evaluation result can be read from the command line", "A defect that passes is named as a false pass" (exit 2) |
| Exit 3 | Real jar | "Petra is warned when no image could be judged" |
| Exit 1, INV-5 rows | Real jar | All of milestone 5 |
| Exits 130, 143 | Real jar, real signals | "An interrupted run leaves no finished report" |
| `--detector.pass-threshold` | Real jar | "An unusable pass threshold is refused" |
| `--debug` | Real jar | "Details on the console change nothing about the result" |
| `OPENROUTER_API_KEY` | Real jar, environment | "A missing API key is refused before any cost" |
| Retry settings | In-process | "The number of attempts is configurable" |
| `docker run` | `scripts/container-check.sh` | milestone 4 |

**Decision Q11 (user, 2026-11-06).** The one `@walking_skeleton` scenario runs in-process and may not be rewritten, so no real-jar scenario runs at hand-off. For `local-jar` and `ci`, the first real-jar scenario is enabled right after the Boot 4.1 upgrade: "Batch result can be read from the command line". For `docker-run` the driving-adapter mandate is waived until slice 04.

## Wave: DISTILL / [REF] Pre-requisites

- **DESIGN:** driving ports, exit codes and texts as in brief.md, ADR-005 and DDD-14, DDD-15, DDD-33; UC-1 and its amendment, UC-2 to UC-4 as the oracle. UC-5 (paths as typed) is followed but **confirmed by the user 2026-11-06**; `Workspace.resolve` is the one place to change.
- **DEVOPS:** `local-jar` for `./mvnw verify`, with no key, no Docker and no `images/`; `docker-run` and `ci` for `scripts/container-check.sh`. The read-only cases need a non-root user; as root they are skipped by an assumption.
- **DELIVER step 1:** Boot 4.1 upgrade, then enable the first real-jar scenario (Q11).
- **Before slice 01 closes:** record the golden Jev response.
- **Before any `@container` scenario is enabled:** the container check negative self-test (see wave decisions).

## Wave: DISTILL / [REF] Wave decisions summary

- **Language and modes:** `[lang-mode] java`, `[policy-mode] inherit`, `[port-mode] bootstrapped`. `deliverable_type` is application.
- **Reconciliation:** passed, 0 contradictions, re-checked against the revised DISCUSS, DESIGN and DEVOPS.
- **Slice order:** 01, 02, 03, 05, 04. US-05 refusals moved out of milestone 3 into `milestone-5-refusal-feedback.feature` (D24).
- **Tier B:** not added. The feature is a one-shot CLI with no state that carries over between runs. The one state model, the attempt loop, is covered exhaustively by `RetryingJevAttemptLoopTest`.
- **Input mode:** JUnit `@ParameterizedTest` example tables at layer 1 (Q1, DDD-37). jqwik is removed. Layer 3 and up are example-only with named sad paths. Generated inputs are therefore gone: coverage now rests on the chosen tables and on exhaustive finite sets.
- **Mandate 8:** every run asserts an unchanged input tree inside `DetectorRun`, including refusals and interrupts. "The output folder holds only what … declares" asserts the output universe of DDD-29, and that no `*.tmp` is left, or at most one after an interrupt. Universe names are port-level: `input.tree`, `output.exists`, `output.tree`.
- **Mandate 12:** `acceptance/support/DomainTypes.java` holds the typed nouns; `ParameterTypes` converts wording to those types; `DetectorWorld` is the composition. All 107 step methods delegate in one statement with no control flow (checked by script). Step reuse is 439 occurrences over 107 step methods = **4.13x** (informational; Background steps count once).
- **Waiver, Jev call counts and cost (K3, K5):** Jev is paid, so automated tests count requests on the stub, never on OpenRouter, and a stub has no real cost or latency. The call-count scenarios prove the mechanism. The real numbers come only from the hand-run production-data ACs, and the reported cost stays a lower bound after failed attempts.
- **Waiver, fake sleeper:** in-process scenarios replace the real wait with a recording fake, as approved in the policy, so that backoff is asserted without real delay. What it cannot show is that a real wait happens. The real sleeper runs in the real-jar scenarios, with short waits.
- **Container check self-test (plan):** a script whose checks always pass looks the same as a clean image. Before any `@container` scenario is enabled, DELIVER adds `scripts/container-check.sh --self-test`, which builds a deliberately bad image and must report `FAIL`. A Java test on the repackaged jar is the fallback. See upstream-issues item 19.
- **Added test dependency:** `io.cucumber:cucumber-picocontainer` (state sharing across step classes). `maven-failsafe-plugin` is bound. No production dependency, no Boot upgrade.
- **Docker in `verify`:** none. `@container` scenarios drive the script only on request.
- **Sizing note (L12, informational):** four slices have 8 or more scenarios (outlines counted once): slice 01 has 20, slice 02 has 9, slice 03 has 30, slice 04 has 10. Slice 05 has 7. This does not block anything: many are table rows of one behaviour. DELIVER should expect slice 03 to take the most steps.
- **AT completeness:** 15/15, **COMPLETE**. C7b is closed by the interrupted real-jar scenario, against the DEVOPS interrupt contract. C4b and C7c are not applicable: the tool has no inverse operation, and concurrent runs are unsupported by contract. C5a: `--debug` is covered on batch only, because C5b shows it changes nothing but the console.
- **Open items:** `distill/upstream-issues.md`.

### Self-review checklist

| # | Item | Result |
|---|---|---|
| 1, 2 | WS strategy declared; WS scenario tagged | Pass |
| 3, 11 | Every driven adapter has a real-I/O scenario | Pass |
| 4 | In-memory doubles documented | Not applicable: none used. WireMock has no real Jev quirks, so the golden response and the real smoke cover that |
| 5 | Container preference | Pass: script, not Testcontainers |
| 6, 7, 8 | Scaffolds for imported classes, marker, `AssertionError` | Pass |
| 9 | RED, not BROKEN | Pass, see `distill/red-classification.md` |
| 10 | Every entry point exercised by protocol | Pass as scenarios, all pending. Not by a walking-skeleton scenario: decision Q11 |
| 12, 13, 15 | Python-specific checks (`capsys`, `des.*` imports, `noqa`) | Not applicable |
| 14 | Timing budgets of at least 200 ms | Pass: no timing assertion; the time-limit scenario uses 1 s |

## Wave: DELIVER / [REF] Implementation summary

- Date 2026-11-09. 14 roadmap steps (slices 01, 02, 03, 05, 04 plus refactor steps), all RED/GREEN/COMMIT PASS in `deliver/execution-log.json`; `des-verify-integrity` exit 0.
- Shipped: `--evaluate` mode with threshold sweep (US-01, US-02), retry decorator with REVIEW fallback (US-03), central CLI error feedback and bootstrap probes (US-05), Docker image and `scripts/container-check.sh` (US-04), CI workflow, README.
- Default pass threshold is 0.87 (see Upstream Issues).
- Refactor pass: commits 00fb855, a071806. Adversarial review revision pass: ae8d097..020c85e.

## Wave: DELIVER / [REF] Files modified

From `git diff --stat c4e65f7..HEAD`: 83 files, +4121 / -484.

- **Production (src/main, 33 files):** `DetectorLauncher`, `DetectorConfiguration`, `DetectorProperties`, `application.yml`; `cli/` (`DetectorCommand` new, `CliArguments`, `ErrorFeedback`, `PassThreshold`); `domain/` (`Evaluation`, `ThresholdSweep`, `BatchReport`, `Inspection`, `JevOutcome`, `PassPolicy`, `Label`, `LabelledOutcome`); `evaluate/` (`EvaluationWriter` new, `LabelledEvaluation`); `inspection/` (`ImageFolder` moved from `batch/`, `Inspector`, `OutputFolder`); `batch/` (`BatchInspection`, `ReportWriter`; `BatchCommand` removed); `jev/` (`OpenRouterJev`, `RetryingJev`, `Sleeper`).
- **Tests (src/test):** new `ArchitectureRulesTest`, `BatchReportTest`, `EvaluationFiguresTest`, `InspectionTest`, `JevJudgementTest`, `JevOutcomeTest`, `LabelledOutcomeTest`, `PassPolicyTest`, `ThresholdSweepTest`, `SweepRows`, `SleeperTest`, `DetectorLauncherTest`; changed acceptance steps and support, `CliArgumentsTest`, `ErrorFeedbackTest`, `OutputFolderTest`, `RetryingJev*Test`, `OpenRouterJevAdapterTest`; five milestone `.feature` files enabled; `jev/golden-response.json`.
- **Build, CI, Docker:** `pom.xml` (PIT, failsafe, ArchUnit, cucumber-picocontainer), `.github/workflows/ci.yml`, `Dockerfile`, `.dockerignore`, `scripts/container-check.sh`.
- **Docs:** `README.md`, `CLAUDE.md`, five slice briefs, `feature-delta.md`, `deliver/roadmap.json`, `deliver/execution-log.json`, `deliver/.develop-progress.json`.

## Wave: DELIVER / [REF] Scenarios green count

- 76 DISTILL scenarios plus the walking skeleton, all enabled, none pending.
- Latest `./mvnw clean verify` (2026-11-09): 653 unit tests + 89 integration/acceptance tests, 0 failures.
- 10 `@container` scenarios green via `scripts/container-check.sh` (outside `verify`, Q4).

## Wave: DELIVER / [REF] DoD check

| # | DoD item | Result | Evidence |
|---|---|---|---|
| 1 | UAT scenarios executable and green | Pass | 742 tests green; US-04 via `container-check.sh`, 10 scenarios green |
| 2 | Production-data AC run on real MVTec AD | Pass | Slice briefs; demo 2026-11-09 below |
| 3 | INV-1..INV-6 asserted | Pass | Acceptance scenarios, Mandate 8 assertions in `DetectorRun`, Ctrl-C 130 and kill -9 137 runs |
| 4 | Jev stubbed in CI; one real smoke per adapter | Pass | WireMock in `verify`; real smokes in slice briefs |
| 5 | Console, report.md, report.json consistent | Pass | Same-count assertions in acceptance; container parity (K4) |
| 6 | README shows the story commands | Pass | `README.md` (110 lines), DEVOPS demo commands incl. guard, `mkdir -p out`, `--user` |
| 7 | No secret committed, logged or packaged | Pass | 601d632 (refusal and `Jev.toString` no longer echo secrets); K4 image scan PASS; refusal hand run with unchanged usage |
| 8 | Reviewed and refactored; `domain/` framework-free | Pass | Refactor 00fb855, a071806; review revision ae8d097..020c85e; ArchUnit rules |
| 9 | Demoable in one session | Pass | Demo evidence below |

## Wave: DELIVER / [REF] Demo evidence

Date 2026-11-09, real Jev. OpenRouter usage 0.09781 -> 0.13814 across these demos.

- **US-01/02:** `java -jar target/damage-detector-0.1.0-SNAPSHOT.jar --evaluate --input=images --output=out/demo-20261109/eval` -> `Evaluated 253 labelled images at pass threshold 0.87: 0 false passes, good auto-pass 73/213 (34.3 %), defects not passed 40/40, Jev cost $0.03724 -> out/demo-20261109/eval/evaluation.md`; exit 0; "Lowest threshold with 0 false passes: 0.86".
- **US-03:** batch on 40 images -> `Inspected 40 images: 11 PASS, 8 SCRAP, 21 REVIEW (REVIEW share 52.5 %; 0 Jev errors in REVIEW), Jev cost $0.00589`; exit 0.
- **US-05:** `--input=out/demo-20261109/shift-2026-10-31` -> `Input folder does not exist: out/demo-20261109/shift-2026-10-31`; exit 1; no cost.
- **US-04:** `docker run --rm --user "$(id -u):$(id -g)" --env-file .env -v …:/in:ro -v …:/out damage-detector:0.1.0 --input=/in --output=/out` on the same 40 -> `Inspected 40 images: 12 PASS, 9 SCRAP, 19 REVIEW … Jev cost $0.00589 -> /out/report.md`; exit 0; output owned by the host user.
- Per-slice evidence (network-loss run, kill -9 -> 137, Ctrl-C in Docker -> 130, refusal hand run with unchanged usage, rollback drill): `slices/`.

## Wave: DELIVER / [REF] Quality gates

- Roadmap validated and reviewed (conditionally approved, fixes applied).
- 14/14 steps COMMIT/PASS; `des-verify-integrity` exit 0.
- Refactor pass: 00fb855, a071806.
- Adversarial review: rejected (4 blockers, test and log side). One revision pass (ae8d097..020c85e); B3 documented, not fixed.
- PIT 95 % (246/259) on the agreed scope: domain, `jev/`, `cli/` logic, `OutputFolder`. Cucumber-only classes are out of scope by user decision (2026-11-06). Gate >= 80 %.

## Wave: DELIVER / [REF] Pre-requisites

- JDK 21, Maven wrapper, Docker for `scripts/container-check.sh` and the US-04 demo.
- `.env` with `OPENROUTER_API_KEY` for real runs; never committed.
- No git remote yet: `.github/workflows/ci.yml` has not run on GitHub; Dependabot alerts to be enabled when the remote exists.

## Wave: DELIVER / [WHY] Upstream Issues and deviations

- (a) **User decision:** default pass threshold raised 0.86 -> 0.87 after the slice-02 sweep (commit a3c3463), outside a roadmap step.
- (b) The start-up failure "See log" fix (00fb855) and the review revision commits ae8d097..020c85e were done after the 14 roadmap steps, outside DES step logging.
- (c) **Review finding B3:** in steps 03-01, 04-02, 04-03, 05-01, 05-02, 06-01, 06-02 the crafters logged RED/GREEN/COMMIT at commit time, so the log does not evidence phase order (crafter reports state RED was observed). Not back-filled.
- (d) A `RetryingJevAttemptLoopTest` model defect was found in 04-01 and fixed by the acceptance designer.
- (e) Jev run-to-run variance: good auto-pass at 0.86 was 99 and 96 of 213; at 0.87 it was 73/213 in the final demo against 81 in the sweep run.
- (f) jqwik removed (the maintainer asks AI agents not to use it); JUnit parameterized tables replace it.
- (g) Rigor profile switched to custom (Sonnet crafters, Haiku reviewers, mutation on) from step 03-01.
