// SCAFFOLD: true
package de.aboutlabs.damagedetector.cli;

import de.aboutlabs.damagedetector.domain.PassPolicy;
import de.aboutlabs.damagedetector.probe.Probe;
import de.aboutlabs.damagedetector.probe.ProbeResult;

/** The raw detector.pass-threshold text, so that "abc" is a refusal and not a binding failure (DDD-32). Pure value. */
public final class PassThreshold implements Probe {

    private PassThreshold() {
    }

    public static PassThreshold of(String raw) {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    public PassPolicy policy() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    @Override
    public ProbeResult probe() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }
}
