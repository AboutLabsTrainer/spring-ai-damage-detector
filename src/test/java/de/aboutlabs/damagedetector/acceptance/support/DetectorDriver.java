package de.aboutlabs.damagedetector.acceptance.support;

import java.util.List;
import java.util.Map;

/** Starts the detector the way a user does; only the process boundary differs between the two drivers. */
interface DetectorDriver {

    RunResult run(List<String> arguments, Map<String, String> environment);

    /** What the user sees of a finished run. */
    record RunResult(int exitCode, String stdout, String stderr) {

        static final RunResult NONE = new RunResult(-1, "", "");

        List<String> stdoutLines() {
            return stdout.lines().filter(line -> !line.isBlank()).toList();
        }

        List<String> stderrLines() {
            return stderr.lines().filter(line -> !line.isBlank()).toList();
        }
    }
}
