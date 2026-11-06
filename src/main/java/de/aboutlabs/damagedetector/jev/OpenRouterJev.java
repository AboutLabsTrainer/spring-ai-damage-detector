package de.aboutlabs.damagedetector.jev;

import de.aboutlabs.damagedetector.DetectorProperties;
import de.aboutlabs.damagedetector.domain.Jev;
import de.aboutlabs.damagedetector.domain.JevJudgement;
import de.aboutlabs.damagedetector.probe.ProbeResult;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** Driven adapter: Jev through the OpenRouter Decisions API (alpha). Sends the image as a data URL, never its name. */
public class OpenRouterJev implements Jev, AttemptSource {

    private static final String DECISIONS_PATH = "/api/alpha/decisions";

    private final RestClient restClient;
    private final String model;

    public OpenRouterJev(RestClient.Builder restClientBuilder, DetectorProperties.Jev properties) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("No Jev API key: set the OPENROUTER_API_KEY environment variable");
        }
        this.restClient = restClientBuilder
            .baseUrl(properties.baseUrl())
            .defaultHeader("Authorization", "Bearer " + properties.apiKey())
            .build();
        this.model = properties.model();
    }

    // SCAFFOLD: true -- DDD-35: plain values, nothing validated or built here. DELIVER drops the constructor above and judge().
    public OpenRouterJev(RestClient.Builder restClientBuilder, String baseUrl, String model, String apiKey,
                         Duration connectTimeout, Duration readTimeout) {
        this.restClient = null;
        this.model = model;
    }

    // SCAFFOLD: true
    @Override
    public AttemptResult attempt(byte[] image, String mediaType) {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    // SCAFFOLD: true
    @Override
    public ProbeResult probe() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    @Override
    public JevJudgement judge(byte[] image, String mediaType) {
        DecisionResponse response = restClient.post()
            .uri(DECISIONS_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestFor(image, mediaType))
            .retrieve()
            .body(DecisionResponse.class);
        return toJudgement(response);
    }

    private DecisionRequest requestFor(byte[] image, String mediaType) {
        String dataUrl = "data:" + mediaType + ";base64," + Base64.getEncoder().encodeToString(image);
        return new DecisionRequest(
            model,
            List.of(DefectQuestion.SCENE, new ImageUrlPart("image_url", new ImageUrl(dataUrl))),
            Map.of(DefectQuestion.KEY, new ChoiceQuestion("choice", DefectQuestion.INSTRUCTIONS, DefectQuestion.CRITERIA)));
    }

    private static JevJudgement toJudgement(DecisionResponse response) {
        if (response == null || response.answers() == null || response.answers().get(DefectQuestion.KEY) == null) {
            throw new IllegalStateException("Jev returned no answer for question '" + DefectQuestion.KEY + "'");
        }
        Answer answer = response.answers().get(DefectQuestion.KEY);
        double cost = response.usage() == null || response.usage().cost() == null ? 0.0 : response.usage().cost();
        double confidence = answer.confidence() == null ? 0.0 : answer.confidence();
        return new JevJudgement(answer.choice(), answer.probabilities(), confidence, cost);
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
