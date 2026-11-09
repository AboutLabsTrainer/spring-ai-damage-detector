package de.aboutlabs.damagedetector;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** CONTRACT_SHAPE: external-effect */
class LogSinkTest {

    @Test
    void the_log_file_lies_directly_in_the_output_folder() {
        assertThat(LogSink.defaultPropertiesFor(Path.of("/out")))
            .containsEntry("logging.file.name", Path.of("/out", "damage-detector.log").toString());
    }
}
