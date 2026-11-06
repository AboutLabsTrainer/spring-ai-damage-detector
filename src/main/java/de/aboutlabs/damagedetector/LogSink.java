// SCAFFOLD: true
package de.aboutlabs.damagedetector;

import java.nio.file.Path;
import java.util.Map;

/** Pure function: the property map naming {@code <output>/damage-detector.log}. The launcher applies it before Spring starts. */
final class LogSink {

    private LogSink() {
    }

    static Map<String, Object> defaultPropertiesFor(Path realOutput) {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }
}
