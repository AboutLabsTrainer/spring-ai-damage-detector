package de.aboutlabs.damagedetector.jev;

import static de.aboutlabs.damagedetector.jev.JevAdapterFixture.DECISIONS_URL;
import static de.aboutlabs.damagedetector.jev.JevAdapterFixture.IMAGE;
import static de.aboutlabs.damagedetector.jev.JevAdapterFixture.answer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.aboutlabs.damagedetector.domain.JevOutcome;
import de.aboutlabs.damagedetector.probe.ProbeResult;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class RetryingJevTest {

    private final JevAdapterFixture fixture = new JevAdapterFixture();

    @Test
    void waits_double_from_the_initial_backoff() {
        fixture.jev.expect(times(4), requestTo(DECISIONS_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        fixture.retrying(4).judge(IMAGE, "image/png");

        assertThat(fixture.waits).containsExactly(seconds(1), seconds(2), seconds(4));
    }

    @Test
    void waits_stop_growing_at_the_maximum_backoff() {
        fixture.jev.expect(times(6), requestTo(DECISIONS_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        fixture.retrying(6).judge(IMAGE, "image/png");

        assertThat(fixture.waits).containsExactly(seconds(1), seconds(2), seconds(4), seconds(8), seconds(10));
    }

    @Test
    void there_is_no_wait_after_the_last_attempt() {
        fixture.jev.expect(once(), requestTo(DECISIONS_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        JevOutcome outcome = fixture.retrying(1).judge(IMAGE, "image/png");

        assertThat(outcome).isEqualTo(new JevOutcome.Failed(1, "HTTP 503 Service Unavailable"));
        assertThat(fixture.waits).isEmpty();
    }

    @Test
    void a_success_after_a_failure_is_a_normal_judgement() {
        fixture.jev.expect(once(), requestTo(DECISIONS_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        fixture.jev.expect(once(), requestTo(DECISIONS_URL))
            .andRespond(withSuccess(answer("BENT_LEAD", 0.1), MediaType.APPLICATION_JSON));

        JevOutcome outcome = fixture.retrying(3).judge(IMAGE, "image/png");

        assertThat(outcome).isInstanceOfSatisfying(JevOutcome.Judged.class, judged -> assertThat(judged.attempts()).isEqualTo(2));
        assertThat(fixture.waits).containsExactly(seconds(1));
    }

    @Test
    void a_first_time_success_counts_one_attempt() {
        fixture.jev.expect(once(), requestTo(DECISIONS_URL))
            .andRespond(withSuccess(answer("GOOD", 0.9), MediaType.APPLICATION_JSON));

        assertThat(fixture.retrying(3).judge(IMAGE, "image/png")).isInstanceOfSatisfying(JevOutcome.Judged.class,
            judged -> assertThat(judged.attempts()).isEqualTo(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"soon", "1.5", "-4", "Wed, 21 Oct 2026 07:28:00 GMT"})
    void a_retry_after_that_is_no_whole_number_of_seconds_is_ignored(String retryAfter) {
        fixture.jev.expect(once(), requestTo(DECISIONS_URL))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", retryAfter));
        fixture.jev.expect(once(), requestTo(DECISIONS_URL))
            .andRespond(withSuccess(answer("GOOD", 0.9), MediaType.APPLICATION_JSON));

        fixture.retrying(3).judge(IMAGE, "image/png");

        assertThat(fixture.waits).containsExactly(seconds(1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 4, 10})
    void a_retry_after_within_the_cap_replaces_the_backoff(int retryAfter) {
        fixture.jev.expect(once(), requestTo(DECISIONS_URL))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", String.valueOf(retryAfter)));
        fixture.jev.expect(once(), requestTo(DECISIONS_URL))
            .andRespond(withSuccess(answer("GOOD", 0.9), MediaType.APPLICATION_JSON));

        fixture.retrying(3).judge(IMAGE, "image/png");

        assertThat(fixture.waits).isEqualTo(List.of(seconds(retryAfter)));
    }

    @Test
    void a_retry_after_above_the_cap_gives_up_at_once() {
        fixture.jev.expect(once(), requestTo(DECISIONS_URL))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "11"));

        JevOutcome outcome = fixture.retrying(3).judge(IMAGE, "image/png");

        assertThat(outcome).isEqualTo(new JevOutcome.Failed(1,
            "HTTP 429 Too Many Requests (Retry-After 11 s exceeds max-backoff)"));
        assertThat(fixture.waits).isEmpty();
        fixture.jev.verify();
    }

    @Test
    void the_attempt_count_of_a_late_give_up_is_the_real_one() {
        fixture.jev.expect(once(), requestTo(DECISIONS_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        fixture.jev.expect(once(), requestTo(DECISIONS_URL))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "60"));

        JevOutcome outcome = fixture.retrying(3).judge(IMAGE, "image/png");

        assertThat(outcome).isEqualTo(new JevOutcome.Failed(2,
            "HTTP 429 Too Many Requests (Retry-After 60 s exceeds max-backoff)"));
    }

    @Disabled("pending: step 05-02, startup probe of RetryingJev (DDD-6)")
    @Test
    void the_probe_accepts_a_key_and_a_usable_address_without_calling_jev() {
        assertThat(fixture.retrying(3).probe()).isInstanceOf(ProbeResult.Ok.class);
        fixture.jev.verify();
    }

    @Disabled("pending: step 05-02, startup probe of RetryingJev (DDD-6)")
    @Test
    void the_probe_refuses_a_blank_key() {
        assertThat(new JevAdapterFixture(" ", JevAdapterFixture.BASE_URL).retrying(3).probe())
            .isInstanceOfSatisfying(ProbeResult.Refused.class,
                refused -> assertThat(refused.reason()).isEqualTo(ProbeResult.Reason.NO_API_KEY));
    }

    @Disabled("pending: step 05-02, startup probe of RetryingJev (DDD-6)")
    @ParameterizedTest
    @ValueSource(strings = {"not a url", "ftp://jev.test", "http://", "/api/alpha"})
    void the_probe_refuses_an_unusable_address(String address) {
        assertThat(new JevAdapterFixture(JevAdapterFixture.API_KEY, address).retrying(3).probe())
            .isInstanceOfSatisfying(ProbeResult.Refused.class,
                refused -> assertThat(refused.reason()).isEqualTo(ProbeResult.Reason.JEV_BASE_URL_INVALID));
    }

    @Disabled("pending: step 05-02, startup probe of RetryingJev (DDD-6)")
    @Test
    void the_key_is_checked_before_the_address() {
        assertThat(new JevAdapterFixture(" ", "not a url").retrying(3).probe())
            .isInstanceOfSatisfying(ProbeResult.Refused.class,
                refused -> assertThat(refused.reason()).isEqualTo(ProbeResult.Reason.NO_API_KEY));
    }

    private static Duration seconds(long seconds) {
        return Duration.ofSeconds(seconds);
    }
}
