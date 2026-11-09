package de.aboutlabs.damagedetector.jev;

import de.aboutlabs.damagedetector.domain.JevJudgement;
import de.aboutlabs.damagedetector.probe.ProbeResult;
import java.net.SocketTimeoutException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Driven adapter: one attempt at Jev through the OpenRouter Decisions API (alpha). Sends the image as a data URL,
 * never its name. Every way an attempt can end is classified here; the retry policy lives in RetryingJev (ADR-004).
 */
public class OpenRouterJev implements AttemptSource {

    private static final String DECISIONS_PATH = "/api/alpha/decisions";
    private static final String UNREADABLE = "unreadable answer";

    private final RestClient.Builder restClientBuilder;
    private final String baseUrl;
    private final String model;
    private final String apiKey;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private RestClient restClient;

    /** Plain values only: nothing is validated or built here, the client is built at the first attempt (DDD-35). */
    public OpenRouterJev(RestClient.Builder restClientBuilder, String baseUrl, String model, String apiKey,
                         Duration connectTimeout, Duration readTimeout) {
        this.restClientBuilder = restClientBuilder.requestFactory(timedRequestFactory(connectTimeout, readTimeout));
        this.baseUrl = baseUrl;
        this.model = model;
        this.apiKey = apiKey;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    private static ClientHttpRequestFactory timedRequestFactory(Duration connectTimeout, Duration readTimeout) {
        HttpClientSettings limits = HttpClientSettings.defaults()
            .withConnectTimeout(connectTimeout)
            .withReadTimeout(readTimeout);
        return ClientHttpRequestFactoryBuilder.jdk().build(limits);
    }

    @Override
    public AttemptResult attempt(byte[] image, String mediaType) {
        long startedAt = System.nanoTime();
        try {
            return toResult(post(image, mediaType));
        } catch (RestClientResponseException e) {
            return failedWithStatus(e.getStatusCode().value(), e.getStatusText(), retryAfterOf(e));
        } catch (ResourceAccessException e) {
            return failedToConnectOrTimedOut(e);
        } catch (RestClientException | HttpMessageNotReadableException e) {
            return stalledOrUnreadable(Duration.ofNanos(System.nanoTime() - startedAt));
        }
    }

    // SCAFFOLD: true -- step 05-02
    @Override
    public ProbeResult probe() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    private synchronized RestClient client() {
        if (restClient == null) {
            restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + (apiKey == null ? "" : apiKey))
                .build();
        }
        return restClient;
    }

    private DecisionResponse post(byte[] image, String mediaType) {
        return client().post()
            .uri(DECISIONS_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestFor(image, mediaType))
            .retrieve()
            .body(DecisionResponse.class);
    }

    private static AttemptResult failedWithStatus(int status, String statusText, Optional<Duration> retryAfter) {
        HttpStatus known = HttpStatus.resolve(status);
        String reason = "HTTP " + status + " " + (known == null ? statusText : known.getReasonPhrase());
        boolean transientStatus = status == 408 || status == 429 || status >= 500;
        return transientStatus ? new AttemptResult.Transient(reason, retryAfter) : new AttemptResult.Permanent(reason);
    }

    /** Only a whole, non-negative number of seconds is usable; an HTTP date or anything else is ignored (DDD-7). */
    private static Optional<Duration> retryAfterOf(RestClientResponseException e) {
        String value = e.getResponseHeaders() == null ? null : e.getResponseHeaders().getFirst("Retry-After");
        if (value == null || !value.matches("\\d{1,9}")) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofSeconds(Long.parseLong(value)));
    }

    private AttemptResult failedToConnectOrTimedOut(ResourceAccessException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpConnectTimeoutException) {
                return timedOut(connectTimeout);
            }
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
                return timedOut(readTimeout);
            }
        }
        return new AttemptResult.Transient("connection error", Optional.empty());
    }

    /** The read limit acts as a whole-response deadline: a Jev that stalls after its headers surfaces as an unreadable body. */
    private AttemptResult stalledOrUnreadable(Duration elapsed) {
        return elapsed.compareTo(readTimeout) >= 0 ? timedOut(readTimeout) : new AttemptResult.Transient(UNREADABLE, Optional.empty());
    }

    private static AttemptResult timedOut(Duration limit) {
        return new AttemptResult.Transient("timeout after " + limit.toSeconds() + " s", Optional.empty());
    }

    private DecisionRequest requestFor(byte[] image, String mediaType) {
        String dataUrl = "data:" + mediaType + ";base64," + Base64.getEncoder().encodeToString(image);
        return new DecisionRequest(
            model,
            List.of(DefectQuestion.SCENE, new ImageUrlPart("image_url", new ImageUrl(dataUrl))),
            Map.of(DefectQuestion.KEY, new ChoiceQuestion("choice", DefectQuestion.INSTRUCTIONS, DefectQuestion.CRITERIA)));
    }

    private static AttemptResult toResult(DecisionResponse response) {
        Answer answer = response == null || response.answers() == null ? null : response.answers().get(DefectQuestion.KEY);
        if (answer == null || !DefectQuestion.CRITERIA.containsKey(answer.choice()) || !isProbability(pGoodOf(answer))) {
            return new AttemptResult.Transient(UNREADABLE, Optional.empty());
        }
        double cost = response.usage() == null || response.usage().cost() == null ? 0.0 : response.usage().cost();
        double confidence = answer.confidence() == null ? 0.0 : answer.confidence();
        return new AttemptResult.Answered(new JevJudgement(answer.choice(), knownProbabilities(answer), confidence, cost));
    }

    private static Double pGoodOf(Answer answer) {
        return answer.probabilities() == null ? null : answer.probabilities().get(JevJudgement.GOOD);
    }

    private static boolean isProbability(Double value) {
        return value != null && Double.isFinite(value) && value >= 0.0 && value <= 1.0;
    }

    private static Map<String, Double> knownProbabilities(Answer answer) {
        Map<String, Double> known = new LinkedHashMap<>();
        answer.probabilities().forEach((defectClass, probability) -> {
            if (probability != null) {
                known.put(defectClass, probability);
            }
        });
        return known;
    }

    record DecisionRequest(String model, List<Object> state, Map<String, ChoiceQuestion> questions) {
    }

    record ImageUrlPart(String type, ImageUrl image_url) {
    }

    record ImageUrl(String url) {
    }

    record ChoiceQuestion(String type, String instructions, Map<String, String> criteria) {
    }

    record DecisionResponse(Map<String, Answer> answers, Usage usage) {
    }

    record Answer(String choice, Map<String, Double> probabilities, Double confidence) {
    }

    record Usage(Double cost) {
    }
}
