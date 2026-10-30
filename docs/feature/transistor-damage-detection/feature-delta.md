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

Date 2026-10-30. Density: lean (Tier-1 only). Evidence: one stakeholder plus SPIKE measurements; personas are illustrative (E6), with no customer interviews.

- **P1 QS inspector**: Petra Lindner (illustrative), `docs/product/personas/qs-inspector.yaml`. She runs the batch and works through REVIEW/SCRAP. Job J1.
- **P3 Showcase presenter**: Jonas Berger (illustrative), `docs/product/personas/showcase-presenter.yaml`. He proves the pass rule is safe and runs the demo anywhere. Jobs J2 and J3.
- P2 (defect-record consumer, DISCOVER) is still in the SSOT. No story in this wave is written for P2.

## Wave: DISCUSS / [REF] JTBD one-liner

> When I run the detector on a folder of transistor images, I want proof that no defect passes, every non-pass documented with a reason even if Jev glitches, and a one-command start anywhere, so I can show and trust the result. (J1 + J2 + J3 in `docs/product/jobs.yaml`; not scored, stakeholder priority J2 > J1 > J3.)

## Wave: DISCUSS / [REF] Locked decisions

DISCOVER used D1–D11, so this wave continues the numbering. "PO-proposed" marks a decision I derived from the user's decision; the stakeholder has not confirmed it explicitly.

- **D12 Qwen is removed from scope entirely.** The user said, 2026-10-30: "I want to get rid of Qwen." Descriptions stay Jev-based (`Suspected X (p=…); P(GOOD)=…`). There is no LM Studio dependency.
- **D13 Jev call failure.** The tool retries with increasing waits (default 3 attempts in total, configurable). If all attempts fail, the image becomes REVIEW, its description carries the error, and the batch continues. The report counts these errors (user decision 2026-10-30). An errored image can never be PASS (C10).
- **D14 All-failed guard (PO-proposed).** If no image in the run got a Jev judgement, the tool still writes the report. The console says `0 of N images judged by Jev` and points to the key and the network, and the exit code is non-zero.
- **D15 Evaluation is a user-facing flag on the same jar: `--evaluate`.** The label is the first sub-folder below `--input` (`good`, `bent_lead`, `cut_lead`, `damaged_case`, `misplaced`). Labels are used only for scoring and are never sent to Jev (C11). Images without a label are skipped and are not sent to Jev.
- **D16 Recall definition.** Recall per defect class = images of that class NOT passed (SCRAP or REVIEW) / class size. The "suspected class correct" share is reported separately. This follows the SPIKE table and clarifies the DISCOVER wording (see Changed Assumptions).
- **D17 Threshold sweep from one pass.** The sweep uses one Jev call per image and recomputes the verdicts for t = 0.50…0.99 in steps of 0.01. The tool never changes the threshold. The user sets `--detector.pass-threshold=<t>`.
- **D18 Evaluation exit code (PO-proposed).** The exit code is non-zero when at least one false pass occurs at the configured threshold, so a script can stop a demo build.
- **D19 Docker is a user-visible contract only.** It means `docker run` with mounted folders and the key from the environment at run time. The image contains no key and no `.env`. Dockerfile and image design are handed to DEVOPS.
- **D20 The pass rule is unchanged.** PASS iff Jev chooses GOOD and P(GOOD) ≥ t. SCRAP iff Jev chooses a defect. Otherwise REVIEW. t stays at 0.86 unless slice-02 evidence moves it.
- **D21 WS strategy B** (see below).
- **D22 Slice order** is evaluate, then sweep, then resilience, then Docker, ordered by learning leverage (slice 01 can invalidate 0.86).
- **D23 Every error or exception gives the user feedback.** User decision 2026-10-30: "Please include feedback in case of errors or exceptions". This also confirms D14 and D18. Whenever a run cannot do what was asked, the user sees a short, actionable message on stderr that names the cause and the fix, and the exit code is non-zero. By default no Java stack trace is shown; the details go to the log. Per-image Jev failures stay visible as REVIEW plus an error count (D13). See INV-5.

## Wave: DISCUSS / [REF] Scope Assessment

PASS. There are 4 stories and 3 modules (batch inspection, evaluation, container packaging). There is 1 external integration (Jev via OpenRouter), and it already exists in the skeleton. The estimate is about 3.5 days. Every story ships on its own. No split is needed.

## Wave: DISCUSS / [REF] Story map

Backbone (P1/P3 journey, `docs/product/journeys/qs-scrap-documentation.yaml` → `discuss_refinement`):

| Start a run | Judge each image | Record results | Check accuracy | Choose threshold |
|---|---|---|---|---|
| `java -jar` CLI *(WS, done)* | Jev on image bytes *(WS, done)* | report.md/json, scrap/, review/ *(WS, done)* | | |
| | retry, then REVIEW on error *(US-03)* | error count in report *(US-03)* | `--evaluate` metrics *(US-01)* | sweep + safe threshold *(US-02)* |
| `docker run` *(US-04)* | | | | |

- **Walking skeleton:** commit `89c9f4d`. It covers every activity except accuracy checking.
- **Slices** (each ≤1 day; briefs in `slices/`): 01 evaluate (US-01, J2, outcome K1) → 02 threshold sweep (US-02, J2, K1/K2) → 03 Jev failure resilience (US-03, J1, K3) → 04 Docker run (US-04, J3, K4).
- **Priority rationale:** slice 01 tests the riskiest assumption first. The 0.86 threshold has a 0.01 margin and was fitted in-sample, and a rerun could show a false pass. Slice 02 turns that evidence into a decision. Slice 03 must land before long unattended runs; the 253-image evaluation (~5 min) is exposed to the abort bug until then. Slice 04 changes no behaviour, only packaging, so it carries the least learning risk.

## Wave: DISCUSS / [REF] User stories with elevator pitches

### US-01: See whether any defect passes on the labelled images

`job_id: J2` · slice: `slice-01-evaluate-labelled-set` · persona: P3

**Problem.** Jonas Berger presents the detector as safe: "no defect passes". The only evidence is the SPIKE run on 26 good and 40 defect images, and 0.86 was fitted on those same images with a 0.01 margin (highest defect P(GOOD) = 0.85). The probe script is deleted, so he has no way to re-check the claim or show it live.

#### Elevator Pitch
Before: Jonas cannot measure false passes or the good auto-pass rate with the product; the numbers exist only in the SPIKE notes.
After: run `java -jar target/damage-detector-0.1.0-SNAPSHOT.jar --evaluate --input=images --output=out/eval` → sees `Evaluated 253 labelled images at pass threshold 0.86: 0 false passes, good auto-pass 110/213 (51.6 %), defects not passed 40/40, Jev cost $0.03795 -> out/eval/evaluation.md` (counts illustrative), plus `evaluation.md` with a per-class table and a false-pass list.
Decision enabled: Jonas decides whether he can claim "zero false passes on all 253 MVTec AD images" in the showcase, or must move the threshold first (US-02).

**Domain examples**
1. *Happy path:* the full set `images/` (213 good, 10 each of bent_lead, cut_lead, damaged_case, misplaced). All 40 defects are SCRAP or REVIEW. `evaluation.md` shows `0 false passes`, recall 10/10 for each class, and the good auto-pass rate over all 213.
2. *Boundary:* on this run Jev gives `damaged_case/002.png` P(GOOD) = 0.87 with choice GOOD (hypothetical; in the SPIKE it was a false pass at t = 0.80). It appears under "False passes" as `damaged_case/002.png  P(GOOD)=0.87`, and the exit code is non-zero.
3. *Error:* Jonas points `--input` at `out/` by mistake. It has no `good/` or defect sub-folders, so he sees `No labelled images below out: expected sub-folders good, bent_lead, cut_lead, damaged_case, misplaced`. No Jev call is made and the exit code is non-zero.
4. *Edge:* `images/extra/foo.png` (unknown folder) is listed as `skipped (no label): 1` and is not sent to Jev.

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
  Then no request to Jev contains a file name or a folder name such as "damaged_case" or "good"

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
- [ ] The exit code is non-zero if false passes > 0 at the configured threshold (D18), and zero otherwise.
- [ ] No Jev request contains a file or folder name (C11). Unlabelled images are skipped, counted, and not sent.
- [ ] A missing or empty labelled structure gives a message naming the five expected sub-folders, a non-zero exit, and 0 Jev calls.
- [ ] `evaluation.md` ends with the attribution `Sample data: MVTec AD "transistor", CC BY-NC-SA 4.0, non-commercial use` (C7).
- [ ] Production-data AC: a real run over all 253 images in `images/` completes, and its summary line and `evaluation.md` are kept as evidence in slice 01.

**Outcome KPIs:** K1 and K5. **Technical notes:** about $0.00015/image (~$0.04 per 253) and about 1.2 s/image (~5 min sequential). Jev results can differ from run to run, so record the run date. All 40 defects were already in the SPIKE sample: the defect side checks reproducibility, it is not held-out validation. Only the 187 good images not used in the SPIKE are new evidence. The tool does not know which 26 good images the SPIKE used, so a held-out split is OUT. Until slice 03 lands, a Jev failure aborts the evaluation. **Dependencies:** skeleton 89c9f4d, an OpenRouter key with credit, and a local `images/` folder (gitignored).

### US-02: Pick a pass threshold that lets no defect through

`job_id: J2` · slice: `slice-02-threshold-sweep` · persona: P3

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

`job_id: J1` · slice: `slice-03-jev-failure-resilience` · persona: P1

**Problem.** Petra Lindner starts a batch over a shift's images and walks away. Today a single failed Jev call (timeout, HTTP 503, 429 rate limit) aborts the whole run: she gets no report.md, no scrap records, and has to start again and pay again.

#### Elevator Pitch
Before: one failed Jev call aborts the batch, and Petra gets no report at all.
After: run `java -jar target/damage-detector-0.1.0-SNAPSHOT.jar --input=shift-2026-10-30 --output=out/shift-2026-10-30` → sees `Inspected 40 images: 18 PASS, 9 SCRAP, 13 REVIEW (REVIEW share 32.5 %; 2 Jev errors in REVIEW), Jev cost $0.00570 -> out/shift-2026-10-30/report.md` (counts illustrative), and `review/good/042.png.txt` reads `REVIEW: Jev call failed after 3 attempts: HTTP 503 Service Unavailable`.
Decision enabled: Petra releases the PASS parts, documents the SCRAP parts, and looks at the 2 errored images herself (or reruns only those) instead of rerunning the whole batch.

**Domain examples**
1. *Transient blip:* Jev returns 503 for `bent_lead/003.png`, then answers on the 2nd attempt. The image gets its normal verdict (SCRAP) and is not counted as an error.
2. *Persistent failure:* `good/042.png` times out on all 3 attempts. It becomes REVIEW with `Jev call failed after 3 attempts: timeout after 120 s`. The batch goes on with `good/043.png`.
3. *Everything fails:* Petra's key was revoked (HTTP 401) and all 4 images fail. report.md lists 4 REVIEW with the error, the console says `0 of 4 images judged by Jev - check OPENROUTER_API_KEY and network`, and the exit code is non-zero.

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
  And the run ends with a non-zero exit code
```

**Acceptance criteria**
- [ ] A failed Jev call (timeout, connection error, HTTP error, unreadable answer) is retried with increasing waits, default 3 attempts in total, configurable.
- [ ] If all attempts fail, the image is REVIEW, its description is `Jev call failed after N attempts: <reason>`, and the remaining images are still processed.
- [ ] The console summary, report.md and report.json show the number of Jev errors. report.json marks each errored image.
- [ ] An errored image is never PASS. A success on retry gives the normal verdict and is not counted as an error.
- [ ] If 0 images were judged, the report is still written, the console names OPENROUTER_API_KEY and the network, and the exit code is non-zero (D14).
- [ ] No error text, log line or report contains the API key.
- [ ] INV-5 (D23): missing arguments, a missing input folder, an input folder without images, and any unexpected exception each print one actionable message with no stack trace and give a non-zero exit code.
- [ ] Production-data AC: a real run on 10 images from `images/` with the network cut for part of the run (or a wrong base URL for 1 attempt) completes with a report.

**Outcome KPIs:** K3. **Technical notes:** the retry/backoff mechanism and which errors are retried are DESIGN choices; the user-visible result must match the ACs. In evaluate mode, errored images count as not passed (US-01/US-02). **Dependencies:** skeleton; none on US-01/02.

### US-04: Run the inspection and the evaluation from Docker

`job_id: J3` · slice: `slice-04-docker-run` · persona: P3

**Problem.** Jonas wants to show the detector on a colleague's machine. Today that needs JDK 21, Maven, a built jar and a sourced `.env`. Copying a Docker image that contains his OpenRouter key would leak it.

#### Elevator Pitch
Before: the demo only runs where Java and Maven are installed and `.env` is sourced by hand.
After: run `docker run --rm --env-file .env -v "$PWD/images:/in:ro" -v "$PWD/out:/out" damage-detector:0.1.0 --input=/in --output=/out` → sees the same summary line as the jar (`Inspected 253 images: … -> /out/report.md`), and `./out` contains report.md, report.json, scrap/ and review/ that he can open and delete without sudo.
Decision enabled: Jonas decides he can demo on any Docker host and share the image without sharing his key.

**Domain examples**
1. *Happy path:* on a fresh Ubuntu laptop with Docker only, the command above on 5 images from `images/` writes `./out/report.md` with the same verdict counts as `java -jar` on the same stubbed Jev answers.
2. *Evaluate:* `docker run --rm --env-file .env -v "$PWD/images:/in:ro" -v "$PWD/out:/out" damage-detector:0.1.0 --evaluate --input=/in --output=/out/eval` writes `evaluation.md`.
3. *Error:* without `--env-file`, Jonas sees `No Jev API key: set the OPENROUTER_API_KEY environment variable`, the exit code is non-zero, and no Jev call is made.

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
  Then he is told to set OPENROUTER_API_KEY and the run ends with a non-zero exit code
```

**Acceptance criteria**
- [ ] The documented `docker run` command (batch and `--evaluate`) accepts the same flags as the jar and writes the same set of files into the mounted output folder.
- [ ] Verdict counts and report structure equal the jar run for the same Jev answers (checked with stubbed Jev).
- [ ] The key comes only from the run-time environment. `docker image inspect`, `docker history` and the image filesystem contain no key and no `.env`.
- [ ] Files written to the mounted output can be deleted by the host user without sudo when the documented command is used.
- [ ] A missing key gives the existing message and a non-zero exit before any image is processed.
- [ ] Production-data AC: the documented command runs on ≥5 real images from `images/` against real Jev and writes report.md.

**Outcome KPIs:** K4. **Technical notes:** DEVOPS owns the Dockerfile, base image, user mapping, tag and build command. `images/` is 312 MB and gitignored, so it is mounted, never copied into the image. **Dependencies:** US-03 recommended first (long demo runs); Docker Engine on the demo host. Handoff: **DEVOPS (nw-platform-architect)**.

## Wave: DISCUSS / [REF] Acceptance criteria

The ACs are embedded per story above (US-01: 8, US-02: 5, US-03: 7, US-04: 6). Cross-cutting invariants that hold for every story:
- **INV-1 (C10):** PASS only when Jev chose GOOD with P(GOOD) ≥ the configured threshold. An image whose Jev call failed is never PASS.
- **INV-2 (C11):** no request to Jev contains a file name, folder name or label.
- **INV-3:** the API key never appears in the image, logs, console or reports.
- **INV-4:** PASS + SCRAP + REVIEW = total, and Jev errors ≤ REVIEW.
- **INV-5 (D23): errors and exceptions always give feedback.** Each case below prints one actionable message, gives a non-zero exit code, and shows no stack trace by default. Message texts are examples; DESIGN may reword them but must keep the cause and the fix.

  | Situation | User sees (example) |
  |---|---|
  | `--input` or `--output` missing | `Usage: java -jar damage-detector.jar [--evaluate] --input=<folder> --output=<folder> (missing --output)` |
  | Input folder does not exist | `Input folder does not exist: /home/jonas/imgs` |
  | Input folder has no images | `No PNG/JPEG images below /home/jonas/empty` |
  | Output folder not writable | `Cannot write to output folder /out: permission denied` |
  | No API key | `No Jev API key: set the OPENROUTER_API_KEY environment variable` |
  | No image judged by Jev (D14) | `0 of 4 images judged by Jev - check OPENROUTER_API_KEY and network` (report still written) |
  | Evaluate: no labelled sub-folders | `No labelled images below out: expected sub-folders good, bent_lead, cut_lead, damaged_case, misplaced` |
  | Evaluate: false pass at the configured t (D18) | summary line with `1 false passes` + list in evaluation.md |
  | Single image fails at Jev (D13) | no abort; REVIEW + `Jev call failed after 3 attempts: …` + error count |
  | Any other unexpected exception | `Inspection failed: <short cause>. See log for details.` |

  Each story's acceptance tests cover the rows it touches: US-01 the evaluate rows, US-03 the Jev rows, US-04 the key and output rows in the container. The rows for missing arguments, a missing input folder and an unexpected exception are tested in US-03, the first story that changes error handling.

## Wave: DISCUSS / [REF] Outcome KPIs

Objective: the showcase claims only what the tool measures. That means zero false passes on all labelled images, an honest review share, and no lost batch.

| # | Who | Does what | Target | Baseline | Measured by |
|---|---|---|---|---|---|
| K1 (north star) | P3 presenter | sees false passes on the full labelled set (40 defects) at the chosen threshold | **0** | 0/40 at 0.86, in-sample (SPIKE) | `--evaluate` summary line / evaluation.json |
| K2 | P3 presenter | reports the good auto-pass rate on all 213 good images | reported in every evaluation; informative goal ≥ 50 %, not a gate | 14/26 = 54 % (SPIKE) | evaluation.md sweep row at the configured t |
| K3 | P1 inspector | gets a complete report from a batch with injected Jev failures | 100 % of runs complete; 0 errored images PASS | 0 % (one failure aborts) | acceptance test with 2 of 10 calls failing + slice-03 real run |
| K4 | P3 presenter | gets the jar-equivalent report from `docker run` | verdict counts identical in 100 % of compared runs; 0 key occurrences in the image | no container | stubbed-Jev parity test; `docker history`/`inspect` scan |
| K5 (guardrail) | P3 presenter | pays and waits for a full evaluation | ≤ $0.05 and ≤ 10 min per 253-image run | $0.00015/image, 1.2 s/image (SPIKE) | evaluation summary cost; wall clock |
| G1 (guardrail) | n/a | Jev requests containing a file or folder name | 0 | 0 (skeleton test) | WireMock request assertion |

DEVOPS handoff: K4 and K5 need no runtime monitoring. They are measured per run from the report files.

## Wave: DISCUSS / [REF] Definition of Done

1. Every UAT scenario of the story is an executable acceptance test and is green (`./mvnw verify`).
2. The production-data AC was run on real MVTec AD images, and its output is cited in the slice brief.
3. INV-1 to INV-4 are asserted by tests.
4. Jev stays stubbed in automated tests (no paid calls in CI). One real-Jev contract smoke per driven adapter is documented.
5. The console summary line, report.md and report.json stay consistent (same counts).
6. The README shows the exact command from the story's elevator pitch.
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
- Data residency and commercial use (blocked by CC BY-NC-SA, C7).

## Wave: DISCUSS / [REF] WS strategy

**B: real local adapters plus a fake for the costly external.** The skeleton already exists (`89c9f4d`). Acceptance tests start the real Spring Boot CLI on the real filesystem with Jev stubbed by WireMock, because Jev is a paid LLM API. Every new story extends this harness. Each slice's production-data AC uses real Jev on real images, run by hand and cited. US-04 adds a container-level run with the same stub. This is not D: there is no env-switching between real and fake in the automated suite.

## Wave: DISCUSS / [REF] Driving ports

| Port | Form | Story |
|---|---|---|
| Batch CLI | `java -jar target/damage-detector-0.1.0-SNAPSHOT.jar --input=<folder> --output=<folder>` | skeleton, US-03 |
| Evaluate CLI | `… --evaluate --input=<labelled folder> --output=<folder>` | US-01, US-02 |
| Threshold | `--detector.pass-threshold=<0..1>` (Spring property, default 0.86) | US-02 |
| Retry attempts | configuration property, default 3 (name chosen in DESIGN) | US-03 |
| Container | `docker run --rm --env-file .env -v <in>:/in:ro -v <out>:/out damage-detector:0.1.0 [same flags]` | US-04 |
| Secret | env `OPENROUTER_API_KEY` | all |
| Outputs | stdout summary line, exit code, `report.md/json`, `scrap/`, `review/`, `evaluation.md/json` | all |

## Wave: DISCUSS / [REF] Pre-requisites

- The walking skeleton `89c9f4d` is green (`./mvnw verify`).
- An OpenRouter key with credit in a gitignored `.env`. A full evaluation costs about $0.04.
- A local `images/` folder (MVTec AD transistor, 253 PNG, gitignored, 312 MB).
- Docker Engine on the demo host (US-04). DEVOPS delivers the image build.
- Jev image input via OpenRouter is an alpha/early feature (E8a), and its format may change. This is a risk to all slices.

## Wave: DISCUSS / [REF] DoR validation

| # | DoR item | Status | Evidence |
|---|---|---|---|
| 1 | Problem statement in domain language | PASS | Each story opens with a persona pain: the 0.01 margin, the threshold trade-off, a lost batch, no Java on the demo host |
| 2 | Persona with specific characteristics | PASS | P1 Petra Lindner and P3 Jonas Berger (illustrative, YAML in `docs/product/personas/`) |
| 3 | ≥3 domain examples with real data | PASS | Real MVTec paths (`damaged_case/002.png`, `good/001.png` P(GOOD)=0.85), real error texts. Hypothetical values are marked |
| 4 | UAT Given/When/Then, 3–7 scenarios | PASS | US-01: 4, US-02: 3, US-03: 4, US-04: 3 |
| 5 | AC derived from UAT | PASS | Every scenario maps to ≥1 AC. Each story adds a production-data AC |
| 6 | Right-sized | PASS | ≤1 day per story (estimates in the slice briefs); 3–4 scenarios each |
| 7 | Technical notes | PASS | Cost/latency, Jev variance, the in-sample defect set, DEVOPS ownership, the abort risk before slice 03 |
| 8 | Dependencies tracked | PASS | US-02→US-01; US-04 after US-03 (recommended); key, `images/`, Docker Engine |
| 9 | Outcome KPIs with targets | PASS | K1–K5 and G1 with numeric targets, baselines and measurement |

JTBD traceability: US-01 and US-02 → J2, US-03 → J1, US-04 → J3. No story is `@infrastructure`, and every slice has a user-visible story. **DoR status: PASSED.** No per-wave peer review was run (optional per nw-discuss; the consolidated review runs at the end of DISTILL).

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

- **Decisions:** D12–D23 above. The user confirmed D14 and D18 on 2026-10-30 and extended them with D23/INV-5 (feedback on every error or exception). The `alternatives-considered` expansion was offered and not taken, so the output stays lean. The user approved going on to DESIGN.
- **Requirements:** 4 stories in 4 slices. Feature type: user-facing CLI (batch + evaluation) plus a container runtime. The walking skeleton exists.
- **Constraints carried forward:** C1, C2, C3, C7, C10, C11, C12; Jev via OpenRouter alpha; key from the environment only.
- **Upstream changes:** see Changed Assumptions (Qwen removed, batch failure behaviour, recall definition, threshold check as a feature).
- **Handoff:** DESIGN (nw-solution-architect) gets all sections. DEVOPS (nw-platform-architect) gets US-04 and KPIs K4/K5.
