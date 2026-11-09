package de.aboutlabs.damagedetector.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** What an inspected image reports: verdict, cost and the human-readable reason (suspected, never certain). */
class InspectionTest {

    private static final PassPolicy POLICY = new PassPolicy(0.86);

    private static JevOutcome judged(String choice, Map<String, Double> probabilities, double cost) {
        return new JevOutcome.Judged(new JevJudgement(choice, probabilities, 0.4, cost));
    }

    @Test
    void the_inspection_carries_image_verdict_outcome_and_the_threshold_it_was_judged_at() {
        JevOutcome outcome = judged("GOOD", Map.of("GOOD", 0.9), 0.001);

        Inspection inspection = Inspection.of("sub/a.png", outcome, POLICY);

        assertThat(inspection.image()).isEqualTo("sub/a.png");
        assertThat(inspection.verdict()).isEqualTo(Verdict.PASS);
        assertThat(inspection.outcome()).isSameAs(outcome);
        assertThat(inspection.passThreshold()).isEqualTo(0.86);
    }

    @Test
    void a_judged_image_reports_its_cost() {
        Inspection inspection = Inspection.of("a.png", judged("GOOD", Map.of("GOOD", 0.9), 0.0042), POLICY);

        assertThat(inspection.judged()).isTrue();
        assertThat(inspection.cost()).isEqualTo(0.0042);
    }

    @Test
    void a_failed_image_is_not_judged_and_costs_nothing_known() {
        Inspection inspection = Inspection.of("a.png", new JevOutcome.Failed(3, "HTTP 503"), POLICY);

        assertThat(inspection.judged()).isFalse();
        assertThat(inspection.cost()).isZero();
        assertThat(inspection.verdict()).isEqualTo(Verdict.REVIEW);
    }

    @Test
    void a_pass_says_good_with_its_probability() {
        Inspection inspection = Inspection.of("a.png", judged("GOOD", Map.of("GOOD", 0.9123), 0.0), POLICY);

        assertThat(inspection.description()).isEqualTo("GOOD; P(GOOD)=0.91");
    }

    @Test
    void a_scrap_names_the_suspected_class_and_its_probability() {
        Inspection inspection = Inspection.of("a.png", judged("CUT_LEAD", Map.of("GOOD", 0.1, "CUT_LEAD", 0.75), 0.0), POLICY);

        assertThat(inspection.description()).isEqualTo("Suspected CUT_LEAD (p=0.75); P(GOOD)=0.10");
    }

    @Test
    void a_review_explains_it_is_below_the_threshold() {
        Inspection inspection = Inspection.of("a.png", judged("GOOD", Map.of("GOOD", 0.7), 0.0), POLICY);

        assertThat(inspection.description()).isEqualTo("Leans GOOD but below the pass threshold 0.86; P(GOOD)=0.70");
    }

    @ParameterizedTest
    @CsvSource({"1,'Jev call failed after 1 attempt: HTTP 503'", "2,'Jev call failed after 2 attempts: HTTP 503'", "3,'Jev call failed after 3 attempts: HTTP 503'"})
    void a_failure_names_the_attempts_and_the_reason(int attempts, String expected) {
        Inspection inspection = Inspection.of("a.png", new JevOutcome.Failed(attempts, "HTTP 503"), POLICY);

        assertThat(inspection.description()).isEqualTo(expected);
    }
}
