// SCAFFOLD: true
package de.aboutlabs.damagedetector.jev;

import java.time.Duration;

/** Waiting between attempts, as an injected capability so tests need no real delay. */
@FunctionalInterface
public interface Sleeper {

    void sleep(Duration wait);
}
