package de.aboutlabs.damagedetector.jev;

import static de.aboutlabs.damagedetector.jev.JevAdapterFixture.API_KEY;
import static de.aboutlabs.damagedetector.jev.JevAdapterFixture.DECISIONS_URL;
import static de.aboutlabs.damagedetector.jev.JevAdapterFixture.IMAGE;
import static de.aboutlabs.damagedetector.jev.JevAdapterFixture.answer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.aboutlabs.damagedetector.domain.JevJudgement;
import de.aboutlabs.damagedetector.domain.JevOutcome;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/** DELIVER enables these with slice 03; PIT counts them, it does not count Cucumber (DEVOPS, Mutation testing strategy). */
class OpenRouterJevAdapterTest {

    private final JevAdapterFixture fixture = new JevAdapterFixture();

    @Test
    void sends_the_key_the_model_the_image_and_the_fixed_question_and_nothing_about_the_file() {
        fixture.jev.expect(once(), requestTo(DECISIONS_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer " + API_KEY))
            .andExpect(jsonPath("$.model").value("typesafe/jev-1.13"))
            .andExpect(jsonPath("$.state[1].type").value("image_url"))
            .andExpect(jsonPath("$.state[1].image_url.url")
                .value("data:image/png;base64," + Base64.getEncoder().encodeToString(IMAGE)))
            .andExpect(jsonPath("$.questions.defect.type").value("choice"))
            .andExpect(jsonPath("$.questions.defect.criteria.GOOD").exists())
            .andExpect(jsonPath("$.questions.defect.criteria.BENT_LEAD").exists())
            .andRespond(withSuccess(answer("GOOD", 0.91), MediaType.APPLICATION_JSON));

        fixture.retrying(1).judge(IMAGE, "image/png");

        fixture.jev.verify();
    }

    @Test
    void a_valid_answer_becomes_a_judgement_with_its_cost_and_confidence() {
        fixture.jev.expect(once(), requestTo(DECISIONS_URL))
            .andRespond(withSuccess(answer("CUT_LEAD", 0.12), MediaType.APPLICATION_JSON));

        JevOutcome outcome = fixture.retrying(1).judge(IMAGE, "image/png");

        assertThat(outcome).isInstanceOfSatisfying(JevOutcome.Judged.class, judged -> {
            JevJudgement judgement = judged.judgement();
            assertThat(judgement.choice()).isEqualTo("CUT_LEAD");
            assertThat(judgement.probabilityOfGood()).isEqualTo(0.12);
            assertThat(judgement.confidence()).isEqualTo(0.41);
            assertThat(judgement.cost()).isEqualTo(0.00015);
        });
    }

    @Test
    void a_missing_confidence_and_a_missing_cost_become_zero() {
        fixture.jev.expect(once(), requestTo(DECISIONS_URL)).andRespond(withSuccess(
            "{\"answers\": {\"defect\": {\"choice\": \"GOOD\", \"probabilities\": {\"GOOD\": 0.9}}}}", MediaType.APPLICATION_JSON));

        JevOutcome outcome = fixture.retrying(1).judge(IMAGE, "image/png");

        assertThat(outcome).isInstanceOfSatisfying(JevOutcome.Judged.class, judged -> {
            assertThat(judged.judgement().confidence()).isZero();
            assertThat(judged.judgement().cost()).isZero();
        });
    }

    @ParameterizedTest
    @CsvSource({"0.0", "1.0"})
    void the_edges_of_the_probability_range_are_valid(double pGood) {
        fixture.jev.expect(once(), requestTo(DECISIONS_URL))
            .andRespond(withSuccess(answer("GOOD", pGood), MediaType.APPLICATION_JSON));

        assertThat(fixture.retrying(1).judge(IMAGE, "image/png")).isInstanceOf(JevOutcome.Judged.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {408, 429, 500, 502, 503, 504})
    void a_transient_status_is_tried_again_up_to_the_limit(int status) {
        HttpStatus httpStatus = HttpStatus.valueOf(status);
        fixture.jev.expect(times(3), requestTo(DECISIONS_URL)).andRespond(withStatus(httpStatus));

        JevOutcome outcome = fixture.retrying(3).judge(IMAGE, "image/png");

        assertThat(outcome).isEqualTo(new JevOutcome.Failed(3, "HTTP " + status + " " + httpStatus.getReasonPhrase()));
        fixture.jev.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 422})
    void any_other_client_error_is_final_after_one_attempt(int status) {
        HttpStatus httpStatus = HttpStatus.valueOf(status);
        fixture.jev.expect(once(), requestTo(DECISIONS_URL)).andRespond(withStatus(httpStatus));

        JevOutcome outcome = fixture.retrying(3).judge(IMAGE, "image/png");

        assertThat(outcome).isEqualTo(new JevOutcome.Failed(1, "HTTP " + status + " " + httpStatus.getReasonPhrase()));
        assertThat(fixture.waits).isEmpty();
        fixture.jev.verify();
    }

    @Test
    void a_timeout_is_transient_and_named() {
        fixture.jev.expect(times(2), requestTo(DECISIONS_URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        JevOutcome outcome = fixture.retrying(2).judge(IMAGE, "image/png");

        assertThat(outcome).isInstanceOfSatisfying(JevOutcome.Failed.class, failed -> {
            assertThat(failed.attempts()).isEqualTo(2);
            assertThat(failed.reason()).isEqualTo("timeout after 30 s");
        });
    }

    @Test
    void a_connection_error_is_transient_and_its_reason_carries_no_request_data() {
        fixture.jev.expect(times(2), requestTo(DECISIONS_URL))
            .andRespond(withException(new ConnectException("Connection refused to " + API_KEY)));

        JevOutcome outcome = fixture.retrying(2).judge(IMAGE, "image/png");

        assertThat(outcome).isInstanceOfSatisfying(JevOutcome.Failed.class, failed -> {
            assertThat(failed.attempts()).isEqualTo(2);
            assertThat(failed.reason()).isNotBlank().doesNotContain(API_KEY);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"answers\": {\"defect\": {\"choice\": \"GOOD\", \"confidence\": 0.4}}}",
        "{\"answers\": {\"defect\": {\"choice\": \"GOOD\", \"probabilities\": null}}}",
        "{\"answers\": {\"defect\": {\"choice\": \"GOOD\", \"probabilities\": {\"GOOD\": null}}}}",
        "{\"answers\": {\"defect\": {\"choice\": \"GOOD\", \"probabilities\": {\"CUT_LEAD\": 0.1}}}}",
        "{\"answers\": {\"defect\": {\"choice\": \"GOOD\", \"probabilities\": {\"GOOD\": 1.7}}}}",
        "{\"answers\": {\"defect\": {\"choice\": \"GOOD\", \"probabilities\": {\"GOOD\": 1.0000001}}}}",
        "{\"answers\": {\"defect\": {\"choice\": \"GOOD\", \"probabilities\": {\"GOOD\": -0.2}}}}",
        "{\"answers\": {\"defect\": {\"choice\": \"GOOD\", \"probabilities\": {\"GOOD\": NaN}}}}",
        "{\"answers\": {\"defect\": {\"choice\": \"SCRATCHED\", \"probabilities\": {\"GOOD\": 0.9}}}}",
        "{\"answers\": {\"defect\": {\"choice\": null, \"probabilities\": {\"GOOD\": 0.9}}}}",
        "{\"answers\": {}}",
        "{}",
        "<html>Bad gateway page</html>",
        ""
    })
    void an_answer_that_cannot_be_trusted_is_transient_and_unreadable(String body) {
        fixture.jev.expect(times(2), requestTo(DECISIONS_URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        JevOutcome outcome = fixture.retrying(2).judge(IMAGE, "image/png");

        assertThat(outcome).isEqualTo(new JevOutcome.Failed(2, "unreadable answer"));
        fixture.jev.verify();
    }

    @Test
    void a_recorded_real_answer_is_understood() throws IOException {
        // OQ-5: the contract with the alpha API is one real, unmodified response. Recorded at the slice-01 production run.
        try (InputStream golden = getClass().getResourceAsStream("/jev/golden-response.json")) {
            assertThat(golden).as("src/test/resources/jev/golden-response.json, recorded from a real Jev run").isNotNull();
            fixture.jev.expect(once(), requestTo(DECISIONS_URL))
                .andRespond(withSuccess(new String(golden.readAllBytes(), StandardCharsets.UTF_8), MediaType.APPLICATION_JSON));
        }

        assertThat(fixture.retrying(1).judge(IMAGE, "image/png")).isInstanceOfSatisfying(JevOutcome.Judged.class, judged -> {
            assertThat(judged.judgement().choice()).isEqualTo("GOOD");
            assertThat(judged.judgement().probabilityOfGood()).isEqualTo(0.88);
        });
    }
}
