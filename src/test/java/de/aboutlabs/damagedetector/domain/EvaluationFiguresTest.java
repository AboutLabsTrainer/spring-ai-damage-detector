package de.aboutlabs.damagedetector.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The headline figures of DDD-4/15/16: per-class table (recall = not passed), judged/error counts and the cost lower bound. */
class EvaluationFiguresTest {

    private static LabelledOutcome judged(Label label, String choice, double pGood, double cost, int attempts) {
        return new LabelledOutcome("image.png", label,
            new JevOutcome.Judged(new JevJudgement(choice, Map.of("GOOD", pGood), 0.4, cost), attempts));
    }

    private static LabelledOutcome failed(Label label) {
        return new LabelledOutcome("image.png", label, new JevOutcome.Failed(3, "HTTP 503"));
    }

    private static Evaluation mixed() {
        return Evaluation.of(List.of(
            judged(Label.GOOD, "GOOD", 0.91, 0.001, 1),
            judged(Label.GOOD, "CUT_LEAD", 0.10, 0.002, 1),
            judged(Label.CUT_LEAD, "CUT_LEAD", 0.10, 0.004, 1),
            judged(Label.CUT_LEAD, "GOOD", 0.95, 0.008, 1),
            judged(Label.CUT_LEAD, "BENT_LEAD", 0.20, 0.016, 1),
            failed(Label.BENT_LEAD),
            judged(Label.MISPLACED, "GOOD", 0.80, 0.032, 1)), 0.86);
    }

    @Test
    void the_configured_threshold_is_reported() {
        assertThat(mixed().passThreshold()).isEqualTo(0.86);
    }

    @Test
    void the_outcomes_are_kept_in_order() {
        assertThat(mixed().outcomes()).hasSize(7);
        assertThat(mixed().outcomes().get(0).label()).isEqualTo(Label.GOOD);
        assertThat(mixed().outcomes().get(6).label()).isEqualTo(Label.MISPLACED);
    }

    @Test
    void good_images_passed_are_counted() {
        assertThat(mixed().goodPassed()).isEqualTo(1);
        assertThat(mixed().goodCount()).isEqualTo(2);
    }

    @Test
    void the_class_table_has_one_row_per_defect_class_in_declaration_order_also_without_images() {
        List<Evaluation.ClassRow> rows = mixed().classRows();

        assertThat(rows).extracting(Evaluation.ClassRow::label)
            .containsExactly(Label.BENT_LEAD, Label.CUT_LEAD, Label.DAMAGED_CASE, Label.MISPLACED);
    }

    @Test
    void a_class_row_counts_images_not_passed_suspected_correct_and_passed() {
        List<Evaluation.ClassRow> rows = mixed().classRows();

        assertThat(rows.get(0)).isEqualTo(new Evaluation.ClassRow(Label.BENT_LEAD, 1, 1, 0, 0));
        assertThat(rows.get(1)).isEqualTo(new Evaluation.ClassRow(Label.CUT_LEAD, 3, 2, 1, 1));
        assertThat(rows.get(2)).isEqualTo(new Evaluation.ClassRow(Label.DAMAGED_CASE, 0, 0, 0, 0));
        assertThat(rows.get(3)).isEqualTo(new Evaluation.ClassRow(Label.MISPLACED, 1, 1, 0, 0));
    }

    @Test
    void a_good_image_never_appears_in_the_class_table() {
        assertThat(mixed().classRows()).extracting(Evaluation.ClassRow::images).containsExactly(1, 3, 0, 1);
    }

    @Test
    void judged_and_error_counts_separate_failed_images() {
        Evaluation evaluation = mixed();

        assertThat(evaluation.judged()).isEqualTo(6);
        assertThat(evaluation.jevErrors()).isEqualTo(1);
    }

    @Test
    void the_cost_sums_judged_images_only() {
        assertThat(mixed().jevCost()).isEqualTo(0.063);
    }

    @Test
    void the_cost_is_a_lower_bound_when_an_image_failed() {
        assertThat(mixed().costIsLowerBound()).isTrue();
    }

    @Test
    void the_cost_is_a_lower_bound_when_an_image_needed_a_retry() {
        Evaluation evaluation = Evaluation.of(List.of(judged(Label.GOOD, "GOOD", 0.9, 0.001, 2)), 0.86);

        assertThat(evaluation.costIsLowerBound()).isTrue();
        assertThat(evaluation.jevErrors()).isZero();
    }

    @Test
    void the_cost_is_exact_when_every_image_was_judged_at_the_first_attempt() {
        Evaluation evaluation = Evaluation.of(List.of(judged(Label.GOOD, "GOOD", 0.9, 0.001, 1)), 0.86);

        assertThat(evaluation.costIsLowerBound()).isFalse();
        assertThat(evaluation.jevCost()).isEqualTo(0.001);
    }

    @Test
    void verdicts_follow_the_pass_policy() {
        Evaluation evaluation = mixed();

        assertThat(evaluation.verdictOf(evaluation.outcomes().get(0))).isEqualTo(Verdict.PASS);
        assertThat(evaluation.verdictOf(evaluation.outcomes().get(1))).isEqualTo(Verdict.SCRAP);
        assertThat(evaluation.verdictOf(evaluation.outcomes().get(5))).isEqualTo(Verdict.REVIEW);
        assertThat(evaluation.verdictOf(evaluation.outcomes().get(6))).isEqualTo(Verdict.REVIEW);
    }
}
