package de.aboutlabs.damagedetector.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class ThresholdSweepTest {

    private static ThresholdSweep.Row row(double threshold, int falsePasses) {
        return new ThresholdSweep.Row(threshold, falsePasses, 1, 2);
    }

    @Test
    void a_row_is_found_by_its_threshold() {
        ThresholdSweep sweep = ThresholdSweep.of(List.of(row(0.5, 2), row(0.83, 0)));

        assertThat(sweep.rowFor(0.83)).isEqualTo(row(0.83, 0));
        assertThat(sweep.rowFor(0.5).falsePasses()).isEqualTo(2);
    }

    @Test
    void a_threshold_that_was_not_swept_has_no_row() {
        ThresholdSweep sweep = ThresholdSweep.of(List.of(row(0.5, 2)));

        assertThatThrownBy(() -> sweep.rowFor(0.51)).isInstanceOf(NoSuchElementException.class).hasMessage("no sweep row for threshold 0.51");
    }

    @Test
    void the_lowest_safe_threshold_is_the_first_without_false_passes() {
        ThresholdSweep sweep = ThresholdSweep.of(List.of(row(0.5, 3), row(0.6, 0), row(0.7, 1), row(0.8, 0)));

        assertThat(sweep.lowestSafeThreshold()).hasValue(0.6);
    }

    @Test
    void a_safe_threshold_above_the_sweep_range_does_not_count() {
        ThresholdSweep sweep = ThresholdSweep.of(List.of(row(0.5, 3), row(0.99, 1), row(1.0, 0)));

        assertThat(sweep.lowestSafeThreshold()).isEmpty();
    }

    @Test
    void a_safe_threshold_at_the_end_of_the_range_counts() {
        assertThat(ThresholdSweep.of(List.of(row(0.5, 3), row(0.99, 0))).lowestSafeThreshold()).hasValue(0.99);
    }
}
