# SPIKE Decisions -- transistor-damage-detection

## Assumption Tested
- Does at least one pipeline reach zero false passes on all 40 defect images while auto-passing ≥80% of good images?

## Probe Verdict
- DOESN'T WORK: no variant meets both targets. Variant A (Jev on the image, OpenRouter `typesafe/jev-1.13`) reaches zero false passes at P(GOOD) ≥ 0.86 but auto-passes only 14/26 good images (54%). B (Qwen description → Jev) and C (Qwen direct) leave 3–6 defects through. See `findings.md`.

## Promotion Decision
- PROMOTE (variant A), decided by the user on 2026-10-29. Rationale: A is the only variant with zero false passes on the sample, it is fast (1.2 s/image) and cheap ($0.00015/image), and the review share is acceptable for a showcase as long as it is reported honestly. Three outcomes instead of two: PASS / SCRAP / REVIEW.

## Walking Skeleton (only if PROMOTE)
- Driving adapter: Spring Boot 3.5 non-web CLI batch (`batch/BatchCommand`, ApplicationRunner): `java -jar target/damage-detector-0.1.0-SNAPSHOT.jar --input=<folder> --output=<folder>`. Recursively collects *.png/*.jpg; only image bytes go to Jev (C11). Output: `report.json`, `report.md` (PASS/SCRAP/REVIEW counts, REVIEW share, cost, table of non-passed images with a "suspected" description), copies plus `.txt` description under `scrap/` and `review/`. Driven adapters: `jev/OpenRouterJev` (RestClient → `POST {base-url}/api/alpha/decisions`), filesystem `batch/ImageFolder` + `batch/ReportWriter`. Domain (`domain/`, framework-free): `PassPolicy` (threshold `detector.pass-threshold`, default 0.86), `Verdict`, `JevJudgement`, `Inspection`, `BatchReport`, port `Jev`.
- Acceptance test: `src/test/resources/features/transistor-damage-detection/acceptance/walking-skeleton.feature` (`@walking_skeleton @driving_port`, Cucumber on JUnit 5 platform, runner `WalkingSkeletonAcceptanceTest`). Starts the real Spring Boot app with CLI args, real filesystem, Jev stubbed with WireMock (asserts request shape, Bearer header and that no file or folder name is sent). `./mvnw verify` green.
- Commit: `89c9f4d` on branch `feature/transistor-damage-detection` (not pushed).
- Demo command: `set -a; . ./.env; set +a; ./mvnw -q package && java -jar target/damage-detector-0.1.0-SNAPSHOT.jar --input=<folder> --output=<folder>`. Real run on 4 renamed images (good/000, good/001, bent_lead/000, misplaced/000): 1 PASS, 1 SCRAP (bent lead, suspected CUT_LEAD), 2 REVIEW (good/001 at P(GOOD)=0.85, misplaced/000 at 0.45), 0 false passes, $0.00059.
- Known gaps (deliberately out of the skeleton): one failed Jev call aborts the batch (no per-image retry/REVIEW fallback), no Dockerfile (DEVOPS), no Qwen description writer, threshold not yet re-validated on the remaining good images.

## Design Implications
- Pass rule: PASS only if Jev choice == GOOD and P(GOOD) ≥ 0.86 (configurable). The 0.86 threshold is fitted on the probe sample and is optimistic; DESIGN/DISTILL must re-validate it on the remaining good images.
- Expect roughly half of good parts in REVIEW. The report must show the REVIEW share.
- Jev class labels are unreliable (20/40 exact); the scrap record must not present the Jev class as certain. Show probabilities.
- Local Qwen (159 s/image) is too slow and too lenient as a gate; at most an optional, offline description writer for scrap records.
- Never send file names or folder names to the model (C11).

## Constraints Discovered
- Claude subscription credentials cannot be used in the app (Anthropic consumer terms); API key or local model only.
- Jev via OpenRouter Decisions API: `POST https://openrouter.ai/api/alpha/decisions`, image as `image_url` data URL in `state`, early feature whose format may change.
- Qwen in LM Studio needs `"reasoning_effort":"none"`, otherwise thinking runs past 900 s.
- OpenRouter key lives in `.env` (gitignored); never in code or logs.
