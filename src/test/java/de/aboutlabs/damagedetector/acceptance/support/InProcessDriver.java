package de.aboutlabs.damagedetector.acceptance.support;

import de.aboutlabs.damagedetector.DetectorLauncher;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** The default driver (policy: Spring Boot app in-process): the production entry point with console capture. */
final class InProcessDriver implements DetectorDriver {

    private final FakeCapabilities capabilities;

    InProcessDriver(FakeCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public RunResult run(List<String> arguments, Map<String, String> environment) {
        PrintStream stdout = System.out;
        PrintStream stderr = System.err;
        ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
        ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(capturedErr, true, StandardCharsets.UTF_8));
            int exitCode = DetectorLauncher.launch(arguments.toArray(String[]::new), capabilities.initializer());
            return new RunResult(exitCode, capturedOut.toString(StandardCharsets.UTF_8), capturedErr.toString(StandardCharsets.UTF_8));
        } finally {
            System.setOut(stdout);
            System.setErr(stderr);
        }
    }
}
