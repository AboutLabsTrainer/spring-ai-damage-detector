package de.aboutlabs.damagedetector.cli;

import static org.assertj.core.api.Assertions.assertThat;

import de.aboutlabs.damagedetector.probe.ProbeResult;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Bootstrap step 1 (DDD-12): a typo must never turn into a paid run, and allowed options must never be refused. */
@Disabled("pending: slice 05, CliArguments (ADR-005, DDD-33)")
class CliArgumentsTest {

    @ParameterizedTest
    @ValueSource(strings = {"evalute", "evaluat", "evaluates", "Evaluate", "inputs", "dry-run", "help"})
    void an_unknown_option_is_refused_and_named(String option) {
        assertUsageRefusal(new String[] {"--input=in", "--output=out", "--" + option}, "unknown option --" + option);
    }

    @ParameterizedTest
    @ValueSource(strings = {"detector.pass-threshold=0.9", "detector.jev.retry.max-attempts=5", "spring.main.banner-mode=off"})
    void a_dotted_property_is_allowed(String property) {
        assertThat(CliArguments.check(new String[] {"--input=in", "--output=out", "--" + property})).isInstanceOf(ProbeResult.Ok.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false", "images", ""})
    void a_value_after_evaluate_is_refused(String value) {
        assertUsageRefusal(new String[] {"--input=in", "--output=out", "--evaluate=" + value}, "--evaluate takes no value");
    }

    @ParameterizedTest
    @CsvSource({"--output=out, missing --input", "--input=in, missing --output"})
    void a_missing_folder_is_named(String argument, String problem) {
        assertUsageRefusal(new String[] {argument}, problem);
    }

    @Test
    void no_arguments_at_all_are_refused() {
        assertThat(CliArguments.check(new String[0])).isInstanceOf(ProbeResult.Refused.class);
    }

    @Test
    void the_spring_flags_debug_and_trace_are_allowed() {
        assertThat(CliArguments.check(new String[] {"--input=in", "--output=out", "--debug", "--trace"})).isInstanceOf(ProbeResult.Ok.class);
    }

    @Test
    void input_and_output_keep_their_values_in_either_order() {
        CliArguments arguments = CliArguments.of(new String[] {"--output=b/c", "--input=a"});

        assertThat(arguments.input()).isEqualTo("a");
        assertThat(arguments.output()).isEqualTo("b/c");
        assertThat(arguments.evaluate()).isFalse();
    }

    @Test
    void evaluate_alone_selects_the_evaluation() {
        assertThat(CliArguments.of(new String[] {"--evaluate", "--input=in", "--output=out"}).evaluate()).isTrue();
    }

    private static void assertUsageRefusal(String[] arguments, String problem) {
        assertThat(CliArguments.check(arguments)).isEqualTo(new ProbeResult.Refused(ProbeResult.Reason.USAGE, List.of(problem)));
    }
}
