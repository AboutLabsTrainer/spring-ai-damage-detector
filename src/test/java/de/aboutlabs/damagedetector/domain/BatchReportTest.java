package de.aboutlabs.damagedetector.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The summary figures QS reads from a batch (DDD-14/15): counts per verdict, review share, errors and the cost lower bound. */
class BatchReportTest {

    private static final PassPolicy POLICY = new PassPolicy(0.86);

    private static JevOutcome judged(String choice, double pGood, double cost, int attempts) {
        return new JevOutcome.Judged(new JevJudgement(choice, Map.of("GOOD", pGood, "CUT_LEAD", 1 - pGood), 0.4, cost), attempts);
    }

    private static Inspection inspect(String image, JevOutcome outcome) {
        return Inspection.of(image, outcome, POLICY);
    }

    private static BatchReport mixed() {
        return new BatchReport(List.of(
            inspect("a.png", judged("GOOD", 0.95, 0.001, 1)),
            inspect("b.png", judged("GOOD", 0.90, 0.002, 1)),
            inspect("c.png", judged("CUT_LEAD", 0.1, 0.004, 1)),
            inspect("d.png", judged("GOOD", 0.70, 0.008, 1)),
            inspect("e.png", new JevOutcome.Failed(3, "HTTP 503"))));
    }

    @Test
    void counts_images_per_verdict() {
        BatchReport report = mixed();

        assertThat(report.count(Verdict.PASS)).isEqualTo(2);
        assertThat(report.count(Verdict.SCRAP)).isEqualTo(1);
        assertThat(report.count(Verdict.REVIEW)).isEqualTo(2);
    }

    @Test
    void the_review_share_is_the_fraction_of_images_in_review() {
        assertThat(mixed().reviewShare()).isEqualTo(0.4);
    }

    @Test
    void an_empty_batch_has_no_review_share_and_nothing_else() {
        BatchReport report = new BatchReport(List.of());

        assertThat(report.reviewShare()).isZero();
        assertThat(report.judged()).isZero();
        assertThat(report.jevErrors()).isZero();
        assertThat(report.jevCost()).isZero();
        assertThat(report.costIsLowerBound()).isFalse();
        assertThat(report.notPassed()).isEmpty();
    }

    @Test
    void a_failed_image_is_a_jev_error_and_a_judged_one_is_not() {
        BatchReport report = mixed();

        assertThat(report.judged()).isEqualTo(4);
        assertThat(report.jevErrors()).isEqualTo(1);
    }

    @Test
    void the_cost_sums_judged_images_and_counts_failed_ones_as_zero() {
        assertThat(mixed().jevCost()).isEqualTo(0.015);
    }

    @Test
    void the_cost_is_exact_when_every_image_was_judged_at_the_first_attempt() {
        BatchReport report = new BatchReport(List.of(inspect("a.png", judged("GOOD", 0.95, 0.001, 1))));

        assertThat(report.costIsLowerBound()).isFalse();
    }

    @Test
    void the_cost_is_a_lower_bound_when_an_image_failed() {
        assertThat(mixed().costIsLowerBound()).isTrue();
    }

    @Test
    void the_cost_is_a_lower_bound_when_an_image_needed_a_retry() {
        BatchReport report = new BatchReport(List.of(
            inspect("a.png", judged("GOOD", 0.95, 0.001, 1)),
            inspect("b.png", judged("GOOD", 0.95, 0.001, 2))));

        assertThat(report.costIsLowerBound()).isTrue();
        assertThat(report.jevErrors()).isZero();
    }

    @Test
    void not_passed_lists_scrap_and_review_images_in_order() {
        assertThat(mixed().notPassed()).extracting(Inspection::image).containsExactly("c.png", "d.png", "e.png");
    }

    @Test
    void the_report_keeps_its_own_copy_of_the_inspections() {
        List<Inspection> source = new ArrayList<>(mixed().inspections());
        BatchReport report = new BatchReport(source);
        source.clear();

        assertThat(report.inspections()).hasSize(5);
        assertThatThrownBy(() -> report.inspections().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
