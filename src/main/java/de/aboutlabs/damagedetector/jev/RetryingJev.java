// SCAFFOLD: true
package de.aboutlabs.damagedetector.jev;

import de.aboutlabs.damagedetector.domain.JevOutcome;
import de.aboutlabs.damagedetector.probe.Probe;
import de.aboutlabs.damagedetector.probe.ProbeResult;
import java.time.Duration;

/**
 * Retry decorator (ADR-004). DELIVER makes this the only implementation of the Jev port, once the port returns
 * JevOutcome; {@code implements Jev} cannot be declared before that.
 */
public class RetryingJev implements Probe {

    public RetryingJev(AttemptSource attemptSource, int maxAttempts, Duration initialBackoff, Duration maxBackoff, Sleeper sleeper) {
    }

    public JevOutcome judge(byte[] image, String mediaType) {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    @Override
    public ProbeResult probe() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }
}
