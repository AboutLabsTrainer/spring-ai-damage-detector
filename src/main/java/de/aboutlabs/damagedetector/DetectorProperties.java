package de.aboutlabs.damagedetector;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param passThreshold raw text, so that a mistyped value is a refusal and not a binding failure; minimum P(GOOD) for an automatic PASS; 0.87 is one step of margin over the lowest safe value 0.86 found on the 253-image sweep
 * @param jev           connection to Jev via the OpenRouter Decisions API
 */
@ConfigurationProperties("detector")
public record DetectorProperties(@DefaultValue("0.87") String passThreshold, @DefaultValue Jev jev) {

    public record Jev(
        @DefaultValue("https://openrouter.ai") String baseUrl,
        @DefaultValue("typesafe/jev-1.13") String model,
        String apiKey,
        @DefaultValue("10s") Duration connectTimeout,
        @DefaultValue("30s") Duration readTimeout,
        @DefaultValue Retry retry) {

        @Override
        public String toString() {
            return "Jev[baseUrl=" + baseUrl + ", model=" + model + ", apiKey=***]";
        }
    }

    /** Retry policy of one Jev call: attempts per image and the wait between them, which doubles up to the maximum. */
    public record Retry(
        @DefaultValue("3") int maxAttempts,
        @DefaultValue("1s") Duration initialBackoff,
        @DefaultValue("10s") Duration maxBackoff) {
    }
}
