package de.aboutlabs.damagedetector.inspection;

import de.aboutlabs.damagedetector.probe.Probe;
import de.aboutlabs.damagedetector.probe.ProbeResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * The single write port, root-scoped to --output. It rejects paths that escape the root and creates missing parents on
 * every write. Every write is atomic: {@code <name>.tmp} in the same folder, then ATOMIC_MOVE; a failed write deletes it.
 */
public final class OutputFolder implements Probe {

    private static final String PROBE_FILE = ".damage-detector-write-probe";

    private final Path root;

    private OutputFolder(Path root) {
        this.root = root;
    }

    public static OutputFolder rootedAt(Path root) {
        return new OutputFolder(root);
    }

    public void writeString(String relativePath, String content) {
        writeBytes(relativePath, content.getBytes(StandardCharsets.UTF_8));
    }

    public void writeBytes(String relativePath, byte[] content) {
        Path target = targetOf(relativePath);
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.createDirectories(target.getParent());
            publish(temp, target, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The real path of the root, which the launcher hands to LogSink. */
    public Path realRoot() {
        try {
            return root.toRealPath();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Creates the folder, then writes, renames and deletes the probe file, so a filesystem without the rename refuses here. */
    @Override
    public ProbeResult probe() {
        writeString(PROBE_FILE, "");
        try {
            Files.delete(root.resolve(PROBE_FILE));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new ProbeResult.Ok();
    }

    /** Only the relative part is checked: the root itself may hold a shortcut, which normalizing would break. */
    private Path targetOf(String relativePath) {
        Path relative = Path.of(relativePath).normalize();
        if (relative.isAbsolute() || relative.startsWith("..")) {
            throw new IllegalArgumentException("Path leaves the output folder: " + relativePath);
        }
        return root.resolve(relative);
    }

    private static void publish(Path temp, Path target, byte[] content) throws IOException {
        try {
            Files.write(temp, content);
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }
}
