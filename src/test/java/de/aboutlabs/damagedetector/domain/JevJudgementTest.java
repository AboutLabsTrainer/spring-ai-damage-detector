package de.aboutlabs.damagedetector.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JevJudgementTest {

    @Test
    void choosing_good_is_recognised() {
        assertThat(new JevJudgement("GOOD", Map.of("GOOD", 0.9), 0.4, 0.0).choseGood()).isTrue();
        assertThat(new JevJudgement("CUT_LEAD", Map.of("GOOD", 0.1), 0.4, 0.0).choseGood()).isFalse();
    }

    @Test
    void probabilities_are_read_for_good_and_for_the_choice_and_default_to_zero() {
        JevJudgement judgement = new JevJudgement("CUT_LEAD", Map.of("GOOD", 0.2, "CUT_LEAD", 0.7), 0.4, 0.0);

        assertThat(judgement.probabilityOfGood()).isEqualTo(0.2);
        assertThat(judgement.probabilityOfChoice()).isEqualTo(0.7);
        assertThat(new JevJudgement("MISPLACED", Map.of("GOOD", 0.2), 0.4, 0.0).probabilityOfChoice()).isZero();
        assertThat(new JevJudgement("CUT_LEAD", Map.of(), 0.4, 0.0).probabilityOfGood()).isZero();
    }

    @Test
    void the_probabilities_are_a_read_only_copy_that_keeps_order() {
        Map<String, Double> source = new HashMap<>(Map.of("GOOD", 0.9));
        JevJudgement judgement = new JevJudgement("GOOD", source, 0.4, 0.0);
        source.put("CUT_LEAD", 0.1);

        assertThat(judgement.probabilities()).containsOnlyKeys("GOOD");
        assertThatThrownBy(() -> judgement.probabilities().put("X", 1.0)).isInstanceOf(UnsupportedOperationException.class);
    }
}
