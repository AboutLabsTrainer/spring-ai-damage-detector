# Slice 04: Run the inspection and the evaluation from Docker

**Story:** US-04 · **Job:** J3 · **Persona:** P3 Jonas Berger (illustrative) · **Estimate:** ≤1 day (reference class: containerising an existing Spring Boot CLI) · **Implementation owner:** DEVOPS (nw-platform-architect)

## Goal
One documented `docker run` command produces the same report as the jar, with the API key supplied only at run time.

## IN (user-visible contract)
- `docker run --rm --env-file .env -v "$PWD/images:/in:ro" -v "$PWD/out:/out" damage-detector:0.1.0 --input=/in --output=/out`
- The same with `--evaluate --output=/out/eval`.
- Output files in `./out` can be deleted by the host user without sudo.
- Missing key → the existing message `No Jev API key: set the OPENROUTER_API_KEY environment variable`, non-zero exit.
- The README shows the build and run commands.

## OUT
- Dockerfile design, base image, image size targets, registry/publishing, CI pipeline (DEVOPS decides).
- docker-compose, Kubernetes, web endpoint.
- Copying `images/` into the image (312 MB, licence). Always mounted.

## Learning hypothesis
- **Disproves "the CLI is container-ready without code changes" if it fails:** the containerised run behaves differently from the jar (paths, file ownership, missing outputs, key handling). That would show hidden host assumptions in the adapters.
- **Confirms if it succeeds:** the showcase runs on any Docker host, and the image can be shared without the key.

## Acceptance criteria
1. With good/000.png and cut_lead/000.png mounted and stubbed Jev: `./out` holds report.md, report.json and `scrap/cut_lead/000.png(.txt)`, and the verdict counts equal the jar run.
2. `docker image inspect`, `docker history` and the image filesystem contain no OpenRouter key and no `.env`.
3. Without OPENROUTER_API_KEY: message, non-zero exit, no image processed.
4. Files in `./out` can be removed by the host user without sudo.
5. **Production data:** the documented command on ≥5 real images from `images/` against real Jev writes report.md. Record the summary line below.

## Dependencies
- Docker Engine on the demo host. Slice 03 recommended first (demo runs must not abort).
- DEVOPS wave delivers the build. DISCUSS fixes only this contract.

## Dogfood moment
Same day: run the documented command on a machine or user account without JDK/Maven.

## Evidence (fill after the run)
- Summary line: _pending_
