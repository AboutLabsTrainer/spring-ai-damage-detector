package de.aboutlabs.damagedetector.acceptance.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.GroundTruth;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ImagePath;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ImageVerdict;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.JevClass;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/** What an evaluation left for Jonas: evaluation.md, evaluation.json and the summary line. */
public final class EvaluationRecord {

    private static final Pattern SWEEP_ROW = Pattern.compile("(?m)^\\|\\s*(\\d\\.\\d+)\\s*\\|\\s*\\d+\\s*\\|\\s*\\d+/\\d+\\s*\\|\\s*$");

    private final Workspace workspace;
    private final DetectorRun run;

    EvaluationRecord(Workspace workspace, DetectorRun run) {
        this.workspace = workspace;
        this.run = run;
    }

    /** K1 derivation: label other than good with verdict PASS. All views of the record must agree. */
    public void falsePasses(int count) {
        assertThat(images().stream().filter(EvaluationRecord::isFalsePass)).as("false passes in the record").hasSize(count);
        assertThat((Integer) json().read("$.summary.falsePasses")).as("summary.falsePasses").isEqualTo(count);
        assertThat(listAt("$.falsePassImages")).as("falsePassImages").hasSize(count);
        assertThat(markdown()).contains(": " + count + " false passes,");
        assertThat(run.summaryLines()).anySatisfy(line -> assertThat(line).contains(": " + count + " false passes,"));
    }

    public void goodAutoPass(int passed, int total) {
        assertThat((Integer) json().read("$.summary.goodPassed")).as("summary.goodPassed").isEqualTo(passed);
        assertThat((Integer) json().read("$.summary.goodTotal")).as("summary.goodTotal").isEqualTo(total);
        assertThat(markdown()).contains("good auto-pass " + passed + "/" + total + " (");
        assertThat(run.summaryLines()).anySatisfy(line -> assertThat(line).contains("good auto-pass " + passed + "/" + total + " ("));
    }

    public void recall(GroundTruth label, int notPassed, int total) {
        assertThat(classEntry(label)).containsEntry("images", total).containsEntry("notPassed", notPassed);
        assertThat(markdown()).containsPattern(classRow(label, total, notPassed, "\\d+", "\\d+"));
    }

    public void classRow(GroundTruth label, int images, int notPassed, int suspectedCorrect, int passed) {
        assertThat(classEntry(label)).containsEntry("images", images).containsEntry("notPassed", notPassed)
            .containsEntry("suspectedCorrect", suspectedCorrect).containsEntry("passed", passed);
        assertThat(markdown()).containsPattern(classRow(label, images, notPassed, String.valueOf(suspectedCorrect), String.valueOf(passed)));
    }

    public void suspectedClassNotCorrect(ImagePath image) {
        Map<String, Object> entry = entryOf(image);
        JevClass suspected = JevClass.valueOf((String) entry.get("jevChoice"));
        assertThat(suspected.isGroundTruthOf(image)).as("suspected %s for %s", suspected, image.value()).isFalse();
        GroundTruth label = GroundTruth.ofFolder((String) entry.get("label"));
        assertThat(classEntry(label)).containsEntry("suspectedCorrect", 0);
    }

    public void listsFalsePass(ImagePath image, double pGood) {
        assertThat(entryOf(image)).containsEntry("verdict", "PASS");
        assertThat(listAt("$.falsePassImages")).anySatisfy(entry -> {
            assertThat(entry).containsEntry("path", image.value());
            assertThat(((Number) entry.get("pGood")).doubleValue()).isEqualTo(pGood);
        });
        assertThat(markdown()).contains(String.format(Locale.ROOT, "- %s  P(GOOD)=%.2f", image.value(), pGood));
    }

    public void verdict(ImagePath image, ImageVerdict verdict) {
        assertThat(entryOf(image)).containsEntry("verdict", verdict.name());
    }

    public void skippedWithoutLabel(int count) {
        assertThat((Integer) json().read("$.summary.skipped")).as("summary.skipped").isEqualTo(count);
        assertThat(markdown()).contains("skipped (no label): " + count);
    }

    public void listsImages(int count) {
        assertThat(images()).hasSize(count);
        assertThat(markdown()).contains("Evaluated " + count + " labelled images");
    }

    /** A marker only the earlier record carries: an appended or merged record would still hold it. */
    public void noLongerMentions(String earlierMarker) {
        assertThat(markdown()).doesNotContain(earlierMarker);
        assertThat(textOf(workspace.output().resolve("evaluation.json"))).doesNotContain(earlierMarker);
    }

    public void markedAsJevError(ImagePath image) {
        assertThat(entryOf(image)).containsEntry("jevError", true);
        assertThat(entryOf(image).get("jevChoice")).as("no choice for a failed image").isNull();
    }

    public void countsJevErrors(int errors, int judged) {
        assertThat((Integer) json().read("$.summary.jevErrors")).as("summary.jevErrors").isEqualTo(errors);
        assertThat((Integer) json().read("$.summary.judged")).as("summary.judged").isEqualTo(judged);
        assertThat(markdown()).contains("Jev errors: " + errors + "; images judged: " + judged + ".");
    }

    public void cost(String amount) {
        assertThat(numberAt("$.jevCost")).isCloseTo(Double.parseDouble(amount.substring(1)), within(1e-9));
        assertThat(markdown()).contains("Jev cost: " + amount + ".");
    }

    public void costIsALowerBound() {
        assertThat((Boolean) json().read("$.costIsLowerBound")).as("costIsLowerBound").isTrue();
        assertThat(markdown()).contains("(lower bound: failed attempts are not counted)");
        assertThat(run.summaryLines()).anySatisfy(line -> assertThat(line).contains("(lower bound)"));
    }

    public void statesDuration() {
        assertThat(numberAt("$.elapsedSeconds")).isGreaterThanOrEqualTo(0.0);
        assertThat(markdown()).containsPattern(Pattern.compile("Elapsed: \\d+(\\.\\d+)? s\\."));
    }

    public void runDate(String date) {
        assertThat((String) json().read("$.runDate")).as("runDate").isEqualTo(date);
        assertThat(markdown()).contains("Run date: " + date + ".");
    }

    public void endsWith(String attribution) {
        assertThat(markdown().stripTrailing()).endsWith(attribution);
    }

    public void says(String sentence) {
        assertThat(markdown()).contains(sentence);
    }

    public void lowestSafeThreshold(String threshold) {
        assertThat(numberAt("$.lowestSafeThreshold")).isEqualTo(Double.parseDouble(threshold));
        assertThat(markdown()).contains("Lowest threshold with 0 false passes: " + threshold);
    }

    public void noThresholdIsSafe() {
        assertThat((Object) json().read("$.lowestSafeThreshold")).as("lowestSafeThreshold").isNull();
        assertThat(markdown()).contains("No threshold up to 0.99 reaches 0 false passes; route every GOOD to REVIEW");
    }

    public void sweepRow(double threshold, int falsePasses, int goodPassed, int goodTotal) {
        Map<String, Object> row = sweepRows().stream()
            .filter(candidate -> Math.abs(((Number) candidate.get("threshold")).doubleValue() - threshold) < 1e-9).findFirst()
            .orElseThrow(() -> new AssertionError("no sweep row for " + threshold));
        assertThat(row).containsEntry("falsePasses", falsePasses).containsEntry("goodPassed", goodPassed)
            .containsEntry("goodTotal", goodTotal);
    }

    public void sweepCoversEveryHundredth() {
        List<Integer> hundredths = sweepRows().stream()
            .map(row -> (int) Math.round(((Number) row.get("threshold")).doubleValue() * 100)).toList();
        assertThat(hundredths).containsAll(IntStream.rangeClosed(50, 99).boxed().toList());
    }

    public void readableSweepShows(double firstThreshold, double lastThreshold, double configured) {
        TreeSet<Integer> shown = new TreeSet<>();
        Matcher rows = SWEEP_ROW.matcher(markdown());
        while (rows.find()) {
            shown.add((int) Math.round(Double.parseDouble(rows.group(1)) * 100));
        }
        TreeSet<Integer> expected = new TreeSet<>(IntStream.rangeClosed((int) Math.round(firstThreshold * 100),
            (int) Math.round(lastThreshold * 100)).boxed().toList());
        expected.add((int) Math.round(configured * 100));
        assertThat(shown).isEqualTo(expected);
    }

    private static Pattern classRow(GroundTruth label, int images, int notPassed, String suspectedCorrect, String passed) {
        return Pattern.compile("\\|\\s*" + label.folderName() + "\\s*\\|\\s*" + images + "\\s*\\|\\s*" + notPassed
            + "\\s*\\|\\s*" + suspectedCorrect + "\\s*\\|\\s*" + passed + "\\s*\\|");
    }

    private static boolean isFalsePass(Map<String, Object> image) {
        return !"good".equals(image.get("label")) && "PASS".equals(image.get("verdict"));
    }

    private Map<String, Object> entryOf(ImagePath image) {
        return images().stream().filter(entry -> image.value().equals(entry.get("path"))).findFirst().orElseThrow(
            () -> new AssertionError(image.value() + " is not in evaluation.json"));
    }

    private Map<String, Object> classEntry(GroundTruth label) {
        return listAt("$.classes").stream().filter(entry -> label.folderName().equals(entry.get("label"))).findFirst()
            .orElseThrow(() -> new AssertionError(label.folderName() + " is not in classes of evaluation.json"));
    }

    private List<Map<String, Object>> images() {
        return listAt("$.images");
    }

    private List<Map<String, Object>> sweepRows() {
        return listAt("$.sweep");
    }

    private List<Map<String, Object>> listAt(String path) {
        List<Map<String, Object>> entries = json().read(path);
        assertThat(entries).as(path + " in evaluation.json").isNotNull();
        return entries;
    }

    private double numberAt(String path) {
        Number value = json().read(path);
        assertThat(value).as(path + " in evaluation.json").isNotNull();
        return value.doubleValue();
    }

    private DocumentContext json() {
        return JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS))
            .parse(textOf(workspace.output().resolve("evaluation.json")));
    }

    private String markdown() {
        return textOf(workspace.output().resolve("evaluation.md"));
    }

    private static String textOf(Path file) {
        assertThat(file).as("a file the run should have written").exists();
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
