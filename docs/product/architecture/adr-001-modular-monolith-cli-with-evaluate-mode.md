# ADR-001: Modular monolith CLI; evaluate mode in the same jar

## Status
Accepted (Option A, confirmed by the user 2026-10-30).

## Context
- The product is a showcase with a single developer/presenter (E6).
- C1: Spring Boot with best practices. C2: runs in Docker. C3: must stay extensible toward a DB and microservices, but those are out of scope now.
- DISCUSS D15 asks for evaluation as a user-facing `--evaluate` flag on the same jar. The labels come from the first sub-folder and are never sent to Jev (C11).
- The walking skeleton (`89c9f4d`) already has a ports-and-adapters layout with a framework-free `domain/`.
- Quality drivers: correctness (C10), ground-truth confidentiality (C11), testability, and time-to-market (about 3.5 days for 4 stories).

## Decision
- Keep **one Spring Boot non-web jar** as a **modular monolith with ports and adapters**: `domain/` (pure), `batch/`, `evaluate/`, `jev/`, `cli/`.
- `--evaluate` is a second use case. It reuses the per-image inspection path (`Inspector`: read bytes → `Jev` port → `PassPolicy`). Label resolution and scoring live only in `evaluate/`.
- ArchUnit rules enforce the boundaries (domain is framework-free; `jev..` cannot see `evaluate..`; only `cli..` writes to the console).

## Alternatives considered
1. **A separate evaluation tool** (a second Maven module or main class) that reads `report.json` from a batch run and joins the labels. Rejected for three reasons. It contradicts D15 (same jar, one command). It needs a two-step workflow and a path-to-label join outside the product. The label-to-image mapping would still need folder walking, so the C11 risk would just move.
2. **Spring Batch job** (chunked steps, job repository). Rejected. It needs a job repository DB (out of scope) and adds a framework for 253 sequential items. The resume/restart value is not required (US-03 handles failures per image).
3. **Separate microservices** (inspection service plus evaluation service). Rejected. The team is one person, there is no independent deployment need, and DISCUSS lists microservices as out of scope. C3 stays open through ports.

## Consequences
- Positive:
  - One artifact and one Docker image.
  - The same `PassPolicy` drives the batch, the evaluation headline and the sweep, so the "configured t row equals the headline" rule holds by construction.
  - Adding a DB or web adapter later means adding adapters only.
- Negative:
  - The production jar contains evaluation code. This is acceptable for a showcase and is guarded by the `--evaluate` flag.
  - The CLI adapter must reject unknown options, so that a typo does not silently run a paid batch.
