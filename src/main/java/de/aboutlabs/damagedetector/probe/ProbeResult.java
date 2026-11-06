// SCAFFOLD: true
package de.aboutlabs.damagedetector.probe;

import java.util.List;

/** Ok, or refused with a reason and the details the user message needs (paths, the option name, the cause). */
public sealed interface ProbeResult {

    record Ok() implements ProbeResult {
    }

    record Refused(Reason reason, List<String> details) implements ProbeResult {
    }

    enum Reason {
        USAGE,
        INPUT_MISSING,
        NO_IMAGES,
        NO_LABELLED_IMAGES,
        OUTPUT_SAME_AS_OR_INSIDE_INPUT,
        OUTPUT_NOT_WRITABLE,
        NO_API_KEY,
        JEV_BASE_URL_INVALID,
        THRESHOLD_INVALID
    }
}
