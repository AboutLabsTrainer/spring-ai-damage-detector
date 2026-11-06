package de.aboutlabs.damagedetector.acceptance.support;

import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.Interruption;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** The @driving_adapter driver (policy: subprocess of the built jar): real arguments, streams and exit code. */
final class RealProcessDriver implements DetectorDriver {

    private static final String DEFAULT_JAR = "target/damage-detector-0.1.0-SNAPSHOT.jar";
    private static final long TIME_LIMIT_SECONDS = 60;

    private final Workspace workspace;

    RealProcessDriver(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public RunResult run(List<String> arguments, Map<String, String> environment) {
        return run(arguments, environment, process -> { });
    }

    /** Sends the signal once the run has reached the given point. The exit code then comes from the JVM. */
    RunResult runInterrupted(List<String> arguments, Map<String, String> environment, Runnable reached, Interruption interruption) {
        return run(arguments, environment, process -> {
            reached.run();
            signal(process, interruption);
        });
    }

    private RunResult run(List<String> arguments, Map<String, String> environment, Consumer<Process> whileRunning) {
        Path stdout = workspace.sibling("process.stdout");
        Path stderr = workspace.sibling("process.stderr");
        List<String> command = new ArrayList<>(List.of(javaExecutable(), "-jar", System.getProperty("detector.jar", DEFAULT_JAR)));
        command.addAll(arguments);
        ProcessBuilder builder = new ProcessBuilder(command).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
        builder.environment().remove("OPENROUTER_API_KEY");
        builder.environment().remove("JAVA_TOOL_OPTIONS");
        builder.environment().putAll(environment);
        try {
            Process process = builder.start();
            whileRunning.accept(process);
            if (!process.waitFor(TIME_LIMIT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new AssertionError("The detector did not finish within " + TIME_LIMIT_SECONDS + " s");
            }
            return new RunResult(process.exitValue(), Files.readString(stdout), Files.readString(stderr));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void signal(Process process, Interruption interruption) {
        try {
            new ProcessBuilder("kill", "-" + interruption.signal(), String.valueOf(process.pid())).start().waitFor();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static String javaExecutable() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }
}
