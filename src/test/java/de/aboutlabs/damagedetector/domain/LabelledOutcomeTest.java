package de.aboutlabs.damagedetector.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The suspected class counts as correct only when Jev judged the image and named the ground truth.
 *
 * <p>CONTRACT_SHAPE: pure-function
 */
class LabelledOutcomeTest {

    private static LabelledOutcome judged(Label label, String choice) {
        return new LabelledOutcome("a.png", label, new JevOutcome.Judged(new JevJudgement(choice, Map.of("GOOD", 0.1), 0.4, 0.0), 1));
    }

    @ParameterizedTest
    @CsvSource({"CUT_LEAD,CUT_LEAD,true", "CUT_LEAD,BENT_LEAD,false", "GOOD,GOOD,true", "GOOD,CUT_LEAD,false", "MISPLACED,GOOD,false"})
    void the_suspected_class_is_correct_when_it_names_the_label(Label label, String choice, boolean expected) {
        assertThat(judged(label, choice).suspectedClassCorrect()).isEqualTo(expected);
    }

    @Test
    void a_failed_image_never_has_a_correct_suspected_class() {
        LabelledOutcome failed = new LabelledOutcome("a.png", Label.GOOD, new JevOutcome.Failed(3, "HTTP 503"));

        assertThat(failed.suspectedClassCorrect()).isFalse();
    }
}
