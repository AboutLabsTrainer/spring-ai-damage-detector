package de.aboutlabs.damagedetector.jev;

import de.aboutlabs.damagedetector.domain.Jev;
import de.aboutlabs.damagedetector.domain.JevOutcome;
import de.aboutlabs.damagedetector.probe.Probe;
import de.aboutlabs.damagedetector.probe.ProbeResult;
import java.time.Duration;

/**
 * Retry decorator and the only implementation of the Jev port (ADR-004). Transient failures are tried again up to
 * the attempt limit with a doubling wait; a permanent failure ends the image at once. A failure never escapes.
 */
public class RetryingJev implements Jev, Probe {

    private final AttemptSource attemptSource;
    private final int maxAttempts;
    private final Duration initialBackoff;
    private final Duration maxBackoff;
    private final Sleeper sleeper;

    RetryingJev(AttemptSource attemptSource, int maxAttempts, Duration initialBackoff, Duration maxBackoff, Sleeper sleeper) {
        this.attemptSource = attemptSource;
        this.maxAttempts = maxAttempts;
        this.initialBackoff = initialBackoff;
        this.maxBackoff = maxBackoff;
        this.sleeper = sleeper;
    }

    public static RetryingJev around(OpenRouterJev adapter, int maxAttempts, Duration initialBackoff, Duration maxBackoff,
                                     Sleeper sleeper) {
        return new RetryingJev(adapter, maxAttempts, initialBackoff, maxBackoff, sleeper);
    }

    @Override
    public JevOutcome judge(byte[] image, String mediaType) {
        for (int attempt = 1; ; attempt++) {
            AttemptResult result = attemptSource.attempt(image, mediaType);
            if (result instanceof AttemptResult.Answered answered) {
                return new JevOutcome.Judged(answered.judgement(), attempt);
            }
            if (result instanceof AttemptResult.Transient transientFailure && attempt < maxAttempts) {
                sleeper.sleep(backoffAfter(attempt));
                continue;
            }
            return new JevOutcome.Failed(attempt, reasonOf(result));
        }
    }

    private static String reasonOf(AttemptResult result) {
        return switch (result) {
            case AttemptResult.Transient transientFailure -> transientFailure.reason();
            case AttemptResult.Permanent permanent -> permanent.reason();
            case AttemptResult.Answered answered -> throw new IllegalStateException("An answer is not a failure");
        };
    }

    private Duration backoffAfter(int failedAttempts) {
        Duration wait = initialBackoff;
        for (int doubling = 1; doubling < failedAttempts && wait.compareTo(maxBackoff) < 0; doubling++) {
            wait = wait.multipliedBy(2);
        }
        return wait.compareTo(maxBackoff) > 0 ? maxBackoff : wait;
    }

    // SCAFFOLD: true -- step 05-02
    @Override
    public ProbeResult probe() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }
}
