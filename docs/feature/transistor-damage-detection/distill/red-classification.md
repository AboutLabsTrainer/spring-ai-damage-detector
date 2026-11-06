# RED classification: transistor-damage-detection

Date 2026-11-06, redone on the final feature set, including milestone 5 and the interrupt outline. The pre-DELIVER gate: every pending test was enabled, run, and put back. DELIVER reads this at PREPARE to confirm RED is genuine.

**Result: 708 failing tests. 707 MISSING_FUNCTIONALITY, 1 FIXTURE_MISSING, BROKEN 0** (`IMPORT_ERROR`, `FIXTURE_BROKEN`, `SETUP_FAILURE`, `WRONG_ASSERTION`, `OBSERVABLE_NOT_AT_PORT`). Every failure is an assertion error.

| Group | Tests | Failing | Passing |
|---|---|---|---|
| Cucumber examples, in-process and real jar | 116 | 116 | 0 |
| Cucumber examples, `@container` | 10 | 10 | 0 |
| Direct JUnit tests, including parameter sets | 583 | 582 | 1 |
| Walking skeleton | 1 | 0 | 1 |

**Correction to the first version of this file.** It counted 70 direct tests. The jqwik classes were named `*Properties`, which surefire does not include, so they never ran in any gate. The first version therefore proved nothing about them. Their replacements are named `*Test` and all ran here.

## How it was run

- **Pass A, as handed off.** `@pending` and `@Disabled` lifted (`-Djunit.jupiter.conditions.deactivate`); scaffolds in place. 661 tests stop at `Not yet implemented -- RED scaffold`. In-process scenarios stop at `DetectorLauncher`, so this pass alone proves little about their later steps.
- **Pass B, a stronger check.** `DetectorLauncher` was temporarily delegated to today's production code, so the in-process scenarios ran to their real assertions. That proves the harness reaches the assertion and that the scenario needs the feature. The change was reverted. The "First failure" column below comes from this pass.
- **Container.** `@container` scenarios fail on the `FAIL <finding>: Not yet implemented` lines of `scripts/container-check.sh`. Docker is not used yet.

Not proved: a scenario's assertions have never been seen to pass, because the features do not exist. A failure at GREEN may still be a wrong oracle rather than a wrong implementation. The report oracle now follows the ratified DDD-14 and DDD-15, so that risk is smaller than before.

**Interrupt harness.** On today's jar both examples of "An interrupted run leaves no finished report" get past the exit-code step (130 for Ctrl-C, 143 for a stop request) and the no-`report.md` step, and fail on the missing log file. So the signals and the wait-for-Jev logic work, and the RED is the declared log.

## What the gate corrected

- First run: a missing report file or field raised an infrastructure exception; it is now an assertion failure.
- First run: six examples passed on today's code without the behaviour they name, and each got a further `Then`.
- Final gate: the earlier version of the table below merged outline examples under the title "Examples". It now has one row per example, and all 76 scenarios appear by title.
- Revision cycle 1: "The tool never moves the threshold on its own" passed on today's code, because it never proved that a different safe threshold existed. It now asserts the 0.88 safe threshold as well.

## Not MISSING_FUNCTIONALITY

`OpenRouterJevAdapterTest.a_recorded_real_answer_is_understood` is **FIXTURE_MISSING**: `src/test/resources/jev/golden-response.json` does not exist. This is a DELIVER prerequisite: record it at the slice-01 production run (OQ-5). No code change can turn this test green.

## Tests that pass and stay enabled

- The walking skeleton.
- `ApplicationDefaultsContractTest.the_key_is_only_a_placeholder_and_there_is_no_log_path`: a preservation guard that holds today. It runs in every `./mvnw verify`.

## Container scenarios

All 10 in `milestone-4-docker-run.feature`: MISSING_FUNCTIONALITY, the named finding of `scripts/container-check.sh` is not `PASS`.

## Reconciliation

| | Rows | Failing |
|---|---|---|
| US-01 | 26 | 26 |
| US-02 | 12 | 12 |
| US-03 | 53 | 53 |
| US-05 | 25 | 25 |
| US-04, `@container` | 10 | 10 |
| Direct JUnit, 68 test methods | 68 | 582 |
| **Total** | **194** | **708** |

## Per scenario and example

First failure comes from pass B, and from the container run for US-04. "Example" is the row of the scenario outline, in table order.

### Cucumber scenarios: one row per example

| Story | Scenario | Example | Class | First failure |
|---|---|---|---|---|
| US-01 | A class without images is still shown, with zero rows |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-01 | A defect that passes is named as a false pass |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-01 | A false pass is also reported as a problem |  | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-01 | A folder without labels is refused before any cost | #1.1 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-01 | A folder without labels is refused before any cost | #1.2 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-01 | A wrong suspected class does not count as correct |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-01 | An evaluation writes nothing but its declared files |  | MISSING_FUNCTIONALITY | State delta violated: output.tree: expected every entry declared for a completed evaluatio |
| US-01 | An image in a deeper sub-folder keeps its label |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-01 | An image without a label is skipped, counted and not sent to Jev | #1.1 | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-01 | An image without a label is skipped, counted and not sent to Jev | #1.2 | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-01 | An image without a label is skipped, counted and not sent to Jev | #1.3 | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-01 | An image without a label is skipped, counted and not sent to Jev | #1.4 | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-01 | Evaluation result can be read from the command line |  | MISSING_FUNCTIONALITY | [stdout] Expecting actual: |
| US-01 | Folder names are only used for scoring |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-01 | Presenter sees how many good parts pass automatically |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-01 | Presenter sees the recall of each defect class |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-01 | Presenter sees zero false passes on the labelled set |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-01 | Running the evaluation again replaces the earlier record |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-01 | The class table separates recall from the suspected class |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-01 | The evaluation keeps its cost and duration as evidence |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-01 | The evaluation names its run date |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-01 | The evaluation names the source of the sample data |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-01 | The evaluation says what its defect images can and cannot prove |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-01 | The false-pass line sits exactly at the pass threshold | #1.1 | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-01 | The false-pass line sits exactly at the pass threshold | #1.2 | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-01 | The false-pass line sits exactly at the pass threshold | #1.3 | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-02 | A defect above the configured threshold moves the safe threshold |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-02 | Every threshold is safe when no defect is judged GOOD |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-02 | Presenter is told when no threshold is safe |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-02 | Presenter sees the trade-off between false passes and auto-passed good parts |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-02 | The chosen threshold applies to the next batch |  | MISSING_FUNCTIONALITY | [Jev errors] Expecting actual not to be null |
| US-02 | The row of the configured threshold equals the headline | #1.1 | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-02 | The row of the configured threshold equals the headline | #1.2 | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-02 | The row of the configured threshold equals the headline | #1.3 | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-02 | The row of the configured threshold equals the headline | #1.4 | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-02 | The sweep costs no extra Jev calls |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-02 | The sweep record is complete and the readable table stays short |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-02 | The tool never moves the threshold on its own |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-03 | A brief Jev outage does not change the verdict |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-03 | A failed image carries no Jev choice |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-03 | A failure that cannot go away is not retried | #1.1 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | A failure that cannot go away is not retried | #1.2 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | A failure that cannot go away is not retried | #1.3 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | A failure that cannot go away is not retried | #1.4 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | A failure that may pass goes away is retried, then the image goes to REVIEW | #1.1 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | A failure that may pass goes away is retried, then the image goes to REVIEW | #1.2 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | A failure that may pass goes away is retried, then the image goes to REVIEW | #1.3 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | A failure that may pass goes away is retried, then the image goes to REVIEW | #1.4 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | A failure that may pass goes away is retried, then the image goes to REVIEW | #1.5 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | A failure that may pass goes away is retried, then the image goes to REVIEW | #1.6 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | A folder that already holds files keeps them |  | MISSING_FUNCTIONALITY | Expecting ListN: ["notes.txt", "report.json", "report.md"] |
| US-03 | A garbled answer followed by a good one gives the normal verdict |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-03 | A request to wait longer than allowed ends the attempts at once |  | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | A success after a failed attempt also shows the cost as a lower bound |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-03 | After a failed image the cost is shown as a lower bound |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-03 | After failed attempts the evaluation shows its cost as a lower bound |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-03 | An answer that cannot be trusted never becomes a verdict | #1.1 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | An answer that cannot be trusted never becomes a verdict | #1.2 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | An answer that cannot be trusted never becomes a verdict | #1.3 | MISSING_FUNCTIONALITY | Expecting actual: "REVIEW: Leans GOOD but below the pass threshold 0.86; P(GOOD)=0.00 |
| US-03 | An answer that cannot be trusted never becomes a verdict | #1.4 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | An answer that cannot be trusted never becomes a verdict | #1.5 | MISSING_FUNCTIONALITY | Expecting actual: "REVIEW: Leans GOOD but below the pass threshold 0.86; P(GOOD)=-0.20 |
| US-03 | An answer that cannot be trusted never becomes a verdict | #1.6 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | An answer that cannot be trusted never becomes a verdict | #1.7 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | An answer that cannot be trusted never becomes a verdict | #1.8 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | An answer that cannot be trusted never becomes a verdict | #1.9 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | An answer that cannot be trusted never becomes a verdict | #1.10 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | An image Jev does not answer in time goes to REVIEW and the batch goes on |  | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | An image whose connection breaks goes to REVIEW |  | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | An interrupted run leaves no finished report | #1.1 | MISSING_FUNCTIONALITY | State delta violated: output.exists: expected set to <true> but went from <false> to <fals |
| US-03 | An interrupted run leaves no finished report | #1.2 | MISSING_FUNCTIONALITY | State delta violated: output.exists: expected set to <true> but went from <false> to <fals |
| US-03 | Batch completes and counts a failed image |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-03 | Batch result can be read from the command line |  | MISSING_FUNCTIONALITY | [stdout] Expecting actual: |
| US-03 | Details on the console change nothing about the result |  | MISSING_FUNCTIONALITY | [Jev errors] Expecting actual not to be null |
| US-03 | Jev's own request to wait is honoured up to the longest allowed wait | #1.1 | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-03 | Jev's own request to wait is honoured up to the longest allowed wait | #1.2 | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-03 | PNG and JPEG images are inspected and other files are ignored |  | MISSING_FUNCTIONALITY | [Jev errors] Expecting actual not to be null |
| US-03 | Petra is warned when Jev cannot be reached at all |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-03 | Petra is warned when no image could be judged |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-03 | The API key never shows up in what the detector leaves behind | #1.1 | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-03 | The API key never shows up in what the detector leaves behind | #1.2 | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-03 | The API key never shows up in what the detector leaves behind | #1.3 | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-03 | The batch of ten reports every image |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-03 | The evaluation counts a failed image as not passed at every threshold |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-03 | The evaluation warns when no image could be judged |  | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-03 | The number of attempts is configurable | #1.1 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | The number of attempts is configurable | #1.2 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | The number of attempts is configurable | #1.3 | MISSING_FUNCTIONALITY | Expecting path: <path> |
| US-03 | The report says how many images Jev judged |  | MISSING_FUNCTIONALITY | [a file the run should have written] Expecting path: |
| US-03 | The waits between attempts grow |  | MISSING_FUNCTIONALITY | [waits between attempts] expected: [1L, 2L] |
| US-03 | The waits stop growing at the longest allowed wait |  | MISSING_FUNCTIONALITY | [waits between attempts] expected: [1L, 2L, 4L, 8L, 10L] |
| US-03 | Without failed attempts the cost is exact |  | MISSING_FUNCTIONALITY | [Jev errors] Expecting actual not to be null |
| US-05 | A missing API key is refused before any cost | #1.1 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | A missing API key is refused before any cost | #1.2 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | A missing API key is refused before any cost | #1.3 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | A mistake on the command line is refused before anything is written | #1.1 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | A mistake on the command line is refused before anything is written | #1.2 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | A mistake on the command line is refused before anything is written | #1.3 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | A mistake on the command line is refused before anything is written | #1.4 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | A mistake on the command line is refused before anything is written | #1.5 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | A mistake on the command line is refused before anything is written | #1.6 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | A mistake on the command line is refused before anything is written | #1.7 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | A mistake on the command line is refused before anything is written | #1.8 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | A mistake on the command line is refused before anything is written | #1.9 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | A mistake on the command line is refused before anything is written | #1.10 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | A mistake on the command line is refused before anything is written | #1.11 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | An input folder without images is refused | #1.1 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | An input folder without images is refused | #1.2 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | An output folder that cannot be written is refused | #1.1 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | An output folder that cannot be written is refused | #1.2 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | An output folder that cannot be written is refused | #1.3 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | An unexpected failure gives one line and leaves the details in the log |  | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | An unusable Jev address is refused before any cost | #1.1 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | An unusable Jev address is refused before any cost | #1.2 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | An unusable pass threshold is refused | #1.1 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | An unusable pass threshold is refused | #1.2 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-05 | An unusable pass threshold is refused | #1.3 | MISSING_FUNCTIONALITY | [stderr must be exactly one line] Expected size: 1 but was: 0 in: |
| US-04 | A forgotten output folder is refused |  | MISSING_FUNCTIONALITY | [container-check findings] Expecting ListFromStream: |
| US-04 | A missing key is reported before any work |  | MISSING_FUNCTIONALITY | [container-check findings] Expecting ListFromStream: |
| US-04 | Checking the image leaves Jonas's own key file alone |  | MISSING_FUNCTIONALITY | [container-check findings] Expecting ListFromStream: |
| US-04 | Docker passes on the false-pass signal |  | MISSING_FUNCTIONALITY | [container-check findings] Expecting ListFromStream: |
| US-04 | Docker passes on the nothing-judged signal |  | MISSING_FUNCTIONALITY | [container-check findings] Expecting ListFromStream: |
| US-04 | Presenter can delete what Docker wrote |  | MISSING_FUNCTIONALITY | [container-check findings] Expecting ListFromStream: |
| US-04 | Presenter gets the evaluation from Docker |  | MISSING_FUNCTIONALITY | [container-check findings] Expecting ListFromStream: |
| US-04 | Presenter gets the usual report from Docker |  | MISSING_FUNCTIONALITY | [container-check findings] Expecting ListFromStream: |
| US-04 | The API key leaves no trace of a Docker run |  | MISSING_FUNCTIONALITY | [container-check findings] Expecting ListFromStream: |
| US-04 | The shared Docker image carries no API key |  | MISSING_FUNCTIONALITY | [container-check findings] Expecting ListFromStream: |

### Direct JUnit tests: one row per test method

"Failing" counts parameter sets.

| Test | Failing | Class | First failure |
|---|---|---|---|
| ApplicationDefaultsContractTest.the_jev_time_limits_and_the_retry_defaults_are_the_agreed_ones | 1 | MISSING_FUNCTIONALITY | Expecting map: {"detector.jev.api-key"="${OPENROUTER_API_KEY:}", "detector.jev.base-url"=" |
| LogSinkTest.the_log_file_lies_directly_in_the_output_folder | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| cli.CliArgumentsTest.a_dotted_property_is_allowed | 3 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| cli.CliArgumentsTest.a_missing_folder_is_named | 2 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| cli.CliArgumentsTest.a_value_after_evaluate_is_refused | 4 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| cli.CliArgumentsTest.an_unknown_option_is_refused_and_named | 7 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| cli.CliArgumentsTest.evaluate_alone_selects_the_evaluation | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| cli.CliArgumentsTest.input_and_output_keep_their_values_in_either_order | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| cli.CliArgumentsTest.no_arguments_at_all_are_refused | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| cli.CliArgumentsTest.the_spring_flags_debug_and_trace_are_allowed | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| cli.ErrorFeedbackTest.a_completed_run_ends_with_its_code_and_nothing_judged_wins_over_false_pass | 5 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| cli.ErrorFeedbackTest.a_false_pass_points_to_the_evaluation | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| cli.ErrorFeedbackTest.an_exception_without_a_message_is_named_by_its_type | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| cli.ErrorFeedbackTest.an_unexpected_exception_points_to_the_log | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| cli.ErrorFeedbackTest.before_the_log_exists_the_line_does_not_point_to_it | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| cli.ErrorFeedbackTest.every_refusal_gives_its_line_and_exit_1 | 11 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| cli.ErrorFeedbackTest.every_refusal_reason_has_a_line | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| cli.ErrorFeedbackTest.no_image_judged_names_the_key_and_the_network | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| cli.ErrorFeedbackTest.only_the_first_line_of_a_cause_is_shown | 3 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| cli.PassThresholdTest.a_number_from_0_to_1_gives_its_policy | 6 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| cli.PassThresholdTest.anything_else_is_refused_and_echoed | 8 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| domain.EvaluationTest.a_failed_image_never_passes_at_any_threshold | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| domain.EvaluationTest.a_false_pass_is_a_defect_jev_called_good_at_or_above_the_threshold | 64 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| domain.EvaluationTest.counts_add_up | 64 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| domain.EvaluationTest.no_outcomes_give_an_empty_evaluation | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| domain.EvaluationTest.raising_the_threshold_never_passes_more_images | 8 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| domain.EvaluationTest.the_lowest_safe_threshold_is_safe_and_the_one_below_it_is_not | 8 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| domain.EvaluationTest.the_row_of_the_configured_threshold_equals_the_headline | 64 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| domain.EvaluationTest.the_sweep_holds_every_hundredth_from_050_to_099_plus_the_configured_threshold | 64 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| domain.EvaluationTest.the_us_02_example_gives_the_safe_threshold_083 | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| evaluate.LabelResolutionTest.anything_else_has_no_label | 9 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| evaluate.LabelResolutionTest.the_first_segment_decides_however_deep_the_image_lies | 8 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| inspection.OutputFolderTest.a_failed_replacement_leaves_the_old_file_whole | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| inspection.OutputFolderTest.a_failed_write_deletes_its_temp_file | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| inspection.OutputFolderTest.a_write_leaves_no_temp_file | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| inspection.OutputFolderTest.rejects_a_path_that_leaves_the_root | 3 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| inspection.OutputFolderTest.rejects_bytes_for_a_path_that_leaves_the_root | 2 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| inspection.OutputFolderTest.the_probe_creates_the_folder_and_leaves_no_probe_file | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| inspection.OutputFolderTest.the_probe_overwrites_a_leftover_probe_file_from_a_killed_run | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| inspection.OutputFolderTest.the_probe_refuses_a_read_only_folder | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| inspection.OutputFolderTest.the_real_root_follows_a_shortcut | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| inspection.OutputFolderTest.writes_below_its_root_and_creates_the_folders_on_the_way | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| inspection.OutputFolderTest.writes_bytes_unchanged | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.OpenRouterJevAdapterTest.a_connection_error_is_transient_and_its_reason_carries_no_request_data | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.OpenRouterJevAdapterTest.a_missing_confidence_and_a_missing_cost_become_zero | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.OpenRouterJevAdapterTest.a_recorded_real_answer_is_understood | 1 | FIXTURE_MISSING | [src/test/resources/jev/golden-response.json, recorded from a real Jev run] Expecting actu |
| jev.OpenRouterJevAdapterTest.a_timeout_is_transient_and_named | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.OpenRouterJevAdapterTest.a_transient_status_is_tried_again_up_to_the_limit | 6 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.OpenRouterJevAdapterTest.a_valid_answer_becomes_a_judgement_with_its_cost_and_confidence | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.OpenRouterJevAdapterTest.an_answer_that_cannot_be_trusted_is_transient_and_unreadable | 14 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.OpenRouterJevAdapterTest.any_other_client_error_is_final_after_one_attempt | 5 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.OpenRouterJevAdapterTest.sends_the_key_the_model_the_image_and_the_fixed_question_and_nothing_about_the_file | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.OpenRouterJevAdapterTest.the_edges_of_the_probability_range_are_valid | 2 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.RetryingJevAttemptLoopTest.every_retry_is_preceded_by_one_wait_that_never_shrinks_or_exceeds_the_cap | 5 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.RetryingJevAttemptLoopTest.the_attempt_loop_stops_at_the_first_answer_the_first_final_failure_or_the_limit | 156 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.RetryingJevTest.a_first_time_success_counts_one_attempt | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.RetryingJevTest.a_retry_after_above_the_cap_gives_up_at_once | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.RetryingJevTest.a_retry_after_that_is_no_whole_number_of_seconds_is_ignored | 4 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.RetryingJevTest.a_retry_after_within_the_cap_replaces_the_backoff | 3 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.RetryingJevTest.a_success_after_a_failure_is_a_normal_judgement | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.RetryingJevTest.the_attempt_count_of_a_late_give_up_is_the_real_one | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.RetryingJevTest.the_key_is_checked_before_the_address | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.RetryingJevTest.the_probe_accepts_a_key_and_a_usable_address_without_calling_jev | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.RetryingJevTest.the_probe_refuses_a_blank_key | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.RetryingJevTest.the_probe_refuses_an_unusable_address | 4 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.RetryingJevTest.there_is_no_wait_after_the_last_attempt | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.RetryingJevTest.waits_double_from_the_initial_backoff | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
| jev.RetryingJevTest.waits_stop_growing_at_the_maximum_backoff | 1 | MISSING_FUNCTIONALITY | Not yet implemented -- RED scaffold |
