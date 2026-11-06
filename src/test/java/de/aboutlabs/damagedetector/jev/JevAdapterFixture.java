package de.aboutlabs.damagedetector.jev;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * The real adapters on a MockRestServiceServer (policy: direct adapter tests, no port). Tests enter through RetryingJev,
 * the Jev port. The mock is bound after OpenRouterJev is constructed, so it is not replaced by the adapter's timed
 * request factory (DDD-35). This is the only place that builds OpenRouterJev.
 */
final class JevAdapterFixture {

    static final String BASE_URL = "http://jev.test";
    static final String DECISIONS_URL = BASE_URL + "/api/alpha/decisions";
    static final String API_KEY = "sk-or-canary-adapter-test";
    static final byte[] IMAGE = {1, 2, 3, 4};
    static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);
    static final Duration MAX_BACKOFF = Duration.ofSeconds(10);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    final List<Duration> waits = new ArrayList<>();
    final MockRestServiceServer jev;
    private final OpenRouterJev attemptSource;

    JevAdapterFixture() {
        this(API_KEY, BASE_URL);
    }

    JevAdapterFixture(String apiKey, String baseUrl) {
        RestClient.Builder builder = RestClient.builder();
        attemptSource = new OpenRouterJev(builder, baseUrl, "typesafe/jev-1.13", apiKey, Duration.ofSeconds(10), READ_TIMEOUT);
        jev = MockRestServiceServer.bindTo(builder).build();
    }

    RetryingJev retrying(int maxAttempts) {
        return new RetryingJev(attemptSource, maxAttempts, INITIAL_BACKOFF, MAX_BACKOFF, waits::add);
    }

    static String answer(String choice, double pGood) {
        return """
            {"answers": {"defect": {"type": "choice", "choice": "%s", "probabilities": {"GOOD": %s}, "confidence": 0.41}},
             "usage": {"cost": 0.00015}}
            """.formatted(choice, pGood);
    }
}
