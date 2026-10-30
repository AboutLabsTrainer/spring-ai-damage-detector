# ADR-005: Central CLI error feedback, bootstrap probes, logging and exit codes

## Status
Accepted with Option A (user, 2026-10-30). UC-2 was confirmed on 2026-10-30. The log location `<output>/damage-detector.log` was decided by the user on 2026-10-30. Revised on 2026-10-30 for review iteration 2.

## Context
- **D23 / INV-5:** whenever a run cannot do what was asked, the user sees one short, actionable stderr line with the cause and the fix. The exit code is non-zero, there is no stack trace by default, and the details go to the log.
- **D14 and D18** also need non-zero exits.
- **In the skeleton:**
  - `OpenRouterJev`'s constructor throws on a missing key during wiring, which prints a stack trace.
  - `BatchCommand` throws `IllegalArgumentException`.
  - Console logging is on.
- **The log file must live in `--output`,** but Spring configures logging before any bean or runner exists. Spring therefore has to know the output folder before it starts, and that folder must exist and be writable.
- **Earned Trust:** refuse before any paid call, and before any write, when the environment cannot honour the run.

## Decision
1. **One mapping component, `cli/ErrorFeedback`.**
   - It turns typed refusals and any unexpected exception into one stderr line plus an exit code.
   - Refusal reasons: usage, input missing, no images, no labelled images, output same as or inside input, output not writable, no API key.
   - Anything else maps to `Inspection failed: <short cause>. See log for details.`
   - The INV-5 example texts are the defaults.
   - It is used at two catch points: the `main` bootstrap (start-up, binding and wiring errors, e.g. `--detector.pass-threshold=1.5`) and the CLI runner.
2. **Bootstrap in `main`, the outermost composition root.** These steps run before `SpringApplication.run`, in order. Any refusal gives exit 1 and 0 Jev calls.
   1. **Pre-parse the arguments** (`cli/CliArguments`):
      - `--input` and `--output` are required;
      - unknown options are refused, except dotted properties and the Spring flags `debug` and `trace`;
      - `--evaluate` must have no value.

      A refusal here writes nothing.
   2. **Real-path check, with no write.**
      - The input must exist, and its real path is taken with `toRealPath`.
      - The output real path is the real path of its nearest existing ancestor, with the not-yet-existing segments appended.
      - The run is refused if the output equals the input (`isSameFile`) or lies inside it (component-wise `startsWith` on the real paths).
   3. **Create `--output`** through `OutputFolder`. This is a declared delta: only the missing directories down to `--output`.
   4. **Write probe:** create and delete `<output>/.damage-detector-write-probe`. On a normal exit it is always removed. A JVM kill between the create and the delete may leave an empty probe file. That leftover is documented and harmless: it is not an image, and it is overwritten next time.
   5. **`LogSink`** sets `<real output>/damage-detector.log` as the log file. It does this programmatically, as a SpringApplication default property from the composition root, not as ambient `application.yml` config. Console logging is off unless `--debug` is given. There is **no temp-dir fallback**: a refusal in steps 1–4 has no log file, and its stderr line is the complete feedback.
   6. **Start Spring.** `DetectorCommand` then probes that the input has images (or ≥ 1 labelled image) and that the API key is present, and runs the use case.
3. **Bean construction does not validate** (wire, then probe, then use).
4. **Exit codes:**

   | Code | Meaning |
   |---|---|
   | 0 | OK |
   | 1 | Refused or unexpected error |
   | 2 | Evaluate: false pass(es) at the configured t (D18) |
   | 3 | No image judged by Jev (D14) |

   Precedence is 1 > 3 > 2. Reports are written for codes 2 and 3. Inside Spring the code is reported through `ExitCodeGenerator`; in the bootstrap it is passed directly to `System.exit`.
5. **Console access:** only `cli.ErrorFeedback` and `cli.DetectorCommand` (for the summary line) write to `System.out` and `System.err` (ArchUnit rule 3).
6. **`application.yml` contract:**
   - It holds only defaults: threshold, retry, Jev timeouts, Jev base URL and model, and the key placeholder `${OPENROUTER_API_KEY:}`.
   - It sets the console log off and `root: warn`.
   - It holds **no log file path**.

## Alternatives considered
1. **Spring Boot `FailureAnalyzer` plus `ExitCodeExceptionMapper`.** Rejected: it prints a multi-line block, covers start-up only, and scatters the messages across classes.
2. **Each component prints its own message.** Rejected: there would be no single test point, and ArchUnit rule 3 forbids it.
3. **Key check in the adapter constructor.** Rejected: it fails during wiring with a stack trace and cannot be ordered with the other probes.
4. **Log path from ambient config (`logging.file.name: ${output}/…` in `application.yml`).** Rejected: the output folder may not exist yet, and output-inside-input is not checked first. Logback would then create files in unchecked places or print status noise to the console.
5. **A Spring `EnvironmentPostProcessor` to set the log path.** Rejected: it runs inside Spring's start-up, after logging is initialised, and cannot be ordered with the path checks and the output creation without duplicating them.
6. **A temp-dir log fallback.** Rejected (user decision): it hides the log away from the evidence, and it is lost with `docker run --rm`.

## Consequences
- Positive:
  - Every INV-5 row is testable at one driving port, with stderr, exit code, Jev call count and the output tree asserted.
  - There are no paid calls and no stray writes on a misconfigured run.
  - A bad Docker mount becomes `Cannot write to output folder /out: permission denied`.
- Negative:
  - The bootstrap runs before Spring, so it uses plain objects (`CliArguments`, `ImageFolder`/`OutputFolder` factories, `ErrorFeedback`) without DI. The crafter keeps these classes free of Spring.
  - Distinct exit codes must be documented in the README.
  - A refusal before step 5 leaves no log file, by design.
