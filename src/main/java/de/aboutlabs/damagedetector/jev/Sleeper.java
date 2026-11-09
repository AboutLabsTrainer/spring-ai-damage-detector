package de.aboutlabs.damagedetector.jev;

import java.time.Duration;

/** Waiting between attempts, as an injected capability so tests need no real delay. */
@FunctionalInterface
public interface Sleeper {

    void sleep(Duration wait);

    static Sleeper realTime() {
        return wait -> {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for the next Jev attempt", e);
            }
        };
    }
}
