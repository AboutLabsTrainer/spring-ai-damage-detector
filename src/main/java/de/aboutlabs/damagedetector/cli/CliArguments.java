// SCAFFOLD: true
package de.aboutlabs.damagedetector.cli;

import de.aboutlabs.damagedetector.probe.ProbeResult;

/** Pre-parser shared by the bootstrap and DetectorCommand. Plain object: it runs before Spring starts. */
public final class CliArguments {

    private CliArguments() {
    }

    /** Bootstrap step 1. A refusal here writes nothing. */
    public static ProbeResult check(String[] args) {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    public static CliArguments of(String[] args) {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    public boolean evaluate() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    public String input() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    public String output() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }
}
