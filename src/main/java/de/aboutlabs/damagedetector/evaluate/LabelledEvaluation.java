// SCAFFOLD: true
package de.aboutlabs.damagedetector.evaluate;

import de.aboutlabs.damagedetector.domain.Label;
import java.util.Optional;

/** The only place where a label is resolved (DDD-3). Not a bean yet: the evaluate use case arrives in DELIVER. */
public final class LabelledEvaluation {

    private LabelledEvaluation() {
    }

    /** The first path segment, matched exactly against the five folder names (DDD-13). */
    public static Optional<Label> labelOf(String relativePath) {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }
}
