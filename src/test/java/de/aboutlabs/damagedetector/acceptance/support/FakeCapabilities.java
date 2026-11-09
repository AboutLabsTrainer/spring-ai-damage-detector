package de.aboutlabs.damagedetector.acceptance.support;

import static org.assertj.core.api.Assertions.assertThat;

import de.aboutlabs.damagedetector.jev.Sleeper;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/** The non-deterministic capabilities of an in-process run: the wait between attempts and the run date. */
public final class FakeCapabilities {

    private final List<Duration> waits = new ArrayList<>();
    private final Sleeper sleeper = waits::add;
    private Clock clock = Clock.systemUTC();

    public void today(LocalDate date) {
        clock = Clock.fixed(date.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    public void waitedSeconds(List<Long> seconds) {
        assertThat(waits.stream().map(Duration::toSeconds).toList()).as("waits between attempts").isEqualTo(seconds);
    }

    public void neverWaited() {
        assertThat(waits).as("waits between attempts").isEmpty();
    }

    ApplicationContextInitializer<ConfigurableApplicationContext> initializer() {
        return context -> {
            GenericApplicationContext beans = (GenericApplicationContext) context;
            beans.registerBean("recordingSleeper", Sleeper.class, () -> sleeper, bean -> bean.setPrimary(true));
            beans.registerBean("scenarioClock", Clock.class, () -> clock, bean -> bean.setPrimary(true));
        };
    }
}
