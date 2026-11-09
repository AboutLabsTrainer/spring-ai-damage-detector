package de.aboutlabs.damagedetector.jev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** CONTRACT_SHAPE: external-effect */
class SleeperTest {

    @Test
    void waits_at_least_the_requested_time() {
        long start = System.nanoTime();

        Sleeper.realTime().sleep(Duration.ofMillis(30));

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofMillis(25));
    }

    @Test
    void a_zero_wait_returns_at_once() {
        long start = System.nanoTime();

        Sleeper.realTime().sleep(Duration.ZERO);
        Sleeper.realTime().sleep(Duration.ofMillis(-5));

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(100));
    }

    @Test
    void an_interrupted_wait_fails_and_keeps_the_interrupt_flag() {
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> Sleeper.realTime().sleep(Duration.ofSeconds(5)))
                .isInstanceOf(IllegalStateException.class).hasMessage("Interrupted while waiting for the next Jev attempt")
                .hasCauseInstanceOf(InterruptedException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }
}
