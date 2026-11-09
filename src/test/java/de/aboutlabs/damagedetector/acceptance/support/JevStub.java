package de.aboutlabs.damagedetector.acceptance.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.GroundTruth;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ImagePath;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.JevClass;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.JevError;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.UnreadableAnswer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

/** Jev as WireMock on a real port: answers per image (by its bytes), failures, and the journal of what was asked. */
public final class JevStub {

    private static final String DECISIONS_PATH = "/api/alpha/decisions";
    private static final String IMAGE_URL = "$.state[1].image_url.url";
    private static final double DEFAULT_DEFECT_P_GOOD = 0.12;
    private static final double DEFAULT_DEFECT_P_CHOICE = 0.53;
    private static final int ANSWER_DELAY_BEYOND_THE_LIMIT_MS = 1500;
    private static final int ANSWER_DELAY_DURING_AN_INTERRUPT_MS = 6000;
    private static final long AWAIT_SECONDS = 30;
    private static final String ANSWER = """
        {"model": "typesafe/jev-1.13-20260917",
         "answers": {"defect": {"type": "choice", "choice": "%s", "probabilities": %s, "confidence": 0.41}},
         "usage": {"input_tokens": 3505, "output_tokens": 65, "cost": %s},
         "provider": "TypeSafe"}
        """;

    private final WireMockServer server = new WireMockServer(options().dynamicPort().http2PlainDisabled(true));
    private final Workspace workspace;
    private final List<ImagePath> imagesKnown = new ArrayList<>();

    JevStub(Workspace workspace) {
        this.workspace = workspace;
    }

    void start() {
        server.start();
    }

    void stop() {
        if (server.isRunning()) {
            server.stop();
        }
    }

    public String baseUrl() {
        return server.isRunning() ? server.baseUrl() : "http://localhost:" + 1;
    }

    public void judges(ImagePath image, JevClass choice, double pGood) {
        server.stubFor(forImage(image).willReturn(judgement(image, choice, pGood)));
    }

    public void judges(ImagePath image, JevClass choice) {
        judges(image, choice, DEFAULT_DEFECT_P_GOOD);
    }

    public void failsOnEveryAttempt(ImagePath image, JevError error) {
        server.stubFor(forImage(image).willReturn(errorReply(error)));
    }

    public void failsOnceThenJudges(ImagePath image, JevError error, JevClass choice) {
        replies(image, errorReply(error), judgement(image, choice, DEFAULT_DEFECT_P_GOOD));
    }

    public void rejectsEveryCall(JevError error) {
        server.stubFor(post(urlEqualTo(DECISIONS_PATH)).willReturn(errorReply(error)));
    }

    public void asksToWaitThenJudges(ImagePath image, int seconds, JevError error, JevClass choice) {
        replies(image, waitRequest(seconds, error), judgement(image, choice, DEFAULT_DEFECT_P_GOOD));
    }

    public void asksToWaitOnEveryAttempt(ImagePath image, int seconds, JevError error) {
        server.stubFor(forImage(image).willReturn(waitRequest(seconds, error)));
    }

    public void answersTooLate(ImagePath image) {
        server.stubFor(forImage(image).willReturn(judgement(image, JevClass.GOOD, 0.99)
            .withFixedDelay(ANSWER_DELAY_BEYOND_THE_LIMIT_MS)));
    }

    public void keepsThinkingAbout(ImagePath image) {
        server.stubFor(forImage(image).willReturn(judgement(image, JevClass.GOOD, 0.99).withFixedDelay(ANSWER_DELAY_DURING_AN_INTERRUPT_MS)));
    }

    public void breaksTheConnection(ImagePath image) {
        server.stubFor(forImage(image).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }

    public void answersUnreadably(ImagePath image, UnreadableAnswer answer) {
        server.stubFor(forImage(image).willReturn(answer.reply()));
    }

    public void answersUnreadablyOnceThenJudges(ImagePath image, UnreadableAnswer answer, JevClass choice) {
        replies(image, answer.reply(), judgement(image, choice, DEFAULT_DEFECT_P_GOOD));
    }

    public void cannotBeReached() {
        server.stop();
    }

    public void wasAskedAbout(ImagePath image, int times) {
        assertThat(server.findAll(requestFor(image))).as("requests about %s", image.value()).hasSize(times);
    }

    public void wasCalled(int times) {
        assertThat(server.isRunning()).as("the Jev stub must be up, or a count of 0 proves nothing").isTrue();
        assertThat(requests()).as("Jev calls").hasSize(times);
    }

    /** Blocks until the detector has asked about this image, so an interrupt lands in the middle of the run. */
    public void awaitAskedAbout(ImagePath image) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(AWAIT_SECONDS);
        while (server.findAll(requestFor(image)).isEmpty()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("The detector never asked Jev about " + image.value());
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
        }
    }

    /** The precise C11 assertion of UC-3: the fixed class vocabulary is allowed, per-image names and paths are not. */
    public void carriedNoGroundTruth() {
        String inputName = workspace.input().getFileName().toString().toLowerCase(Locale.ROOT);
        List<String> forbidden = new ArrayList<>(List.of(inputName));
        GroundTruth.folderNames().forEach(label -> forbidden.add(label + "/"));
        imagesKnown.forEach(image -> {
            forbidden.add(image.fileName().toLowerCase(Locale.ROOT));
            forbidden.add(image.value().toLowerCase(Locale.ROOT));
        });
        assertThat(requests()).isNotEmpty();
        for (LoggedRequest request : requests()) {
            String sent = (request.getUrl() + withoutImageData(request)).toLowerCase(Locale.ROOT);
            assertThat(sent).doesNotContain(forbidden);
        }
    }

    public void sentIdenticalRequestsApartFromTheImage() {
        Set<String> bodies = requests().stream().map(JevStub::withoutImageData).collect(Collectors.toSet());
        assertThat(bodies).hasSize(1);
    }

    private void replies(ImagePath image, ResponseDefinitionBuilder first, ResponseDefinitionBuilder then) {
        String sequence = "attempts for " + image.value();
        server.stubFor(forImage(image).inScenario(sequence).whenScenarioStateIs(Scenario.STARTED)
            .willSetStateTo("answered once").willReturn(first));
        server.stubFor(forImage(image).inScenario(sequence).whenScenarioStateIs("answered once").willReturn(then));
    }

    private MappingBuilder forImage(ImagePath image) {
        imagesKnown.add(image);
        return post(urlEqualTo(DECISIONS_PATH)).withRequestBody(matchingJsonPath(IMAGE_URL, equalTo(dataUrlOf(image))));
    }

    private RequestPatternBuilder requestFor(ImagePath image) {
        return postRequestedFor(urlEqualTo(DECISIONS_PATH))
            .withRequestBody(matchingJsonPath(IMAGE_URL, equalTo(dataUrlOf(image))));
    }

    private List<LoggedRequest> requests() {
        return server.findAll(postRequestedFor(urlEqualTo(DECISIONS_PATH)));
    }

    private String dataUrlOf(ImagePath image) {
        return "data:" + image.mediaType() + ";base64," + Base64.getEncoder().encodeToString(workspace.bytesOf(image));
    }

    /** The image is base64, whose alphabet contains '/', so it must not take part in the name checks. */
    private static String withoutImageData(LoggedRequest request) {
        return request.getBodyAsString().replaceAll("\"data:[^\"]*\"", "\"<image>\"");
    }

    private static ResponseDefinitionBuilder judgement(ImagePath image, JevClass choice, double pGood) {
        double cost = choice == JevClass.GOOD ? 0.0001 : 0.0002;
        String probabilities = choice == JevClass.GOOD
            ? "{\"GOOD\": %s, \"MISPLACED\": 0.03, \"DAMAGED_CASE\": 0.03, \"CUT_LEAD\": 0.02, \"BENT_LEAD\": 0.01}".formatted(pGood)
            : "{\"GOOD\": %s, \"%s\": %s}".formatted(pGood, choice, DEFAULT_DEFECT_P_CHOICE);
        return aResponse().withHeader("Content-Type", "application/json")
            .withBody(ANSWER.formatted(choice, probabilities, cost));
    }

    private static ResponseDefinitionBuilder errorReply(JevError error) {
        return aResponse().withStatus(error.status()).withStatusMessage(error.reasonPhrase());
    }

    private static ResponseDefinitionBuilder waitRequest(int seconds, JevError error) {
        return errorReply(error).withHeader("Retry-After", String.valueOf(seconds));
    }
}
