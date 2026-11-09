package de.aboutlabs.damagedetector.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The pure metrics of DDD-4 and the invariants INV-1 and D17, over named outcome sets and thresholds around the edges.
 *
 * <p>CONTRACT_SHAPE: pure-function
 */
class EvaluationTest {

    private static final double[] THRESHOLDS = {0.0, 0.5, 0.6, 0.85, 0.86, 0.875, 0.99, 1.0};

    private static final Map<String, List<LabelledOutcome>> SETS = Map.of(
        "nothing", List.of(),
        "only good", List.of(judged(Label.GOOD, "GOOD", 0.91), judged(Label.GOOD, "GOOD", 0.85), judged(Label.GOOD, "CUT_LEAD", 0.1)),
        "only defects", List.of(judged(Label.CUT_LEAD, "CUT_LEAD", 0.1), judged(Label.MISPLACED, "GOOD", 0.45)),
        "a defect at the edge", List.of(judged(Label.DAMAGED_CASE, "GOOD", 0.86), judged(Label.GOOD, "GOOD", 0.86)),
        "a defect that always passes", List.of(judged(Label.MISPLACED, "GOOD", 1.0), judged(Label.GOOD, "GOOD", 0.91)),
        "a low defect", List.of(judged(Label.BENT_LEAD, "GOOD", 0.5), judged(Label.GOOD, "GOOD", 0.5)),
        "mixed with failures", List.of(judged(Label.GOOD, "GOOD", 0.91), failed(Label.GOOD), failed(Label.BENT_LEAD),
            judged(Label.DAMAGED_CASE, "GOOD", 0.82), judged(Label.CUT_LEAD, "BENT_LEAD", 0.2)),
        "only failures", List.of(failed(Label.GOOD), failed(Label.MISPLACED)));

    static Stream<Arguments> setsAndThresholds() {
        return SETS.entrySet().stream().flatMap(set -> Arrays.stream(THRESHOLDS)
            .mapToObj(threshold -> Arguments.of(set.getKey(), set.getValue(), threshold)));
    }

    static Stream<Arguments> sets() {
        return SETS.entrySet().stream().map(set -> Arguments.of(set.getKey(), set.getValue()));
    }

    @ParameterizedTest(name = "{0} at {1} gives {2} false pass(es)")
    @CsvSource({
        "a defect called GOOD at 0.87,    0.86, 1",
        "a defect called GOOD at 0.87,    0.87, 1",
        "a defect called GOOD at 0.87,    0.88, 0",
        "a defect called CUT_LEAD at 0.99, 0.5, 0",
        "a failed defect,                 0.0, 0"})
    void a_defect_passes_only_when_jev_called_it_good_at_or_above_the_threshold(String image, double threshold, int expectedFalsePasses) {
        List<LabelledOutcome> outcomes = List.of(switch (image) {
            case "a defect called GOOD at 0.87" -> judged(Label.CUT_LEAD, "GOOD", 0.87);
            case "a defect called CUT_LEAD at 0.99" -> judged(Label.CUT_LEAD, "CUT_LEAD", 0.99);
            default -> failed(Label.CUT_LEAD);
        });

        assertThat(Evaluation.of(outcomes, threshold).falsePasses()).isEqualTo(expectedFalsePasses);
    }

    @ParameterizedTest(name = "a good image called GOOD at 0.86 at {0} passes: {1}")
    @CsvSource({"0.86, true", "0.87, false"})
    void a_good_image_passes_only_at_or_above_its_probability(double threshold, boolean passes) {
        Evaluation evaluation = Evaluation.of(List.of(judged(Label.GOOD, "GOOD", 0.86)), threshold);

        assertThat(evaluation.goodPassed()).isEqualTo(passes ? 1 : 0);
        assertThat(evaluation.falsePasses()).isZero();
    }

    @Test
    void a_mixed_set_at_086_has_no_false_pass_and_one_good_passed_of_two() {
        Evaluation evaluation = Evaluation.of(SETS.get("mixed with failures"), 0.86);

        assertThat(evaluation.falsePasses()).isZero();
        assertThat(evaluation.goodPassed()).isEqualTo(1);
        assertThat(evaluation.goodCount()).isEqualTo(2);
        assertThat(evaluation.defectCount()).isEqualTo(3);
    }

    @Test
    void a_mixed_set_at_080_lets_the_082_defect_through() {
        Evaluation evaluation = Evaluation.of(SETS.get("mixed with failures"), 0.80);

        assertThat(evaluation.falsePasses()).isEqualTo(1);
        assertThat(evaluation.goodPassed()).isEqualTo(1);
    }

    @Test
    void the_edge_set_passes_both_images_at_086_and_neither_at_087() {
        List<LabelledOutcome> edge = SETS.get("a defect at the edge");

        assertThat(sweepRow(Evaluation.of(edge, 0.86), 0.86)).containsExactly(1, 1, 1);
        assertThat(sweepRow(Evaluation.of(edge, 0.86), 0.87)).containsExactly(0, 0, 1);
    }

    @Test
    void the_configured_threshold_row_matches_the_headline_of_the_edge_set() {
        Evaluation evaluation = Evaluation.of(SETS.get("a defect at the edge"), 0.875);

        assertThat(sweepRow(evaluation, 0.875)).containsExactly(0, 0, 1);
        assertThat(sweepRow(evaluation, 0.86)).containsExactly(1, 1, 1);
    }

    @ParameterizedTest(name = "{0} at {2}")
    @MethodSource("setsAndThresholds")
    void counts_add_up(String name, List<LabelledOutcome> outcomes, double threshold) {
        Evaluation evaluation = Evaluation.of(outcomes, threshold);

        assertThat(evaluation.goodCount() + evaluation.defectCount()).isEqualTo(outcomes.size());
        assertThat(evaluation.defectsNotPassed() + evaluation.falsePasses()).isEqualTo(evaluation.defectCount());
    }

    @ParameterizedTest(name = "{0} at {2}")
    @MethodSource("setsAndThresholds")
    void the_sweep_holds_every_hundredth_from_050_to_099_plus_the_configured_threshold(String name, List<LabelledOutcome> outcomes, double threshold) {
        List<Double> swept = Evaluation.of(outcomes, threshold).sweep().rows().stream().map(ThresholdSweep.Row::threshold).toList();

        assertThat(swept).contains(threshold);
        for (int hundredths = 50; hundredths <= 99; hundredths++) {
            assertThat(swept).contains(hundredths / 100.0);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sets")
    void raising_the_threshold_never_passes_more_images(String name, List<LabelledOutcome> outcomes) {
        List<ThresholdSweep.Row> rows = Evaluation.of(outcomes, 0.86).sweep().rows();

        assertThat(rows).extracting(ThresholdSweep.Row::falsePasses).isSortedAccordingTo((a, b) -> b - a);
        assertThat(rows).extracting(ThresholdSweep.Row::goodPassed).isSortedAccordingTo((a, b) -> b - a);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sets")
    void the_lowest_safe_threshold_is_safe_and_the_one_below_it_is_not(String name, List<LabelledOutcome> outcomes) {
        ThresholdSweep sweep = Evaluation.of(outcomes, 0.86).sweep();
        OptionalDouble lowest = sweep.lowestSafeThreshold();
        assumeThat(lowest).isPresent();

        assertThat(rowAt(sweep, lowest.getAsDouble()).falsePasses()).isZero();
        if (lowest.getAsDouble() > 0.50) {
            assertThat(rowAt(sweep, Math.round(lowest.getAsDouble() * 100 - 1) / 100.0).falsePasses()).isPositive();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sets")
    void without_a_safe_threshold_even_099_lets_a_defect_through(String name, List<LabelledOutcome> outcomes) {
        ThresholdSweep sweep = Evaluation.of(outcomes, 0.86).sweep();
        assumeThat(sweep.lowestSafeThreshold()).isEmpty();

        assertThat(rowAt(sweep, 0.99).falsePasses()).isPositive();
    }

    @Test
    void a_failed_image_never_passes_at_any_threshold() {
        List<LabelledOutcome> outcomes = SETS.get("only failures");

        assertThat(Evaluation.of(outcomes, 0.0).sweep().rows()).allSatisfy(row -> {
            assertThat(row.falsePasses()).isZero();
            assertThat(row.goodPassed()).isZero();
        });
    }

    @Test
    void no_outcomes_give_an_empty_evaluation() {
        Evaluation evaluation = Evaluation.of(List.of(), 0.86);

        assertThat(evaluation.falsePasses()).isZero();
        assertThat(evaluation.goodCount()).isZero();
        assertThat(evaluation.sweep().lowestSafeThreshold()).hasValue(0.50);
    }

    @Test
    void the_us_02_example_gives_the_safe_threshold_083() {
        List<LabelledOutcome> outcomes = List.of(judged(Label.DAMAGED_CASE, "GOOD", 0.82), judged(Label.GOOD, "GOOD", 0.91),
            judged(Label.GOOD, "GOOD", 0.85), judged(Label.GOOD, "GOOD", 0.88));

        assertThat(Evaluation.of(outcomes, 0.86).sweep().lowestSafeThreshold()).hasValue(0.83);
    }

    private static LabelledOutcome judged(Label label, String choice, double pGood) {
        return new LabelledOutcome("image.png", label,
            new JevOutcome.Judged(new JevJudgement(choice, Map.of("GOOD", pGood), 0.4, 0.0001), 1));
    }

    private static LabelledOutcome failed(Label label) {
        return new LabelledOutcome("image.png", label, new JevOutcome.Failed(3, "HTTP 503 Service Unavailable"));
    }

    private static ThresholdSweep.Row rowAt(ThresholdSweep sweep, double threshold) {
        return SweepRows.rowFor(sweep, threshold);
    }

    /** falsePasses, goodPassed, goodCount of the sweep row at the given threshold. */
    private static List<Integer> sweepRow(Evaluation evaluation, double threshold) {
        ThresholdSweep.Row row = rowAt(evaluation.sweep(), threshold);
        return List.of(row.falsePasses(), row.goodPassed(), row.goodCount());
    }
}
