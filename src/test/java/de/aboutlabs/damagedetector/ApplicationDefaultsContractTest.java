package de.aboutlabs.damagedetector;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

/**
 * ADR-005 decision 6: application.yml holds defaults only. A 30 s wait cannot be observed cheaply, so it is read here.
 * The key-and-log guard holds today and runs in every verify; only the defaults that do not exist yet are pending.
 */
class ApplicationDefaultsContractTest {

    private final Properties defaults = defaults();

    @Disabled("pending: slice 03, application.yml retry and time-limit defaults (ADR-004, UC-4)")
    @Test
    void the_jev_time_limits_and_the_retry_defaults_are_the_agreed_ones() {
        assertThat(defaults)
            .containsEntry("detector.pass-threshold", 0.86)
            .containsEntry("detector.jev.connect-timeout", "10s")
            .containsEntry("detector.jev.read-timeout", "30s")
            .containsEntry("detector.jev.retry.max-attempts", 3)
            .containsEntry("detector.jev.retry.initial-backoff", "1s")
            .containsEntry("detector.jev.retry.max-backoff", "10s");
    }

    @Test
    void the_key_is_only_a_placeholder_and_there_is_no_log_path() {
        assertThat(defaults).containsEntry("detector.jev.api-key", "${OPENROUTER_API_KEY:}");
        assertThat(defaults).doesNotContainKeys("logging.file.name", "logging.file.path");
    }

    private static Properties defaults() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        return yaml.getObject();
    }
}
