package de.aboutlabs.damagedetector.jev;

import static de.aboutlabs.damagedetector.jev.JevAdapterFixture.DECISIONS_URL;
import static de.aboutlabs.damagedetector.jev.JevAdapterFixture.IMAGE;
import static de.aboutlabs.damagedetector.jev.JevAdapterFixture.MAX_BACKOFF;
import static de.aboutlabs.damagedetector.jev.JevAdapterFixture.answer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.aboutlabs.damagedetector.domain.JevOutcome;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.response.DefaultResponseCreator;

/**
 * Every reply script up to three long, for every limit up to four, against a small model of the attempt loop.
 *
 * <p>CONTRACT_SHAPE: bounded-change
 */
class RetryingJevAttemptLoopTest {

    enum Reply { ANSWERS, TRANSIENT, PERMANENT }

    static Stream<Arguments> scriptsAndLimits() {
        List<List<Reply>> scripts = new ArrayList<>(List.of(List.of()));
        List<List<Reply>> all = new ArrayList<>();
        for (int length = 1; length <= 3; length++) {
            List<List<Reply>> longer = new ArrayList<>();
            for (List<Reply> script : scripts) {
                for (Reply reply : Reply.values()) {
                    List<Reply> next = new ArrayList<>(script);
                    next.add(reply);
                    longer.add(next);
                }
            }
            all.addAll(longer);
            scripts = longer;
        }
        return all.stream().flatMap(script -> IntStream.rangeClosed(1, 4).mapToObj(limit -> Arguments.of(script, limit)));
    }

    @ParameterizedTest
    @MethodSource("scriptsAndLimits")
    void the_attempt_loop_stops_at_the_first_answer_the_first_final_failure_or_the_limit(List<Reply> script, int maxAttempts) {
        int expectedAttempts = attemptsOf(script, maxAttempts);
        JevAdapterFixture fixture = new JevAdapterFixture();
        IntStream.range(0, expectedAttempts).forEach(attempt -> respond(fixture, replyAt(script, attempt)));

        JevOutcome outcome = fixture.retrying(maxAttempts).judge(IMAGE, "image/png");

        fixture.jev.verify();
        Reply last = replyAt(script, expectedAttempts - 1);
        assertThat(outcome instanceof JevOutcome.Judged).isEqualTo(last == Reply.ANSWERS);
        int attempts = outcome instanceof JevOutcome.Judged judged ? judged.attempts() : ((JevOutcome.Failed) outcome).attempts();
        assertThat(attempts).isEqualTo(expectedAttempts);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 5, 8})
    void every_retry_is_preceded_by_one_wait_that_never_shrinks_or_exceeds_the_cap(int maxAttempts) {
        JevAdapterFixture fixture = new JevAdapterFixture();
        IntStream.range(0, maxAttempts).forEach(attempt -> respond(fixture, Reply.TRANSIENT));

        fixture.retrying(maxAttempts).judge(IMAGE, "image/png");

        assertThat(fixture.waits).hasSize(maxAttempts - 1).isSorted().allSatisfy(
            wait -> assertThat(wait).isLessThanOrEqualTo(MAX_BACKOFF).isGreaterThan(Duration.ZERO));
    }

    /** A script that runs out carries on with transient failures, so the loop only ever stops at an answer, a permanent failure or the limit. */
    private static Reply replyAt(List<Reply> script, int attempt) {
        return attempt < script.size() ? script.get(attempt) : Reply.TRANSIENT;
    }

    private static int attemptsOf(List<Reply> script, int maxAttempts) {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            if (replyAt(script, attempt) != Reply.TRANSIENT) {
                return attempt + 1;
            }
        }
        return maxAttempts;
    }

    private static void respond(JevAdapterFixture fixture, Reply reply) {
        fixture.jev.expect(once(), requestTo(DECISIONS_URL)).andRespond(responseFor(reply));
    }

    private static DefaultResponseCreator responseFor(Reply reply) {
        return switch (reply) {
            case ANSWERS -> withSuccess(answer("GOOD", 0.9), MediaType.APPLICATION_JSON);
            case TRANSIENT -> withStatus(HttpStatus.SERVICE_UNAVAILABLE);
            case PERMANENT -> withStatus(HttpStatus.UNAUTHORIZED);
        };
    }
}
