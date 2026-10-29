# SPIKE findings: transistor-damage-detection (Phase 1, throwaway probe)

Date: 2026-10-29. **Promoted on 2026-10-29** (variant A) into the walking skeleton, commit `89c9f4d`; the probe directory was deleted after promotion. Former probe code: `/tmp/claude-1000/-home-ibb-tux-GitHub-spring-ai-damage-detector/61e3cdae-98fd-4ce4-acda-afc73c0bb548/scratchpad/spike_transistor_damage_detection/probe.py`. Raw responses: `cache.jsonl` in the same folder.

## Probe question
Does at least one pipeline reach **zero false passes** on all 40 defect images while auto-passing at least 80% of good images?

## Verdict: DOESN'T WORK (probe question not met). Variant A comes closest
- **No variant reaches zero false passes AND ≥80% good auto-pass.**
- **A (Jev on the image) is the only variant that reaches zero false passes**, but only at t ≥ 0.86, where just 14/26 good images (54%) auto-pass; the rest go to review. The margin is one hundredth: the highest P(GOOD) on any defect is 0.85, and good images range 0.71–0.92. The threshold is fitted on the same 66 images, so it is optimistic.
- **B (Qwen description → Jev): FAILS.** Best case 3 false passes (t = 0.995, 20/26 good).
- **C (Qwen direct): FAILS.** 6/40 false passes. Good auto-pass 26/26.
- Final sample: 40 defects + 26 good (all cached good images of the seed-42 selection; N_GOOD raised from 10 to 26 at no Qwen cost). Total OpenRouter spend: $0.0123.
- (History: the first OpenRouter key returned `401 "User not found"`; a new key worked.)
- **Why B fails:** for all 6 C false passes, Qwen's description JSON is fully nominal: body centred, case intact, three full leads, all normal and in their holes. Example: "A black TO-92 transistor is correctly placed with all three leads fully inserted into the board holes." Jev would get no evidence of a defect. A strict rule that passes only if every field is nominal gives the same 6 false passes.
- **Only A can still change the verdict.** Jev image input via OpenRouter is an early feature (E8a).

## Sample (reduced, per user instruction)
- All 40 defect images, 10 per class.
- **Only n=10 good images:** the first 10 of the seed-42 selection of 40. The false-reject estimate is weak: 0/10 rejected still allows a false-reject rate up to about 26% (95% upper bound).
- 16 more good images were already cached before the cut. C passed all 16 as well (26/26 in total).
- C11 was respected: prompts held image bytes plus a generic scene and criteria text. Folder names were used only as scoring keys.

## Variant C: qwen/qwen3.6-35b-a3b via LM Studio (passes only if it says GOOD)
| Class | Recall (correct class or not passed) | Exact class | False passes |
|---|---|---|---|
| BENT_LEAD | 10/10 | 8 (2 called CUT) | 0 |
| CUT_LEAD | 9/10 | 4 (5 called BENT) | 1 (007) |
| DAMAGED_CASE | 7/10 | 7 | 3 (002, 004, 007) |
| MISPLACED | 8/10 | 8 | 2 (000, 006) |
| **Defects** | 34/40 | 27/40 | **6** |
| GOOD auto-pass | 10/10 | 10/10 | n/a |

- **Speed:** 159 s/image on average (max 190 s). About 1571 prompt tokens and 270 output tokens per image at about 6 tokens/s.
- **Cost:** $0 (local).
- **JSON parsing:** 66/66 responses parsed.
- **Decision rule:** any false pass means FALSE.

## Variants A / B: typesafe/jev-1.13 via OpenRouter Decisions API
Pass rule: auto-pass only if choice == GOOD and P(GOOD) ≥ t. n = 40 defects, 26 good.

| t | A false passes | A good auto-pass | B false passes | B good auto-pass |
|---|---|---|---|---|
| 0.5 | 7 | 26/26 | 6 | 26/26 |
| 0.7 | 5 | 26/26 | 6 | 26/26 |
| 0.8 | 2 (damaged_case/002, 005) | 23/26 | 6 | 26/26 |
| 0.86 (derived) | 0 | 14/26 | 6 | 26/26 |
| 0.9 | 0 | 4/26 | 6 | 26/26 |
| 0.95+ | 0 | 0/26 | 6 → 3 at 0.995 | 26 → 20/26 |

- **A** P(GOOD): defects max 0.85; good 0.71–0.92. Mean 1.2 s/image, $0.00015/image (~3.5K input tokens per 1024×1024 PNG).
- **A exact-class accuracy is poor** (20/40): it confuses BENT/CUT and calls 5/10 damaged cases GOOD at t = 0.5. Jev's pass/scrap signal is usable only via a strict threshold; its class label is not reliable for the scrap record.
- **B** mirrors Qwen: P(GOOD) = 1.0 on the nominal-looking defect descriptions. Mean 0.4 s/image, $0.00004/image.
- Jev request shape confirmed: `state` = `["<text>", {"type":"image_url","image_url":{"url":"data:image/png;base64,..."}}]`; response model `typesafe/jev-1.13-20260917`, provider TypeSafe.

## Confusion notes
- The false passes come from what Qwen sees, not from the decision step:
  - cut_lead/007 has a clearly cut left lead, but Qwen reported three full leads in their holes.
  - misplaced/000 has the body shifted left and the lead tips between holes, but Qwen called it centred.
  - Some case damage is subtle. damaged_case/002 has only a small matte scuff.
- CUT and BENT were swapped in 7/20 lead-defect images. This does not change pass/scrap, but the scrap record text would name the wrong defect.
- Qwen never rejected a good image (0/26). It leans toward GOOD, which is the unsafe direction under C10.

## Edge cases and API quirks
- **Qwen thinks by default.** `/no_think`, `chat_template_kwargs.enable_thinking=false` and `reasoning.effort` were all ignored. With thinking on, one image did not finish within 900 s.
- **`"reasoning_effort":"none"` works.** Reasoning tokens drop to 0 and the JSON arrives in `message.content`.
- Qwen accepts `temperature:0` and does see the images. On bent_lead/000 it correctly described the left lead as bent and missing its hole.
- Qwen's replies had no markdown fences. Stripping `<think>` and extracting the outer `{...}` was enough.
- The TypeSafe Decisions API reference `.md` URL returned an empty body.

## Design implications for Spring Boot
1. **Do not rely on Qwen 3.6-35B-A3B as the only vision step.** Options, cheapest first:
   - Variant A (Jev on the image) with a strict threshold: zero false passes on this sample, ~half of good parts sent to review.
   - A stronger vision model (stakeholder decision, open question 2).
   - Good-only anomaly detection fallback (213 good images available).
   - Route everything not clearly good to review.
2. **Zero false passes needs more than one signal before auto-pass.** For example: vision plus a per-lead "tip in hole" check, or anomaly detection plus vision. When the signals disagree, route to review.
3. **Local Qwen speed (about 2.5 min/image) only suits an overnight batch.** Parallelise only if the host has capacity.
4. **Request shapes:**
   - LM Studio (worked):
     `POST {LMSTUDIO_BASE_URL}/chat/completions {"model":"qwen/qwen3.6-35b-a3b","temperature":0,"max_tokens":1500,"reasoning_effort":"none","messages":[{"role":"user","content":[{"type":"text","text":"..."},{"type":"image_url","image_url":{"url":"data:image/png;base64,..."}}]}]}`
     The reply is in `choices[0].message.content`. Use a read timeout of at least 300 s.
   - Jev (confirmed):
     `POST https://openrouter.ai/api/alpha/decisions {"model":"typesafe/jev-1.13","state":["<text>",{"type":"image_url","image_url":{"url":"data:..."}}],"questions":{"defect":{"type":"choice","instructions":"...","criteria":{...5 classes...}}}}`
     The criteria texts are in `probe.py` (`CRITERIA`).
5. **Build the scrap record text from the vision JSON** (`summary` plus the per-lead notes). Treat CUT and BENT labels as unreliable for now.
