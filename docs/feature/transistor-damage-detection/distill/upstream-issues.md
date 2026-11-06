# DISTILL upstream issues: transistor-damage-detection

Date 2026-11-06, updated in final wave gate revision cycle 1. No item is a contradiction between waves; reconciliation passed.

## Specification gaps

| # | Gap | Status |
|---|---|---|
| 1 | Report field names and layout | **Closed** by DDD-14, DDD-15 (Q12). `BatchRecord` and `EvaluationRecord` follow them |
| 2 | Text for an unusable base URL | **Closed** by DDD-33 |
| 3 | Text for an unusable threshold | **Closed** by DDD-32, DDD-33 |
| 4 | `Retry-After` that is not a whole number | **Closed** by DDD-6: ignored, normal backoff. `RetryingJevTest` covers it |
| 5 | Typed or real path in the refusal text | **Closed** by DDD-33. UC-5 is **confirmed by the user 2026-11-06**; `Workspace.resolve` is the one place to change if it moves |
| 6 | Stderr line on exit 2 | **Closed** by Q7, DDD-33. New scenario "A false pass is also reported as a problem" |
| 7 | SIGINT or kill during a run | **Closed** by Q5, D25, the DEVOPS interrupt contract. New outline "An interrupted run leaves no report" (C7b) |

## For DELIVER

| # | Item | Status |
|---|---|---|
| 8 | **Golden Jev response missing.** OQ-5 needs `src/test/resources/jev/golden-response.json`. `a_recorded_real_answer_is_understood` is FIXTURE_MISSING until the slice-01 production run records it | **Open**, DELIVER prerequisite |
| 9 | `DetectorLauncher` seam | **Closed** by DDD-30 |
| 10 | Sleeper and clock must be injectable beans; the tests register primary beans | **Open**, DELIVER |
| 11 | Skeleton bypasses the bootstrap | **Closed** by brief.md: `OutputFolder` creates missing parents on write |
| 12 | `OpenRouterJev` imports `DetectorProperties` | **Closed** by DDD-5, DDD-35. A plain-value scaffold constructor exists. DELIVER removes the old constructor, `judge` and `implements Jev` |
| 13 | `RetryingJev` cannot declare `implements Jev` yet | **Open**, DELIVER; the port change makes it possible |
| 14 | Boot 4.1 and test libraries | **Closed** by Q1, DDD-37. Still to check in step 1: `cucumber-picocontainer` and `MockRestServiceServer.bindTo(RestClient.Builder)` on JUnit 6 |
| 15 | ArchUnit rules are not in this wave | **Open**, DELIVER step 1 |
| 16 | Remove `failIfNoTests = false` from `DrivingAdapterAcceptanceIT` when the first real-jar scenario is enabled, right after the Boot 4.1 upgrade (Q11) | **Open**, DELIVER |
| 17 | `jqwik.properties` | **Closed** by Q1: jqwik removed |
| 18 | The notice printed by jqwik | **Closed** by Q1: a deliberate notice from its maintainer, and the library is removed |
| 19 | **Container check self-test (M5).** A script whose checks always pass would look the same as a clean image. Before any `@container` scenario is enabled, DELIVER adds `scripts/container-check.sh --self-test`: it builds a deliberately bad image (canary in `.env`, a key file deleted in a later layer, the canary inside the jar) and must report `FAIL` for `image-holds-no-key`. A Java test on the repackaged jar is the fallback if a bad image is too slow | **Open**, DELIVER slice 04 |
| 20 | `OpenRouterJev` has additive scaffold members (`// SCAFFOLD: true`). They are the only change to an existing production class; its behaviour is unchanged | **Open**, DELIVER replaces them |
