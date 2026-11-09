package de.aboutlabs.damagedetector.domain;

import java.util.NoSuchElementException;

/** Test support: finds the row of a sweep at a threshold. */
final class SweepRows {

    private static final double SAME_THRESHOLD = 1e-9;

    private SweepRows() {
    }

    static ThresholdSweep.Row rowFor(ThresholdSweep sweep, double threshold) {
        return sweep.rows().stream().filter(row -> Math.abs(row.threshold() - threshold) < SAME_THRESHOLD).findFirst()
            .orElseThrow(() -> new NoSuchElementException("no sweep row for threshold " + threshold));
    }
}
