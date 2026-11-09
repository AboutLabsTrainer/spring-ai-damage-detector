package de.aboutlabs.damagedetector;

import java.nio.file.Path;
import java.util.Map;

/** Pure function: the property map naming {@code <output>/damage-detector.log}. The launcher applies it before Spring starts. */
final class LogSink {

    private static final String LOG_FILE = "damage-detector.log";

    private LogSink() {
    }

    static Map<String, Object> defaultPropertiesFor(Path realOutput) {
        return Map.of("logging.file.name", realOutput.resolve(LOG_FILE).toString());
    }
}
