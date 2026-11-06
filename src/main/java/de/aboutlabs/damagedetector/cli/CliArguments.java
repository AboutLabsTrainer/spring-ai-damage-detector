package de.aboutlabs.damagedetector.cli;

import de.aboutlabs.damagedetector.probe.ProbeResult;
import java.util.List;

/** Pre-parser shared by the bootstrap and DetectorCommand. Plain object: it runs before Spring starts. */
public final class CliArguments {

    private static final String EVALUATE = "--evaluate";

    private final List<String> arguments;

    private CliArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    // SCAFFOLD: true
    /** Bootstrap step 1. A refusal here writes nothing. */
    public static ProbeResult check(String[] args) {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    public static CliArguments of(String[] args) {
        return new CliArguments(List.of(args));
    }

    public boolean evaluate() {
        return arguments.contains(EVALUATE);
    }

    public String input() {
        return folderOption("input");
    }

    public String output() {
        return folderOption("output");
    }

    private String folderOption(String name) {
        String prefix = "--" + name + "=";
        List<String> values = arguments.stream()
            .filter(argument -> argument.startsWith(prefix))
            .map(argument -> argument.substring(prefix.length()))
            .toList();
        if (values.size() != 1 || values.getFirst().isBlank()) {
            throw new IllegalArgumentException("Usage: java -jar damage-detector.jar --input=<folder> --output=<folder> (missing --" + name + ")");
        }
        return values.getFirst();
    }
}
