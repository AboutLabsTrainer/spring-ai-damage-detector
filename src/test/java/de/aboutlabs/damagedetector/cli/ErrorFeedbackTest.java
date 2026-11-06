package de.aboutlabs.damagedetector.cli;

import static org.assertj.core.api.Assertions.assertThat;

import de.aboutlabs.damagedetector.cli.ErrorFeedback.Feedback;
import de.aboutlabs.damagedetector.probe.ProbeResult.Reason;
import de.aboutlabs.damagedetector.probe.ProbeResult.Refused;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Closed error set of INV-5: every refusal has its line and exit 1. */
@Disabled("pending: slice 05, ErrorFeedback; slices 01 and 03 need its exit-2 and exit-3 lines (ADR-005, DDD-33)")
class ErrorFeedbackTest {

    private final ErrorFeedback feedback = new ErrorFeedback();

    static Stream<Arguments> refusals() {
        return Stream.of(
            Arguments.of(Reason.USAGE, List.of("missing --output"),
                "Usage: java -jar damage-detector.jar [--evaluate] --input=<folder> --output=<folder> (missing --output)"),
            Arguments.of(Reason.USAGE, List.of("unknown option --evalute"),
                "Usage: java -jar damage-detector.jar [--evaluate] --input=<folder> --output=<folder> (unknown option --evalute)"),
            Arguments.of(Reason.USAGE, List.of("--evaluate takes no value"),
                "Usage: java -jar damage-detector.jar [--evaluate] --input=<folder> --output=<folder> (--evaluate takes no value)"),
            Arguments.of(Reason.INPUT_MISSING, List.of("/home/jonas/imgs"), "Input folder does not exist: /home/jonas/imgs"),
            Arguments.of(Reason.NO_IMAGES, List.of("/home/jonas/empty"), "No PNG/JPEG images below /home/jonas/empty"),
            Arguments.of(Reason.NO_LABELLED_IMAGES, List.of("out"),
                "No labelled images below out: expected sub-folders good, bent_lead, cut_lead, damaged_case, misplaced"),
            Arguments.of(Reason.OUTPUT_SAME_AS_OR_INSIDE_INPUT, List.of("/in/out", "/in"),
                "Output folder /in/out is the same as or inside input folder /in: choose an output outside the input"),
            Arguments.of(Reason.OUTPUT_NOT_WRITABLE, List.of("/out", "permission denied"),
                "Cannot write to output folder /out: permission denied"),
            Arguments.of(Reason.NO_API_KEY, List.of(), "No Jev API key: set the OPENROUTER_API_KEY environment variable"),
            Arguments.of(Reason.JEV_BASE_URL_INVALID, List.of(),
                "Jev base URL cannot be used: set detector.jev.base-url to an http or https address"),
            Arguments.of(Reason.THRESHOLD_INVALID, List.of("abc"),
                "Pass threshold must be a number from 0 to 1: set detector.pass-threshold (was abc)"));
    }

    @ParameterizedTest
    @MethodSource("refusals")
    void every_refusal_gives_its_line_and_exit_1(Reason reason, List<String> details, String line) {
        assertThat(feedback.feedbackFor(new Refused(reason, details))).isEqualTo(new Feedback(line, 1));
    }

    @Test
    void every_refusal_reason_has_a_line() {
        for (Reason reason : Reason.values()) {
            Feedback result = feedback.feedbackFor(new Refused(reason, List.of("a", "b")));
            assertThat(result.exitCode()).as(reason.name()).isEqualTo(1);
            assertThat(result.line()).as(reason.name()).isNotBlank();
        }
    }

    @Test
    void an_unexpected_exception_points_to_the_log() {
        assertThat(feedback.feedbackFor(new IllegalStateException("disk full")))
            .isEqualTo(new Feedback("Inspection failed: disk full. See log for details.", 1));
    }

    @Test
    void an_exception_without_a_message_is_named_by_its_type() {
        assertThat(feedback.feedbackFor(new IllegalStateException()).line()).contains("IllegalStateException");
    }

    @Test
    void before_the_log_exists_the_line_does_not_point_to_it() {
        assertThat(feedback.feedbackBeforeLog(new IllegalStateException("disk full")))
            .isEqualTo(new Feedback("Inspection failed: disk full.", 1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"first\n\tat second", "first\r\nsecond", "first\n"})
    void only_the_first_line_of_a_cause_is_shown(String message) {
        assertThat(feedback.feedbackFor(new IllegalStateException(message)))
            .isEqualTo(new Feedback("Inspection failed: first. See log for details.", 1));
        assertThat(feedback.feedbackBeforeLog(new IllegalStateException(message)))
            .isEqualTo(new Feedback("Inspection failed: first.", 1));
    }

    @Test
    void a_false_pass_points_to_the_evaluation() {
        assertThat(feedback.falsePasses(1, 0.86, "out/eval/evaluation.md"))
            .isEqualTo(new Feedback("1 false passes at pass threshold 0.86: see out/eval/evaluation.md", 2));
    }

    @Test
    void no_image_judged_names_the_key_and_the_network() {
        assertThat(feedback.noImageJudged(4))
            .isEqualTo(new Feedback("0 of 4 images judged by Jev - check OPENROUTER_API_KEY and network", 3));
    }

    @ParameterizedTest
    @CsvSource({
        "4, 0, 0",
        "4, 1, 2",
        "1, 3, 2",
        "0, 0, 3",
        "0, 1, 3"
    })
    void a_completed_run_ends_with_its_code_and_nothing_judged_wins_over_false_pass(int judged, int falsePasses, int exitCode) {
        assertThat(feedback.exitCodeOfCompletedRun(judged, falsePasses)).isEqualTo(exitCode);
    }
}
