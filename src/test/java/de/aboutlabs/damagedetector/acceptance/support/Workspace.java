package de.aboutlabs.damagedetector.acceptance.support;

import static org.assertj.core.api.Assumptions.assumeThat;

import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.FolderContent;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ImagePath;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.OutputObstacle;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/** Real folders in a temp directory: the input the detector reads and the output it writes. */
public final class Workspace {

    private static final String GOOD_FIXTURE = "good-000.png";
    private static final String DEFECT_FIXTURE = "cut-lead-000.png";

    private final Path root;
    private Path input;
    private Path output;
    private long imagesPlaced;

    Workspace() {
        try {
            root = Files.createTempDirectory("detector-acceptance-");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        input = root.resolve("shift-input-7f3a");
        output = root.resolve("run-output");
    }

    public Path input() {
        return input;
    }

    public Path output() {
        return output;
    }

    /** A real fixture plus a trailer, so that every image has its own bytes and Jev can answer per image. */
    public void place(ImagePath image) {
        Path target = input.resolve(image.value());
        try (InputStream fixture = getClass().getResourceAsStream("/transistor-images/" + fixtureOf(image))) {
            Files.createDirectories(target.getParent());
            byte[] bytes = fixture.readAllBytes();
            byte[] trailer = ByteBuffer.allocate(Long.BYTES).putLong(++imagesPlaced).array();
            Files.write(target, concat(bytes, trailer));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void placeAll(List<ImagePath> images) {
        images.forEach(this::place);
    }

    public void fillWith(FolderContent content) {
        createFolder(input);
        switch (content) {
            case NOTHING -> { }
            case ONLY_UNKNOWN_FOLDERS -> place(new ImagePath("unsorted/017.png"));
            case ONLY_NOTES -> writeInput("notes.txt", "not an image");
        }
    }

    public void writeInput(String fileName, String content) {
        write(input.resolve(fileName), content);
    }

    public void writeOutput(String fileName, String content) {
        write(output.resolve(fileName), content);
    }

    public void folderInOutput(String folderName) {
        createFolder(output.resolve(folderName));
    }

    public void obstruct(OutputObstacle obstacle) {
        switch (obstacle) {
            case READ_ONLY -> {
                assumeThat(System.getProperty("user.name")).as("root ignores folder permissions").isNotEqualTo("root");
                createFolder(output);
                permissions(output, "r-xr-xr-x");
            }
            case IS_A_FILE -> write(output, "in the way");
            case BELOW_A_FILE -> {
                write(root.resolve("a-file"), "in the way");
                output = root.resolve("a-file").resolve("run-output");
            }
        }
    }

    public void useInput(Path folder) {
        input = folder;
    }

    public void useOutput(Path folder) {
        output = folder;
    }

    public Path sibling(String name) {
        return root.resolve(name);
    }

    public void shortcut(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] bytesOf(ImagePath image) {
        try {
            return Files.readAllBytes(input.resolve(image.value()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The one place that decides how a path appears in expected lines: as typed (UC-5, not yet user-confirmed). */
    public String resolve(String text) {
        return text.replace("<input>", input.toString()).replace("<output>", output.toString());
    }

    /** Relative path to content hash, for everything below the input folder. */
    public Map<String, String> inputTree() {
        Map<String, String> tree = new TreeMap<>();
        filesBelow(input).forEach(file -> tree.put(input.relativize(file).toString(), hashOf(file)));
        return tree;
    }

    public List<String> outputTree() {
        return filesBelow(output).stream().map(file -> output.relativize(file).toString()).sorted().toList();
    }

    public boolean outputExists() {
        return Files.exists(output);
    }

    public void remove() {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(Workspace::deleteQuietly);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Path> filesBelow(Path folder) {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(folder)) {
            return paths.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String fixtureOf(ImagePath image) {
        return image.isGood() ? GOOD_FIXTURE : DEFECT_FIXTURE;
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] all = java.util.Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, all, first.length, second.length);
        return all;
    }

    private static void createFolder(Path folder) {
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(Path file, String content) {
        createFolder(file.getParent());
        try {
            Files.writeString(file, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void permissions(Path path, String permissions) {
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permissions));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            if (Files.isDirectory(path) && !Files.isSymbolicLink(path)) {
                permissions(path, "rwxr-xr-x");
            }
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String hashOf(Path file) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
