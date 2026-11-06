// SCAFFOLD: true
package de.aboutlabs.damagedetector;

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
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }
}
