package de.aboutlabs.damagedetector.inspection;

import de.aboutlabs.damagedetector.probe.Probe;
import de.aboutlabs.damagedetector.probe.ProbeResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** The single read port, root-scoped to --input: relative image paths, sorted, and the bytes of one of them. No labels. */
public final class ImageFolder implements Probe {

    private static final List<String> IMAGE_ENDINGS = List.of(".png", ".jpg", ".jpeg");

    private final Path root;

    private ImageFolder(Path root) {
        this.root = root;
    }

    public static ImageFolder rootedAt(Path root) {
        return new ImageFolder(root);
    }

    public List<String> imagePaths() {
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Input folder does not exist: " + root);
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile).map(this::relativePathOf).filter(ImageFolder::isImage).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] bytesOf(String relativePath) {
        try {
            return Files.readAllBytes(root.resolve(relativePath));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String mediaTypeOf(String relativePath) {
        return lowerCase(relativePath).endsWith(".png") ? "image/png" : "image/jpeg";
    }

    /**
     * Bootstrap step 2: reads metadata only. The output's nearest existing ancestor is resolved to its real path, the
     * missing segments are appended and the result normalized, so shortcuts and {@code ..} across missing folders count.
     */
    public ProbeResult checkRun(Path output) {
        if (!Files.isDirectory(root)) {
            return new ProbeResult.Refused(ProbeResult.Reason.INPUT_MISSING, List.of(root.toString()));
        }
        if (isSameOrInside(realPathOf(output), realPathOf(root), output)) {
            return new ProbeResult.Refused(ProbeResult.Reason.OUTPUT_SAME_AS_OR_INSIDE_INPUT,
                List.of(output.toString(), root.toString()));
        }
        return new ProbeResult.Ok();
    }

    private boolean isSameOrInside(Path realOutput, Path realInput, Path output) {
        return realOutput.startsWith(realInput) || sameFile(output);
    }

    private boolean sameFile(Path output) {
        try {
            return Files.exists(output) && Files.isSameFile(output, root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path realPathOf(Path path) {
        Path absolute = path.toAbsolutePath();
        Path existing = absolute;
        while (!Files.exists(existing)) {
            existing = existing.getParent();
        }
        try {
            return existing.toRealPath().resolve(existing.relativize(absolute)).normalize();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ProbeResult probe() {
        if (imagePaths().isEmpty()) {
            return new ProbeResult.Refused(ProbeResult.Reason.NO_IMAGES, List.of(root.toString()));
        }
        return new ProbeResult.Ok();
    }

    private String relativePathOf(Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    private static boolean isImage(String relativePath) {
        return IMAGE_ENDINGS.stream().anyMatch(lowerCase(relativePath)::endsWith);
    }

    private static String lowerCase(String relativePath) {
        return relativePath.toLowerCase(Locale.ROOT);
    }
}
