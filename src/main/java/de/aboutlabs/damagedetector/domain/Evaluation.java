// SCAFFOLD: true
package de.aboutlabs.damagedetector.domain;

import java.util.List;

/** Pure metrics over one Jev pass at the configured threshold (DDD-4). */
public final class Evaluation {

    private Evaluation() {
    }

    public static Evaluation of(List<LabelledOutcome> outcomes, double configuredThreshold) {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    public int falsePasses() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    public int goodPassed() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    public int goodCount() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    public int defectsNotPassed() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    public int defectCount() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    public ThresholdSweep sweep() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }
}
