package de.aboutlabs.damagedetector.inspection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

import de.aboutlabs.damagedetector.probe.ProbeResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The write port's root rule cannot be reached through the CLI, because relative paths come from the input walk. */
class OutputFolderTest {

    @TempDir
    Path temp;

    @Test
    void writes_below_its_root_and_creates_the_folders_on_the_way() {
        OutputFolder.rootedAt(temp.resolve("out")).writeString("review/good/000.png.txt", "REVIEW: why");

        assertThat(temp.resolve("out/review/good/000.png.txt")).content().isEqualTo("REVIEW: why");
    }

    @ParameterizedTest
    @ValueSource(strings = {"../escaped.txt", "review/../../escaped.txt", "/tmp/escaped.txt"})
    void rejects_a_path_that_leaves_the_root(String path) {
        OutputFolder output = OutputFolder.rootedAt(temp.resolve("out"));

        assertThatThrownBy(() -> output.writeString(path, "x")).isInstanceOf(IllegalArgumentException.class);
        assertThat(temp.resolve("escaped.txt")).doesNotExist();
    }

    @Test
    void the_probe_creates_the_folder_and_leaves_no_probe_file() {
        Path output = temp.resolve("a/b/out");

        assertThat(OutputFolder.rootedAt(output).probe()).isInstanceOf(ProbeResult.Ok.class);

        assertThat(output).isEmptyDirectory();
    }

    @Test
    void the_probe_refuses_a_read_only_folder() throws IOException {
        assumeNotRoot();
        Path output = Files.createDirectory(temp.resolve("out"));
        Files.setPosixFilePermissions(output, PosixFilePermissions.fromString("r-xr-xr-x"));

        assertThat(OutputFolder.rootedAt(output).probe()).isInstanceOfSatisfying(ProbeResult.Refused.class,
            refused -> assertThat(refused.reason()).isEqualTo(ProbeResult.Reason.OUTPUT_NOT_WRITABLE));

        Files.setPosixFilePermissions(output, PosixFilePermissions.fromString("rwxr-xr-x"));
    }

    @Test
    void the_probe_overwrites_a_leftover_probe_file_from_a_killed_run() throws IOException {
        Path output = Files.createDirectory(temp.resolve("out"));
        Files.writeString(output.resolve(".damage-detector-write-probe"), "");

        assertThat(OutputFolder.rootedAt(output).probe()).isInstanceOf(ProbeResult.Ok.class);

        assertThat(output).isEmptyDirectory();
    }

    @Test
    void writes_bytes_unchanged() {
        byte[] image = {1, 2, 3, (byte) 0xFF};

        OutputFolder.rootedAt(temp.resolve("out")).writeBytes("scrap/good/000.png", image);

        assertThat(temp.resolve("out/scrap/good/000.png")).hasBinaryContent(image);
    }

    @ParameterizedTest
    @ValueSource(strings = {"../escaped.png", "/tmp/escaped.png"})
    void rejects_bytes_for_a_path_that_leaves_the_root(String path) {
        OutputFolder output = OutputFolder.rootedAt(temp.resolve("out"));

        assertThatThrownBy(() -> output.writeBytes(path, new byte[] {1})).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void a_write_leaves_no_temp_file() throws IOException {
        OutputFolder.rootedAt(temp.resolve("out")).writeString("report.md", "done");

        try (Stream<Path> files = Files.list(temp.resolve("out"))) {
            assertThat(files.map(file -> file.getFileName().toString())).containsExactly("report.md");
        }
    }

    @Test
    void a_failed_write_deletes_its_temp_file() throws IOException {
        Path root = Files.createDirectory(temp.resolve("out"));
        Files.createDirectory(root.resolve("report.json"));
        Files.writeString(root.resolve("report.json/keep.txt"), "the rename onto a folder that is not empty fails");

        OutputFolder output = OutputFolder.rootedAt(root);

        assertThatThrownBy(() -> output.writeString("report.json", "new")).isInstanceOf(RuntimeException.class);
        assertThat(root.resolve("report.json.tmp")).doesNotExist();
    }

    @Test
    void a_failed_replacement_leaves_the_old_file_whole() throws IOException {
        assumeNotRoot();
        Path root = Files.createDirectory(temp.resolve("out"));
        Files.writeString(root.resolve("report.md"), "the earlier report");
        Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("r-xr-xr-x"));

        OutputFolder output = OutputFolder.rootedAt(root);

        assertThatThrownBy(() -> output.writeString("report.md", "new")).isInstanceOf(RuntimeException.class);
        assertThat(root.resolve("report.md")).content().isEqualTo("the earlier report");

        Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwxr-xr-x"));
    }

    @Test
    void the_real_root_follows_a_shortcut() throws IOException {
        Path real = Files.createDirectory(temp.resolve("real"));
        Path shortcut = Files.createSymbolicLink(temp.resolve("shortcut"), real);

        assertThat(OutputFolder.rootedAt(shortcut).realRoot()).isEqualTo(real.toRealPath());
    }

    private static void assumeNotRoot() {
        assumeThat(System.getProperty("user.name")).as("root ignores folder permissions").isNotEqualTo("root");
    }
}
