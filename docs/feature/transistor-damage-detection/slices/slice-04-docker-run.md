# Slice 04: Run the inspection and the evaluation from Docker

**Story:** US-04 · **Job:** J3 · **Persona:** P3 Jonas Berger (illustrative) · **Contract:** DEVOPS, `## Wave: DEVOPS / [REF] Docker contract (slice 04)` in feature-delta.md · **Built in:** DELIVER

**Execution order:** 01 → 02 → 03 → 05 → **04**. This slice is last.

**Estimate: 1.5 days** (re-estimated 2026-11-06 against the DEVOPS contract; reference class: containerising a Spring Boot CLI plus a scripted image gate). This is a documented exception to the ≤1 day slice rule, so it ships in two ordered halves:
- **Half A, 0.5 day:** Dockerfile, `.dockerignore`, `out/` in `.gitignore`, README commands, one run on real images.
- **Half B, 1 day:** `scripts/container-check.sh` (key scan, stub parity, contract rows), the `image` CI job, the rollback drill.

The `verify` and `mutation` CI jobs and PIT are not part of this slice. They are a separate precursor item (0.5 day) right after DELIVER step 1.

## Goal
One documented command produces the same report as the jar, with the API key supplied only at run time.

## IN (user-visible contract)
Batch:
```bash
mkdir -p out
docker run --rm --user "$(id -u):$(id -g)" --env-file .env \
  -v "$PWD/images:/in:ro" -v "$PWD/out:/out" \
  damage-detector:0.1.0 --input=/in --output=/out
```
- The same with `--evaluate --input=/in --output=/out/eval`.
- Output files in `./out` can be deleted by the host user without sudo.
- Missing key → the existing message `No Jev API key: set the OPENROUTER_API_KEY environment variable`, exit 1.
- The README shows the build and run commands.

## OUT
- Registry and publishing, image size targets.
- docker-compose, Kubernetes, web endpoint.
- Copying `images/` into the image (312 MB, licence). Always mounted.

## Learning hypothesis
- **Disproves "the CLI is container-ready without code changes" if it fails:** the containerised run behaves differently from the jar (paths, file ownership, missing outputs, key handling). That would show hidden host assumptions in the adapters.
- **Confirms if it succeeds:** the showcase runs on any Docker host, and the image can be shared without the key.

## Acceptance criteria
The scenario gate is `scripts/container-check.sh`, not `./mvnw verify` (Q4).

1. With good/000.png and cut_lead/000.png mounted and stubbed Jev: `./out` holds report.md, report.json and `scrap/cut_lead/000.png(.txt)`, and the verdict counts equal the jar run.
2. The shared image holds no OpenRouter key and no `.env` in its configuration, its history or its files.
3. Without OPENROUTER_API_KEY: message, exit 1, no image processed.
4. Files in `./out` can be removed by the host user without sudo.
5. **Production data:** the documented command on ≥5 real images from `images/` against real Jev writes report.md. Record the summary line below.

## Dependencies
- Slice 05, because the Docker failure modes rely on its refusals. Slice 03 recommended first (demo runs must not abort).
- Docker Engine on the demo host.

## Dogfood moment
Same day: run the documented command on a machine or user account without JDK/Maven.

## Evidence (fill after the run)
- Summary line: `Inspected 5 images: 1 PASS, 2 SCRAP, 2 REVIEW (REVIEW share 40.0 %; 0 Jev errors in REVIEW), Jev cost $0.00074 -> /out/report.md`
- Date: 2026-11-09. Cost: $0.00074. Command: the documented batch command (`--user "$(id -u):$(id -g)" --env-file .env`, `/in` read-only, `/out` mounted below `out/`), exit code 0, on 5 real images copied from `images/` (good/000-002, cut_lead/000, bent_lead/000), image `damage-detector:0.1.0` built with Docker only. Output files belong to the host user; the key was never printed.
- Ctrl-C check under `docker run` (SIGINT to the client after 6 s, 40 images): `exit code: 130`, no `report.md`, empty `docker ps`.
- Half A gates: `scripts/container-check.sh --self-test` reports PASS for each detection route (configuration, history, deleted-layer `.env`, key in a layer file, key inside the jar) and for a clean control image. `image-scan` passes on the shared image, and fails (jar-env-file, jar-key) when `.dockerignore` is emptied. The real `.env` hash was identical before and after every run. Base image digests re-checked 2026-11-09: still the newest 21 JRE and JDK noble tags.
