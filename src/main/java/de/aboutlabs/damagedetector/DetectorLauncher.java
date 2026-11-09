package de.aboutlabs.damagedetector;

import de.aboutlabs.damagedetector.cli.CliArguments;
import de.aboutlabs.damagedetector.cli.ErrorFeedback;
import de.aboutlabs.damagedetector.inspection.ImageFolder;
import de.aboutlabs.damagedetector.inspection.OutputFolder;
import de.aboutlabs.damagedetector.probe.ProbeResult;
import java.nio.file.Path;
import java.util.Optional;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * The whole program without ending the JVM (DDD-30). It owns bootstrap steps 1-5, starts Spring, prints the line of any
 * bootstrap or start-up failure, and returns the exit code. {@code main} is only
 * {@code System.exit(DetectorLauncher.launch(args))} and passes no initializers. The varargs are public on purpose, as a
 * test seam: they replace only the two non-deterministic capabilities (Sleeper, Clock) and leave steps 1-5 under test.
 */
public final class DetectorLauncher {

    private DetectorLauncher() {
    }

    @SafeVarargs
    public static int launch(String[] args, ApplicationContextInitializer<ConfigurableApplicationContext>... capabilities) {
        ErrorFeedback errorFeedback = new ErrorFeedback();
        SpringApplication application;
        try {
            Optional<ProbeResult.Refused> refusal = refusalBeforeStart(args);
            if (refusal.isPresent()) {
                return print(errorFeedback.feedbackFor(refusal.get()));
            }
            OutputFolder output = OutputFolder.rootedAt(Path.of(CliArguments.of(args).output()));
            application = new SpringApplication(DamageDetectorApplication.class);
            application.setDefaultProperties(LogSink.defaultPropertiesFor(output.realRoot()));
            application.addInitializers(capabilities);
        } catch (RuntimeException failure) {
            return print(errorFeedback.feedbackBeforeLog(failure));
        }
        try {
            return SpringApplication.exit(application.run(args));
        } catch (RuntimeException failure) {
            return print(errorFeedback.feedbackFor(failure));
        }
    }

    /** Bootstrap steps 1-4: the first two only inspect, the third and fourth create and probe the output folder. */
    private static Optional<ProbeResult.Refused> refusalBeforeStart(String[] args) {
        ProbeResult result = CliArguments.check(args);
        if (result instanceof ProbeResult.Ok) {
            CliArguments arguments = CliArguments.of(args);
            result = ImageFolder.rootedAt(Path.of(arguments.input())).checkRun(Path.of(arguments.output()));
        }
        if (result instanceof ProbeResult.Ok) {
            result = OutputFolder.rootedAt(Path.of(CliArguments.of(args).output())).probe();
        }
        return result instanceof ProbeResult.Refused refused ? Optional.of(refused) : Optional.empty();
    }

    private static int print(ErrorFeedback.Feedback feedback) {
        System.err.println(feedback.line());
        return feedback.exitCode();
    }
}
