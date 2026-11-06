# ADR-005: Central CLI error feedback, bootstrap probes, logging and exit codes

## Status
Accepted with Option A (user, 2026-10-30). UC-2 was confirmed on 2026-10-30. The log location `<output>/damage-detector.log` was decided by the user on 2026-10-30. Revised on 2026-10-30 for review iteration 2, and on 2026-11-06 for the final wave gate (revision cycle 1) with user decisions Q5 and Q7.

## Context
- **D23 / INV-5:** whenever a run cannot do what was asked, the user sees one short, actionable stderr line with the cause and the fix. The exit code is non-zero, there is no stack trace by default, and the details go to the log.
- **D14 and D18** also need non-zero exits. **Q7:** exit 2 also prints one stderr line.
- **D25 / INV-6:** an interrupted run leaves no partial report.
- **In the skeleton:**
  - `OpenRouterJev`'s constructor throws on a missing key during wiring, which prints a stack trace.
  - `BatchCommand` throws `IllegalArgumentException`.
  - Console logging is on.
- **The log file must live in `--output`,** but Spring configures logging before any bean or runner exists. Spring therefore has to know the output folder before it starts, and that folder must exist and be writable.
- **`main` calls `System.exit`,** so an in-process acceptance test needs an entry point that returns the exit code and still runs the bootstrap.
- **Earned Trust:** refuse before any paid call, and before any write, when the environment cannot honour the run.

## Decision
1. **One pure mapping, `cli/ErrorFeedback`.**
   - It turns a refusal or an unexpected exception into a `Feedback(line, exitCode)` value. **It prints nothing.**
   - The printers are named: `DetectorLauncher` (bootstrap and start-up failures) and `cli.DetectorCommand` (everything inside a run). ArchUnit rule 3 allows only these two.
   - **The refusal set is closed.** It equals `ProbeResult.Reason`, and every reason has this text, all with exit 1:

     | Reason | Line (normative) |
     |---|---|
     | `USAGE` | `Usage: java -jar damage-detector.jar [--evaluate] --input=<folder> --output=<folder> (<problem>)`, where `<problem>` is `missing --input`, `missing --output`, `unknown option <option>` or `--evaluate takes no value`. In tests `<option>` is for example `--evalute` |
     | `INPUT_MISSING` | `Input folder does not exist: <i>` |
     | `NO_IMAGES` | `No PNG/JPEG images below <i>` |
     | `NO_LABELLED_IMAGES` | `No labelled images below <i>: expected sub-folders good, bent_lead, cut_lead, damaged_case, misplaced` |
     | `OUTPUT_SAME_AS_OR_INSIDE_INPUT` | `Output folder <o> is the same as or inside input folder <i>: choose an output outside the input` |
     | `OUTPUT_NOT_WRITABLE` | `Cannot write to output folder <o>: <cause>`, where `<cause>` is `permission denied`, `not a folder` or the short exception text |
     | `NO_API_KEY` | `No Jev API key: set the OPENROUTER_API_KEY environment variable` |
     | `JEV_BASE_URL_INVALID` | `Jev base URL cannot be used: set detector.jev.base-url to an http or https address` |
     | `THRESHOLD_INVALID` | `Pass threshold must be a number from 0 to 1: set detector.pass-threshold (was <value>)` |

     `<i>` and `<o>` are the paths **as the user typed them**, because that is what the user can find and fix. The base-URL line does not echo the value, because an address can carry credentials (INV-3).
   - **Completed-run lines:**

     | Situation | Line | Exit |
     |---|---|---|
     | No image judged (D14) | `0 of <n> images judged by Jev - check OPENROUTER_API_KEY and network` | 3 |
     | False pass at the configured t (Q7) | `<n> false passes at pass threshold <t>: see <o>/evaluation.md` | 2 |

   - **Unexpected exception, phase-dependent:**

     | Phase | Line | Method |
     |---|---|---|
     | Before bootstrap step 5 (no log exists) | `Inspection failed: <short cause>.` | `feedbackBeforeLog(Throwable)` |
     | From step 5 on (the log exists) | `Inspection failed: <short cause>. See log for details.` | `feedbackFor(Throwable)` |

     `<short cause>` is the exception message, or its type name if it has none, reduced to its first line. Both give exit 1.
2. **`DetectorLauncher` owns the bootstrap.** `DamageDetectorApplication.main` is only `System.exit(DetectorLauncher.launch(args))`. `launch` runs the program, never ends the JVM, and returns the exit code. These steps run before `SpringApplication.run`, in order. Any refusal gives exit 1 and 0 Jev calls.
   1. **Pre-parse the arguments:** `CliArguments.check(args)` returns a `ProbeResult` and writes nothing.
      - `--input` and `--output` are required;
      - unknown options are refused, except dotted properties and the Spring flags `debug` and `trace`;
      - `--evaluate` must have no value.
   2. **Real-path check, no write:** `ImageFolder.rootedAt(input).checkRun(output)` returns a `ProbeResult`.
      - The input must exist as a folder, and its real path is taken with `toRealPath`.
      - The output real path is the real path of its nearest existing ancestor, with the not-yet-existing segments appended and the result **normalized**.
      - The run is refused if the output equals the input (`isSameFile`) or lies inside it (component-wise `startsWith` on the normalized real paths).
   3. **Create `--output`** in `OutputFolder.probe()`. This is a declared delta: only the missing directories down to `--output`.
   4. **Write probe,** in the same `probe()` call: write `<output>/.damage-detector-write-probe.tmp`, atomically rename it to `.damage-detector-write-probe`, and delete it. On a normal exit it is always removed. A JVM kill in between may leave an empty probe file. That leftover is documented and harmless: it is not an image, and it is overwritten next time.
   5. **`LogSink.defaultPropertiesFor(realOutput)`** returns the property map naming `<real output>/damage-detector.log`. It is a pure function. The launcher sets the map as SpringApplication default properties, not as ambient `application.yml` config, and the logging system performs the only write. Console logging is off unless `--debug` is given. There is **no temp-dir fallback**: a refusal in steps 1–4 has no log file, and its stderr line is the complete feedback.
   6. **Start Spring.** `DetectorCommand` then probes `PassThreshold`, the input (≥ 1 image; in evaluate mode ≥ 1 labelled image instead), and the Jev-side check (key, then base URL), and runs the use case. The root composition injects that check as a plain `probe.Probe`, so `cli` never depends on `jev`.
3. **`launch` keeps its `ApplicationContextInitializer` varargs as public surface, as a test seam.**
   - The in-process driver lives in another package, so `launch` must be public.
   - The varargs replace only the two non-deterministic capabilities (`Sleeper`, `Clock`) and leave steps 1–5 and the real wiring under test.
   - `main` passes none, and nothing on the command line reaches them.
4. **Bean construction does not validate** (wire, then probe, then use). To make that possible for the threshold, `DetectorProperties` binds `detector.pass-threshold` as raw text, and `cli/PassThreshold` parses it. `abc` is therefore a refusal, not a binding failure. `DetectorCommand` hands the resulting `PassPolicy` to the use case after the probes.
5. **Exit codes:**

   | Code | Meaning |
   |---|---|
   | 0 | OK |
   | 1 | Refused or unexpected error |
   | 2 | Evaluate: false pass(es) at the configured t (D18) |
   | 3 | No image judged by Jev (D14) |
   | 130, 143, 137 | Interrupted: SIGINT, SIGTERM, SIGKILL (D25) |

   Precedence among 1, 2 and 3 is 1 > 3 > 2. Reports are written for codes 2 and 3. Inside Spring the code is reported through `ExitCodeGenerator`, and `launch` returns it. For a bootstrap refusal `launch` returns it directly. Codes 130, 143 and 137 come from the JVM and the OS: the program adds no signal handler.
6. **Reports are written only at the end of a run,** atomically, through `OutputFolder` (temp file + rename, `.md` last). See brief.md, "Writing reports".
7. **Console access:** only `DetectorLauncher` and `cli.DetectorCommand` write to `System.out` and `System.err` (ArchUnit rule 3).
8. **`application.yml` contract:**
   - It holds only defaults: threshold, retry, Jev timeouts, Jev base URL and model, and the key placeholder `${OPENROUTER_API_KEY:}`.
   - It sets the console log off and `root: warn`.
   - It holds **no log file path**.
   - The old `spring.http.client.*` keys are removed in DELIVER step 1.

## Alternatives considered
1. **Spring Boot `FailureAnalyzer` plus `ExitCodeExceptionMapper`.** Rejected: it prints a multi-line block, covers start-up only, and scatters the messages across classes.
2. **Each component prints its own message.** Rejected: there would be no single test point, and ArchUnit rule 3 forbids it.
3. **`ErrorFeedback` prints the line itself.** Rejected: printing makes it an effectful component. As a pure mapping it can be tested exhaustively, and the same mapping serves both the bootstrap and the runner.
4. **Key check in the adapter constructor.** Rejected: it fails during wiring with a stack trace and cannot be ordered with the other probes.
5. **Bind the threshold as a `double` and translate the binding exception.** Rejected: `abc` would fail inside Spring's binder, and the refusal text would depend on walking Spring's exception chain.
6. **Log path from ambient config (`logging.file.name: ${output}/…` in `application.yml`).** Rejected: the output folder may not exist yet, and output-inside-input is not checked first. Logback would then create files in unchecked places or print status noise to the console.
7. **A Spring `EnvironmentPostProcessor` to set the log path.** Rejected: it runs inside Spring's start-up, after logging is initialised, and cannot be ordered with the path checks and the output creation without duplicating them.
8. **A temp-dir log fallback.** Rejected (user decision): it hides the log away from the evidence, and it is lost with `docker run --rm`.
9. **A test-only property or a second entry point instead of the `launch` varargs.** Rejected: a property would be a behaviour switch reachable from the CLI, and a second entry point would leave the tested path different from the production path.

## Consequences
- Positive:
  - Every INV-5 row is testable at one driving port, with stderr, exit code, Jev call count and the output tree asserted.
  - The mapping is exhaustively testable as a pure function.
  - There are no paid calls and no stray writes on a misconfigured run.
  - A bad Docker mount becomes `Cannot write to output folder /out: permission denied`.
- Negative:
  - The bootstrap runs before Spring, so it uses plain objects (`CliArguments`, `ImageFolder`/`OutputFolder` factories, `ErrorFeedback`) without DI. The crafter keeps these classes free of Spring.
  - Distinct exit codes must be documented in the README.
  - A refusal before step 5 leaves no log file, by design.
  - Other unusable settings outside the closed set (for example `detector.jev.retry.max-attempts=abc`) fail at start-up and give the generic `Inspection failed` line. They are one line and exit 1, but less specific.
