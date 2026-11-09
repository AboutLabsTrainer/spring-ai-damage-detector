package de.aboutlabs.damagedetector.cli;

import de.aboutlabs.damagedetector.probe.ProbeResult;
import java.util.List;
import java.util.Optional;

/** Pre-parser shared by the bootstrap and DetectorCommand. Plain object: it runs before Spring starts. */
public final class CliArguments {

    private static final String EVALUATE = "--evaluate";

    private static final List<String> ALLOWED = List.of(EVALUATE, "--input", "--output", "--debug", "--trace");

    private final List<String> arguments;

    private CliArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    /** Bootstrap step 1. A refusal here writes nothing. */
    public static ProbeResult check(String[] args) {
        for (String argument : args) {
            Optional<String> problem = problemWith(argument);
            if (problem.isPresent()) {
                return usage(problem.get());
            }
        }
        CliArguments parsed = of(args);
        for (String name : List.of("input", "output")) {
            if (!parsed.hasOne(name)) {
                return usage("missing --" + name);
            }
        }
        return new ProbeResult.Ok();
    }

    private static Optional<String> problemWith(String argument) {
        String name = argument.split("=", 2)[0];
        if (name.equals(EVALUATE) && argument.contains("=")) {
            return Optional.of("--evaluate takes no value");
        }
        boolean known = ALLOWED.contains(name) || (name.startsWith("--") && name.contains("."));
        return known ? Optional.empty() : Optional.of("unknown option " + name);
    }

    private static ProbeResult usage(String problem) {
        return new ProbeResult.Refused(ProbeResult.Reason.USAGE, List.of(problem));
    }

    private boolean hasOne(String name) {
        return optionValues(name).size() == 1 && !optionValues(name).getFirst().isBlank();
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
        if (!hasOne(name)) {
            throw new IllegalArgumentException("Usage: java -jar damage-detector.jar --input=<folder> --output=<folder> (missing --" + name + ")");
        }
        return optionValues(name).getFirst();
    }

    private List<String> optionValues(String name) {
        String prefix = "--" + name + "=";
        return arguments.stream()
            .filter(argument -> argument.startsWith(prefix))
            .map(argument -> argument.substring(prefix.length()))
            .toList();
    }
}
