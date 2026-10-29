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
