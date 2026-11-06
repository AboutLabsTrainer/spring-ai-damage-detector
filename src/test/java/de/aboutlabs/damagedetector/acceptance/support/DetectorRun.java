package de.aboutlabs.damagedetector.acceptance.support;

import static de.aboutlabs.damagedetector.common.StateDelta.assertStateDelta;
import static de.aboutlabs.damagedetector.common.StateDelta.everyEntry;
import static de.aboutlabs.damagedetector.common.StateDelta.setTo;
import static org.assertj.core.api.Assertions.assertThat;

import de.aboutlabs.damagedetector.acceptance.support.DetectorDriver.RunResult;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ApiKey;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ImagePath;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.Interruption;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.RunDeclaration;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.RunEnding;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.StartMistake;
import de.aboutlabs.damagedetector.common.StateDelta;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Starts the detector through the active driver and keeps the before and after observables of the last run.
 * Every run must leave the input folder untouched (DDD-29), so that holds for all scenarios, including refusals.
 */
public final class DetectorRun {

    private static final Set<String> UNIVERSE = Set.of("input.tree", "output.exists", "output.tree");
    private static final List<String> SHORT_WAITS_FOR_THE_REAL_PROCESS =
        List.of("--detector.jev.retry.initial-backoff=1ms", "--detector.jev.retry.max-backoff=5ms");

    private final Workspace workspace;
    private final JevStub jev;
    private final FakeCapabilities capabilities;
    private final List<String> settings = new ArrayList<>();
    private DetectorDriver driver;
    private RealProcessDriver realProcess;
    private String apiKey = "test-key";
    private String jevAddress;
    private Map<String, Object> before = Map.of();
    private Map<String, Object> after = Map.of();
    private RunResult result = RunResult.NONE;

    DetectorRun(Workspace workspace, JevStub jev, FakeCapabilities capabilities) {
        this.workspace = workspace;
        this.jev = jev;
        this.capabilities = capabilities;
        this.driver = new InProcessDriver(capabilities);
    }

    public void useTheRealProcess() {
        realProcess = new RealProcessDriver(workspace);
        driver = realProcess;
    }

    public void attemptsPerImage(int attempts) {
        settings.add("--detector.jev.retry.max-attempts=" + attempts);
    }

    public void answerTimeLimitSeconds(int seconds) {
        settings.add("--detector.jev.read-timeout=" + seconds + "s");
    }

    public void jevAddress(String address) {
        jevAddress = address;
    }

    public void apiKey(ApiKey key) {
        apiKey = switch (key) {
            case MISSING -> null;
            case BLANK -> " ";
            case MARKED -> "sk-or-canary-" + UUID.randomUUID();
        };
    }

    public String apiKeyInUse() {
        return apiKey;
    }

    public void batch() {
        start(locations(), List.of());
    }

    public void batchAt(double threshold) {
        start(locations(), List.of("--detector.pass-threshold=" + threshold));
    }

    public void batchWithConsoleDetails() {
        start(locations(), List.of("--debug"));
    }

    public void evaluation() {
        start(locations(), List.of("--evaluate"));
    }

    public void evaluationAt(double threshold) {
        start(locations(), List.of("--evaluate", "--detector.pass-threshold=" + threshold));
    }

    public void batchWithThresholdSetting(String setting) {
        start(locations(), List.of("--detector.pass-threshold=" + setting));
    }

    public void batchInterruptedDuring(ImagePath image, Interruption interruption) {
        before = observe();
        Map<String, String> environment = new LinkedHashMap<>();
        result = realProcess.runInterrupted(argumentsWith(locations(), List.of(), environment), environment,
            () -> jev.awaitAskedAbout(image), interruption);
        after = observe();
        assertStateDelta(before, after, Set.of("input.tree"), Map.of());
    }

    public void startsWith(StartMistake mistake) {
        start(argumentsFor(mistake), List.of());
    }

    public void endedWith(RunEnding ending) {
        assertThat(result.exitCode()).as("exit code; stderr was: %s", result.stderr()).isEqualTo(ending.exitCode());
    }

    public void toldExactly(String message) {
        assertThat(oneProblemLine()).isEqualTo(workspace.resolve(message));
    }

    public void toldSomethingStartingWith(String start) {
        assertThat(oneProblemLine()).startsWith(workspace.resolve(start));
    }

    public void toldSomethingEndingWith(String end) {
        assertThat(oneProblemLine()).endsWith(workspace.resolve(end));
    }

    public void toldSomethingContaining(String fragment) {
        assertThat(oneProblemLine()).contains(workspace.resolve(fragment));
    }

    public void endedInterruptedBy(Interruption interruption) {
        assertThat(result.exitCode()).as("exit code of an interrupted run").isEqualTo(interruption.exitCode());
    }

    public void reportedNoProblem() {
        assertThat(result.stderrLines()).as("stderr").isEmpty();
    }

    public void onlySummaryLineIs(String line) {
        assertThat(result.stdoutLines()).as("stdout").containsExactly(workspace.resolve(line));
    }

    public List<String> summaryLines() {
        return result.stdoutLines();
    }

    public String console() {
        return result.stdout() + result.stderr();
    }

    /** DDD-29: the output tree equals the declared universe of this run type; nothing else changed. */
    public void changedOnly(RunDeclaration declaration) {
        if (declaration == RunDeclaration.REFUSED_BEFORE_WRITING) {
            assertStateDelta(before, after, UNIVERSE, Map.of());
            return;
        }
        Map<String, StateDelta.Expectation> expected = Map.of(
            "output.exists", setTo(true),
            "output.tree", everyEntry(declaration.describe(), entry -> isDeclared(entry, declaration)));
        assertStateDelta(before, after, UNIVERSE, expected);
        assertThat(workspace.outputTree()).containsAll(declaration.requiredFiles());
        assertThat(workspace.outputTree().stream().filter(entry -> entry.endsWith(".tmp")))
            .as("temp files left behind").hasSizeLessThanOrEqualTo(declaration.tempFilesAllowed());
    }

    /** Files that were there before the run are left alone, so they are not part of its delta. */
    private boolean isDeclared(String entry, RunDeclaration declaration) {
        return declaration.allows(entry) || ((List<?>) before.get("output.tree")).contains(entry);
    }

    private String oneProblemLine() {
        assertThat(result.stderrLines()).as("stderr must be exactly one line").hasSize(1);
        String line = result.stderrLines().getFirst();
        assertThat(line).as("no stack trace").doesNotContain("\tat ").doesNotContain("Exception in thread");
        return line;
    }

    private List<String> locations() {
        return List.of("--input=" + workspace.input(), "--output=" + workspace.output());
    }

    private List<String> argumentsFor(StartMistake mistake) {
        Path input = workspace.input();
        return switch (mistake) {
            case NO_INPUT -> List.of("--output=" + workspace.output());
            case NO_OUTPUT -> List.of("--input=" + input);
            case INPUT_DOES_NOT_EXIST -> {
                workspace.useInput(workspace.sibling("no-such-folder"));
                yield locations();
            }
            case INPUT_IS_A_FILE -> {
                workspace.useInput(input.resolve("good/000.png"));
                yield locations();
            }
            case OUTPUT_EQUALS_INPUT -> {
                workspace.useOutput(input);
                yield locations();
            }
            case OUTPUT_INSIDE_INPUT -> {
                workspace.useOutput(input.resolve("run-output"));
                yield locations();
            }
            case OUTPUT_CLIMBS_BACK -> {
                workspace.useOutput(input.resolve("good").resolve("..").resolve("run-output"));
                yield locations();
            }
            case OUTPUT_THROUGH_SHORTCUT -> {
                workspace.shortcut(workspace.sibling("looks-separate"), input);
                workspace.useOutput(workspace.sibling("looks-separate").resolve("run-output"));
                yield locations();
            }
            case OUTPUT_THROUGH_MISSING_FOLDER -> {
                workspace.useOutput(workspace.sibling("no-such-folder").resolve("..").resolve(input.getFileName()).resolve("run-output"));
                yield locations();
            }
            case MISSPELT_OPTION -> withFlag("--evalute");
            case VALUE_AFTER_EVALUATE -> withFlag("--evaluate=true");
        };
    }

    private List<String> withFlag(String flag) {
        List<String> arguments = new ArrayList<>(locations());
        arguments.add(flag);
        return arguments;
    }

    private void start(List<String> arguments, List<String> options) {
        Map<String, String> environment = new LinkedHashMap<>();
        List<String> all = argumentsWith(arguments, options, environment);
        before = observe();
        result = driver.run(all, environment);
        after = observe();
        assertStateDelta(before, after, Set.of("input.tree"), Map.of());
    }

    private List<String> argumentsWith(List<String> arguments, List<String> options, Map<String, String> environment) {
        List<String> all = new ArrayList<>(arguments);
        all.addAll(options);
        all.addAll(settings);
        all.add("--detector.jev.base-url=" + (jevAddress != null ? jevAddress : jev.baseUrl()));
        if (realProcess != null) {
            all.addAll(SHORT_WAITS_FOR_THE_REAL_PROCESS);
            if (apiKey != null) {
                environment.put("OPENROUTER_API_KEY", apiKey);
            }
        } else if (apiKey != null) {
            all.add("--detector.jev.api-key=" + apiKey);
        }
        return all;
    }

    private Map<String, Object> observe() {
        Map<String, Object> observed = new LinkedHashMap<>();
        observed.put("input.tree", workspace.inputTree());
        observed.put("output.exists", workspace.outputExists());
        observed.put("output.tree", workspace.outputTree());
        return observed;
    }
}
