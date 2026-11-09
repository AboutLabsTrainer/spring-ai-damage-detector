package de.aboutlabs.damagedetector.cli;

import static org.assertj.core.api.Assertions.assertThat;

import de.aboutlabs.damagedetector.probe.ProbeResult;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** CONTRACT_SHAPE: pure-function */
class PassThresholdTest {

    @ParameterizedTest
    @CsvSource({"0, 0.0", "0.0, 0.0", "0.86, 0.86", "0.875, 0.875", "1, 1.0", "1.0, 1.0"})
    void a_number_from_0_to_1_gives_its_policy(String raw, double threshold) {
        PassThreshold passThreshold = PassThreshold.of(raw);

        assertThat(passThreshold.probe()).isInstanceOf(ProbeResult.Ok.class);
        assertThat(passThreshold.policy().passThreshold()).isEqualTo(threshold);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.5", "1.0000001", "-0.1", "abc", "NaN", "Infinity", "0,86", ""})
    void anything_else_is_refused_and_echoed(String raw) {
        assertThat(PassThreshold.of(raw).probe())
            .isEqualTo(new ProbeResult.Refused(ProbeResult.Reason.THRESHOLD_INVALID, List.of(raw)));
    }
}
