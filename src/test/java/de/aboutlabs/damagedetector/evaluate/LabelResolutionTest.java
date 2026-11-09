package de.aboutlabs.damagedetector.evaluate;

import static org.assertj.core.api.Assertions.assertThat;

import de.aboutlabs.damagedetector.domain.Label;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Label resolution is a pure function of the relative path (DDD-13). */
class LabelResolutionTest {

    @ParameterizedTest
    @CsvSource({
        "good/000.png, GOOD",
        "bent_lead/000.png, BENT_LEAD",
        "cut_lead/000.png, CUT_LEAD",
        "damaged_case/000.png, DAMAGED_CASE",
        "misplaced/000.png, MISPLACED",
        "good/shift-1/000.png, GOOD",
        "cut_lead/a/b/c/003.png, CUT_LEAD",
        "good/misplaced/000.png, GOOD"
    })
    void the_first_segment_decides_however_deep_the_image_lies(String path, Label label) {
        assertThat(LabelledEvaluation.labelOf(path)).contains(label);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "loose-017.png", "good.png", "unsorted/017.png", "Good/000.png", "GOOD/000.png", "good_old/000.png",
        "goods/000.png", "unsorted/good/000.png", "bent-lead/000.png"
    })
    void anything_else_has_no_label(String path) {
        assertThat(LabelledEvaluation.labelOf(path)).isEmpty();
    }
}
