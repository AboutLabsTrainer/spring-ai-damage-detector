package de.aboutlabs.damagedetector.acceptance;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import de.aboutlabs.damagedetector.DamageDetectorApplication;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class WalkingSkeletonSteps {

    private static final String GOOD_IMAGE = "good/000.png";
    private static final String CUT_LEAD_IMAGE = "cut_lead/000.png";
    private static final String DECISIONS_PATH = "/api/alpha/decisions";

    private final ObjectMapper json = new ObjectMapper();
    private WireMockServer jev;
    private Path workDir;
    private Path inputDir;
    private Path outputDir;

    @Before
    public void startJevStub() throws IOException {
        jev = new WireMockServer(options().dynamicPort().http2PlainDisabled(true));
        jev.start();
        workDir = Files.createTempDirectory("walking-skeleton-");
        inputDir = workDir.resolve("input");
        outputDir = workDir.resolve("output");
    }

    @After
    public void stopJevStub() {
        jev.stop();
    }

    @Given("an input folder with one good-looking transistor image and one transistor image with a cut lead")
    public void anInputFolderWithTwoImages() throws IOException {
        copyFixture("good-000.png", inputDir.resolve(GOOD_IMAGE));
        copyFixture("cut-lead-000.png", inputDir.resolve(CUT_LEAD_IMAGE));
    }

    @And("Jev judges the good-looking image GOOD with P\\(GOOD) {double}")
    public void jevJudgesGood(double pGood) throws IOException {
        stubJev(GOOD_IMAGE, "GOOD", pGood, pGood, 0.0001);
    }

    @And("Jev judges the cut-lead image CUT_LEAD with probability {double} and P\\(GOOD) {double}")
    public void jevJudgesCutLead(double pCutLead, double pGood) throws IOException {
        stubJev(CUT_LEAD_IMAGE, "CUT_LEAD", pGood, pCutLead, 0.0002);
    }

    @When("the inspector runs the batch inspection on the input folder")
    public void theInspectorRunsTheBatch() {
        String[] args = {
            "--input=" + inputDir,
            "--output=" + outputDir,
            "--detector.jev.base-url=" + jev.baseUrl(),
            "--detector.jev.api-key=test-key"
        };
        try (ConfigurableApplicationContext ignored = SpringApplication.run(DamageDetectorApplication.class, args)) {
            // the batch runs as an ApplicationRunner during startup
        }
    }

    @Then("the report shows {int} PASS, {int} SCRAP and {int} REVIEW")
    public void theReportShows(int pass, int scrap, int review) throws IOException {
        JsonNode summary = json.readTree(outputDir.resolve("report.json").toFile()).get("summary");
        assertThat(summary.get("pass").asInt()).isEqualTo(pass);
        assertThat(summary.get("scrap").asInt()).isEqualTo(scrap);
        assertThat(summary.get("review").asInt()).isEqualTo(review);

        String markdown = Files.readString(outputDir.resolve("report.md"));
        assertThat(markdown)
            .contains("| PASS | " + pass + " |")
            .contains("| SCRAP | " + scrap + " |")
            .contains("| REVIEW | " + review + " |");
    }

    @And("the cut-lead image is copied to the scrap folder with the description {string}")
    public void theScrapImageIsDocumented(String description) throws IOException {
        Path copy = outputDir.resolve("scrap").resolve(CUT_LEAD_IMAGE);
        assertThat(copy).hasSameBinaryContentAs(inputDir.resolve(CUT_LEAD_IMAGE));
        assertThat(Path.of(copy + ".txt")).content().contains(description);
        assertThat(Files.readString(outputDir.resolve("report.md"))).contains(CUT_LEAD_IMAGE).contains(description);
        assertThat(outputDir.resolve("scrap").resolve(GOOD_IMAGE)).doesNotExist();
    }

    @And("no file or folder name was sent to Jev")
    public void noNameWasSentToJev() {
        List<LoggedRequest> requests = jev.findAll(postRequestedFor(urlEqualTo(DECISIONS_PATH)));
        assertThat(requests).hasSize(2);
        for (LoggedRequest request : requests) {
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-key");
            String body = request.getBodyAsString();
            assertThat(body.toLowerCase(Locale.ROOT))
                .doesNotContain("000.png")
                .doesNotContain("cut_lead/")
                .doesNotContain("good/")
                .doesNotContain(workDir.getFileName().toString().toLowerCase(Locale.ROOT));
        }
    }

    private void stubJev(String image, String choice, double pGood, double pChoice, double cost) throws IOException {
        String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(inputDir.resolve(image)));
        String probabilities = choice.equals("GOOD")
            ? "{\"GOOD\": %s, \"MISPLACED\": 0.03, \"DAMAGED_CASE\": 0.03, \"CUT_LEAD\": 0.02, \"BENT_LEAD\": 0.01}".formatted(pGood)
            : "{\"GOOD\": %s, \"MISPLACED\": 0.02, \"DAMAGED_CASE\": 0.01, \"%s\": %s, \"BENT_LEAD\": 0.32}".formatted(pGood, choice, pChoice);
        String response = """
            {"model": "typesafe/jev-1.13-20260917",
             "answers": {"defect": {"type": "choice", "choice": "%s", "probabilities": %s, "confidence": 0.41}},
             "usage": {"input_tokens": 3505, "output_tokens": 65, "cost": %s},
             "provider": "TypeSafe"}
            """.formatted(choice, probabilities, cost);
        jev.stubFor(post(urlEqualTo(DECISIONS_PATH))
            .withHeader("Authorization", equalTo("Bearer test-key"))
            .withRequestBody(matchingJsonPath("$.model", equalTo("typesafe/jev-1.13")))
            .withRequestBody(matchingJsonPath("$.questions.defect.type", equalTo("choice")))
            .withRequestBody(matchingJsonPath("$.questions.defect.criteria.BENT_LEAD"))
            .withRequestBody(matchingJsonPath("$.state[1].type", equalTo("image_url")))
            .withRequestBody(matchingJsonPath("$.state[1].image_url.url", equalTo(dataUrl)))
            .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(response)));
    }

    private void copyFixture(String fixture, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try (InputStream in = getClass().getResourceAsStream("/transistor-images/" + fixture)) {
            Files.copy(in, target);
        }
    }
}
