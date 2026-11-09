package de.aboutlabs.damagedetector.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Whether a failed attempt lies behind an outcome decides if its cost is exact or a lower bound (DDD-15).
 *
 * <p>CONTRACT_SHAPE: pure-function
 */
class JevOutcomeTest {

    private static final JevJudgement JUDGEMENT = new JevJudgement("GOOD", Map.of("GOOD", 0.9), 0.4, 0.001);

    @ParameterizedTest
    @CsvSource({"1,false", "2,true", "3,true"})
    void a_judgement_had_a_failed_attempt_only_after_a_retry(int attempts, boolean expected) {
        assertThat(new JevOutcome.Judged(JUDGEMENT, attempts).hadFailedAttempt()).isEqualTo(expected);
    }

    @Test
    void a_failure_always_had_a_failed_attempt() {
        JevOutcome.Failed failed = new JevOutcome.Failed(1, "HTTP 503");

        assertThat(failed.hadFailedAttempt()).isTrue();
        assertThat(failed.attempts()).isEqualTo(1);
        assertThat(failed.reason()).isEqualTo("HTTP 503");
    }
}
