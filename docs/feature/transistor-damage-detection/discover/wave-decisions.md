# DISCOVER Wave Decisions: transistor-damage-detection

Date: 2026-10-29 | Scope: showcase/demo (E6) | Interviews: 0 (waived for showcase) | Peer review: approved with conditions (attempt 2), N1-N3 applied

## Sources

- **E1** User brief, `docs/brief.md`: what the tool should do, the 4 defect types, the QS audience, and assumptions (Jev, Spring Boot, Docker, extensibility).
- **E2** User statement ("There is a image folder with images taken automatically from a camera (top view)", single stakeholder statement) plus orchestrator inspection of `images/`. Size and volume were verified from file headers and du. good 213, bent_lead 10, cut_lead 10, damaged_case 10, misplaced 10. 253 PNG at 1024x1024 (312 MB). Visually checked: good/000, bent_lead/000, misplaced/000 (black TO-92 transistor on a copper stripboard).
- **E3** The dataset matches MVTec AD "transistor" (CC BY-NC-SA 4.0). Inferred, then confirmed by the user (E7).
- **E4** Jev / TypeSafe System One docs: `POST https://api.typesafe.ai/v1/systemone`, Bearer key, JSON `state` plus typed questions (`choice`, `score`, `noul`), calibrated confidence. SDKs exist for Python and JS/TS only. The docs mention no image input.
- **E5** Questions asked but not answered at first: current QS practice, real line vs showcase, who uses the records, data residency. (Answered in E6/E7.) Still unknown: Jev price per call.
- **E6** User answers, 2026-10-29: "It's a showcase project." Record consumers: "both" (reporting/audit and line feedback). Cloud/data residency: "Maybe in a further iteration. Now it is just a demo / showcase project."
- **E7** User answers, 2026-10-29: "Let only pass through good images. The images are already sorted in the different outcomes, but that is due to the structure of the demo data. Ignore that for the functionality." Batch mode: "yes as a batch". Model: "Use the cheapest Claude model" (resolved to `claude-haiku-5-5`, $0.10 / $0.50 per 1M tokens, from the Claude API skill model table cached 2026-10-06). Dataset: MVTec AD "Confirmed".
- **E8** SPIKE prep, 2026-10-29: (a) Jev is on OpenRouter as `typesafe/jev-1.13` via `POST https://openrouter.ai/api/alpha/decisions` and **accepts image input** in `state` (`image_url` data URL, PNG/JPEG, 256–1568 px per side) — an early feature, run in long-context mode, described as slower and less accurate (openrouter.ai/docs/guides/community/jev, multimodal-decisions). (b) Anthropic does not allow Claude Free/Pro/Max subscription credentials in third-party apps; apps need an API key. Per the user's fallback preference, vision runs on local `qwen/qwen3.6-35b-a3b` in LM Studio. (c) User wants Jev via OpenRouter.

## Decisions

- **[D1] G1/G2 are waived for showcase scope, not passed.** Rationale: the stakeholder confirmed a demo/showcase project (E6); no Mom Test interviews took place, so no customer pain is claimed. Reopen if productized. Source: E1, E6.
- **[D2] Provisionally reject the assumption "Jev classifies images".** Rationale: no image input is documented as of 2026-10-29; the docs describe only text or structured state. Source: E4. Dropped pending vendor confirmation or a live call.
- **[D3] Two-stage pipeline (vision LLM -> structured description -> Jev `choice`) is a HYPOTHESIS for the SPIKE, not an architecture decision.** Rationale: it is the only option seen so far that keeps Jev in the brief. Source: E1, E4.
- **[D4] Call Jev through its REST API from Spring; no SDK.** Source: E4.
- **[D5] Route low-confidence results to human review as part of the hypothesis.** Rationale: Jev supports confidence thresholds, and some defects are subtle. Source: E2, E4.
- **[D6] The main safety metric is the false-pass rate (a defect classified GOOD).** Rationale: an escaped defect is also undocumented scrap. Source: E1. QS still has to set the tolerance (E5).
- **[D7] Use `images/` for the non-commercial showcase, with MVTec AD attribution.** Source: E3, E6.
- **[D10] Zero false passes; folder labels are ground truth only, never classifier input; batch processing.** Source: E7.
- **[D11] SUPERSEDED by E8b: vision step uses local Qwen (`qwen/qwen3.6-35b-a3b`, LM Studio), not `claude-haiku-5-5`.** Original: Rationale: stakeholder asked for the cheapest model; risk A1 rises because subtle lead defects may need a stronger model. Escalation is a stakeholder decision. Source: E7.
- **[D9] Cloud APIs (vision LLM, Jev) are allowed for the demo; data residency deferred to a later iteration.** Source: E6.
- **[D8] Drop supervised per-class training as the main approach. Defer good-only anomaly detection as a fallback.** Rationale: 10 samples per class against 213 good. Source: E3.

## Constraints

| ID | Constraint | Source |
|----|-----------|--------|
| C1 | Spring Boot, best practices (stakeholder-stated) | E1 |
| C2 | Docker runtime (stakeholder-stated) | E1 |
| C3 | Extensible (DB, microservice) (stakeholder-stated) | E1 |
| C4 | Input: top-view 1024x1024 PNG. The user states it comes from an automatic camera (single stakeholder statement); a live line camera is unverified | E2, E3 |
| C5 | Jev: text/structured input only, REST + Bearer key (kept outside the code), no Java SDK documented | E4 |
| C6 | Good parts pass; a defect produces a record with image + text | E1 |
| C7 | Sample data is MVTec AD, CC BY-NC-SA 4.0 (confirmed); non-commercial showcase, attribute MVTec AD | E3, E7 |
| C10 | Only good parts pass; zero false passes | E7 |
| C11 | Folder structure is demo-data only; never a classifier input; ground truth for evaluation | E7 |
| C12 | Batch mode over the input folder | E7 |
| C13 | Vision model: local Qwen `qwen/qwen3.6-35b-a3b` via LM Studio (OpenAI-compatible); Jev via OpenRouter Decisions API (superseded Haiku choice) | E7, E8 |
| C9 | Showcase scope: cloud APIs allowed; data residency deferred | E6 |
| C8 | Small evaluation set: 10 per class. 10/10 gives a two-sided 95% Clopper-Pearson CI lower limit of about 69% (one-sided 95% bound about 74%) | E2, E3 |

## Validated assumptions

| ID | Assumption | Confidence | Source |
|----|-----------|-----------|--------|
| V1 | Labeled, consistent top-view images exist. The user states they come from an automatic top-view camera (E2, single stakeholder statement). The dataset matches public MVTec AD (E3), so whether a live line camera feeds QS is unverified | Medium | E2, E3 |
| V2 | The 4 brief defect types map 1:1 to the dataset classes (damaged -> damaged_case is inferred) | Medium | E1, E2 |
| V3 | Defects can be seen in a top view | Medium (n=3 inspected) | E2 |
| V4 | Jev's typed `choice` + confidence fits 5-class routing | Medium-High (docs only) | E4 |

## Invalidated assumptions

| ID | Assumption | Evidence ref |
|----|-----------|-------------|
| X1 | Jev classifies images directly | E4 → E8a: REVERSED. Not in TypeSafe's direct-API docs, but supported via OpenRouter (early feature). Tested in SPIKE variant A |
| X3 | A Jev Java SDK is available | E4. Provisionally invalidated (docs list SDKs for Python and JS/TS only; no Java SDK documented as of 2026-10-29) |

Unverified / conditional (not invalidated):

| ID | Assumption | Evidence ref |
|----|-----------|-------------|
| X2 | Sample images are free for product use | E3, E7. Resolved for the showcase: MVTec AD confirmed, non-commercial use with attribution; blocks commercial use |

## Assumption risk register (Score = Impact x 3 + Uncertainty x 2 + Ease x 1)

Ranked by score (ties share a rank). All rows score above 12, so all are "test first". The SPIKE covers A1, A2, A5, A6 and A7; A2's target is set (E7); A6's Jev price is still to be found. A3 and A4 are closed for the showcase by E6.

| Rank | ID | Assumption | I | U | E | Score | Priority |
|------|----|-----------|---|---|---|-------|----------|
| 1 | A2 | Zero false passes can be reached (target set, E7) | 3 | 3 | 2 | 17 | Test first (SPIKE) |
| 2 | A1 | The vision LLM (`claude-haiku-5-5`, the cheapest) describes body/leads accurately enough, including subtle defects | 3 | 3 | 1 | 16 | Test first (SPIKE) |
| - | A3 | QS has a real, recurring scrap-documentation pain | 3 | 3 | 1 | 16 | Out of scope for the showcase (E6); reopen if productized |
| - | A4 | Images may be sent to cloud APIs | 3 | 3 | 1 | 16 | Resolved for the demo (E6); residency deferred |
| 5 | A7 | Top-view only is enough for all 4 classes | 3 | 2 | 1 | 14 | Test first (SPIKE) |
| 6 | A5 | Jev confidence is still meaningful when its input is LLM-generated text | 2 | 3 | 1 | 13 | Test first (SPIKE) |
| 6 | A6 | Latency and cost fit the line's throughput | 2 | 3 | 1 | 13 | Test first (SPIKE + ask) |

## Gate status

G1 WAIVED (showcase, E6) | G2 WAIVED (showcase, E6) | G3 NOT STARTED | G4 CONDITIONAL PASS (showcase-scoped; Jev cost and cost per image measured in the SPIKE).
Go/no-go: go for DISCUSS with showcase scope; the feasibility SPIKE is the first technical step.
