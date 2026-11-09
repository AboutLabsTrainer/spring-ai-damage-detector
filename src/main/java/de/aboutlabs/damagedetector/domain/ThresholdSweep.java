package de.aboutlabs.damagedetector.domain;

import java.util.List;
import java.util.OptionalDouble;

/** The verdicts recomputed for t = 0.50 to 0.99 plus the configured t, from stored outcomes only (D17). */
public final class ThresholdSweep {

    public record Row(double threshold, int falsePasses, int goodPassed, int goodCount) {
    }

    private final List<Row> rows;

    private ThresholdSweep(List<Row> rows) {
        this.rows = List.copyOf(rows);
    }

    /** Rows in ascending threshold order. */
    static ThresholdSweep of(List<Row> rows) {
        return new ThresholdSweep(rows);
    }

    public List<Row> rows() {
        return rows;
    }

    /** The lowest swept t with 0 false passes; empty if no t up to 0.99 reaches it. */
    public OptionalDouble lowestSafeThreshold() {
        return rows.stream().filter(row -> row.threshold() <= Evaluation.SWEEP_LAST).filter(row -> row.falsePasses() == 0)
            .mapToDouble(Row::threshold).findFirst();
    }
}
