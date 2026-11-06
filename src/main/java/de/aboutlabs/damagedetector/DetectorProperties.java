package de.aboutlabs.damagedetector;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param passThreshold minimum P(GOOD) for an automatic PASS; 0.86 was fitted on the spike sample and is optimistic
 * @param jev           connection to Jev via the OpenRouter Decisions API
 */
@ConfigurationProperties("detector")
public record DetectorProperties(@DefaultValue("0.86") double passThreshold, @DefaultValue Jev jev) {

    public record Jev(
        @DefaultValue("https://openrouter.ai") String baseUrl,
        @DefaultValue("typesafe/jev-1.13") String model,
        String apiKey,
        @DefaultValue("10s") Duration connectTimeout,
        @DefaultValue("30s") Duration readTimeout) {

        @Override
        public String toString() {
            return "Jev[baseUrl=" + baseUrl + ", model=" + model + ", apiKey=***]";
        }
    }
}
