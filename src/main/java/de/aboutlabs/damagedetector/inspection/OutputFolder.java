// SCAFFOLD: true
package de.aboutlabs.damagedetector.inspection;

import de.aboutlabs.damagedetector.probe.Probe;
import de.aboutlabs.damagedetector.probe.ProbeResult;
import java.nio.file.Path;

/**
 * The single write port, root-scoped to --output. It rejects paths that escape the root and creates missing parents on
 * every write. Every write is atomic: {@code <name>.tmp} in the same folder, then ATOMIC_MOVE; a failed write deletes it.
 */
public final class OutputFolder implements Probe {

    private OutputFolder() {
    }

    public static OutputFolder rootedAt(Path root) {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    public void writeString(String relativePath, String content) {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    public void writeBytes(String relativePath, byte[] content) {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    /** The real path of the root, which the launcher hands to LogSink. */
    public Path realRoot() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    /** Creates the folder, then writes, renames and deletes the probe file, so a filesystem without the rename refuses here. */
    @Override
    public ProbeResult probe() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }
}
