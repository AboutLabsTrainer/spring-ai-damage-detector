// SCAFFOLD: true
package de.aboutlabs.damagedetector.domain;

import java.util.List;
import java.util.OptionalDouble;

/** The verdicts recomputed for t = 0.50 to 0.99 plus the configured t, from stored outcomes only (D17). */
public final class ThresholdSweep {

    public record Row(double threshold, int falsePasses, int goodPassed, int goodCount) {
    }

    private ThresholdSweep() {
    }

    public List<Row> rows() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    public Row rowFor(double threshold) {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    /** The lowest swept t with 0 false passes; empty if no t up to 0.99 reaches it. */
    public OptionalDouble lowestSafeThreshold() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }
}
