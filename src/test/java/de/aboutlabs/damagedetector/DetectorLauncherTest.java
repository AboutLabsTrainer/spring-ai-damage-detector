package de.aboutlabs.damagedetector;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

class DetectorLauncherTest {

    @TempDir
    Path workspace;

    @Test
    void an_unexpected_failure_inside_the_run_points_to_the_log() throws IOException {
        Path input = Files.createDirectory(workspace.resolve("in"));
        Files.write(input.resolve("a.png"), new byte[] {(byte) 0x89, 'P', 'N', 'G'});
        ApplicationContextInitializer<ConfigurableApplicationContext> failing = context -> {
            throw new IllegalStateException("boom");
        };
        PrintStream originalErr = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        int exitCode;
        try {
            System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
            exitCode = DetectorLauncher.launch(
                new String[] {"--input=" + input, "--output=" + workspace.resolve("out")}, failing);
        } finally {
            System.setErr(originalErr);
        }

        assertThat(captured.toString(StandardCharsets.UTF_8).strip())
            .isEqualTo("Inspection failed: boom. See log for details.");
        assertThat(exitCode).isEqualTo(1);
    }
}
