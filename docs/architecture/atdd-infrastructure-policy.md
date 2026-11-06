# ATDD Infrastructure Policy

Per `nw-distill` § Project Infrastructure Policy. One file per project. Apply-if-exists; write-if-absent; rewrite with `--policy=fresh`. Git history is the audit trail.

## Driving
| Port | Mechanism | Note |
|---|---|---|
| CLI (`--input`, `--output`, `--evaluate`) | Spring Boot app started in-process with CLI args | Default for most scenarios (fast) |
| CLI, real process | Subprocess of the built jar | `@driving_adapter` scenarios: arguments, stdout/stderr, exit codes 0-3 |
| `docker run` | `scripts/container-check.sh` | Outside `./mvnw verify`, which stays Docker-free |

## Driven internal (real)
| Port | Mechanism | Note |
|---|---|---|
| Input read port (`ImageFolder`) | Real files in a temp folder | |
| `OutputFolder`, `ReportWriter`, `EvaluationWriter`, `LogSink` | Real files in a temp folder | |

## Driven external / non-deterministic (fake)
| Port | Fake | Note |
|---|---|---|
| Jev, whole app or real jar | WireMock on a real port | Needed for subprocess scenarios, real timeouts and connection failures |
| Jev, direct adapter tests of `OpenRouterJev` | `MockRestServiceServer.bindTo(RestClient.Builder)` | No port; request shape, status handling, unreadable answers. Counts for PIT, which does not count Cucumber |
| Retry wait | Fake sleeper | No real backoff delay in tests |
| Run date (`Clock`) | Fixed clock | Same treatment as the sleeper. Confirmed by the user 2026-11-06 (Q10) |
