package de.aboutlabs.damagedetector.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The zero-false-pass rule (C10) at its edges.
 *
 * <p>CONTRACT_SHAPE: pure-function
 */
class PassPolicyTest {

    private static JevOutcome judged(String choice, double pGood) {
        return new JevOutcome.Judged(new JevJudgement(choice, Map.of("GOOD", pGood), 0.4, 0.0), 1);
    }

    @ParameterizedTest
    @CsvSource({"GOOD,0.86,PASS", "GOOD,0.87,PASS", "GOOD,1.0,PASS", "GOOD,0.8599,REVIEW", "GOOD,0.0,REVIEW",
        "CUT_LEAD,0.99,SCRAP", "BENT_LEAD,0.0,SCRAP"})
    void judges_against_a_threshold_of_086(String choice, double pGood, Verdict expected) {
        assertThat(new PassPolicy(0.86).judge(judged(choice, pGood))).isEqualTo(expected);
    }

    @Test
    void a_failed_outcome_is_always_review() {
        assertThat(new PassPolicy(0.0).judge(new JevOutcome.Failed(3, "HTTP 503"))).isEqualTo(Verdict.REVIEW);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.5, 1.0})
    void accepts_thresholds_within_zero_and_one(double threshold) {
        assertThat(new PassPolicy(threshold).passThreshold()).isEqualTo(threshold);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.0001, 1.0001, -1, 2})
    void rejects_thresholds_outside_zero_and_one(double threshold) {
        assertThatThrownBy(() -> new PassPolicy(threshold)).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("pass threshold must be within [0, 1] but was " + threshold);
    }
}
