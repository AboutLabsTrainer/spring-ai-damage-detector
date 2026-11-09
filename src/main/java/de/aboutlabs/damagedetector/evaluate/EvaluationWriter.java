package de.aboutlabs.damagedetector.evaluate;

import de.aboutlabs.damagedetector.domain.Evaluation;
import de.aboutlabs.damagedetector.domain.Inspection;
import de.aboutlabs.damagedetector.domain.JevOutcome;
import de.aboutlabs.damagedetector.domain.LabelledOutcome;
import de.aboutlabs.damagedetector.domain.PassPolicy;
import de.aboutlabs.damagedetector.domain.Verdict;
import de.aboutlabs.damagedetector.inspection.OutputFolder;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Driven adapter: writes evaluation.json and then evaluation.md through OutputFolder, replacing earlier ones as a whole.
 * Bounded change: no scrap or review copies (DDD-14). Field names follow the ratified contract.
 */
@Component
public class EvaluationWriter {

    private static final String DISCLOSURE = "The defect images were in the SPIKE sample, so the defect side shows "
        + "reproducibility and is not held-out validation. Jev results can differ from run to run.";
    private static final String ATTRIBUTION = "Sample data: MVTec AD \"transistor\", CC BY-NC-SA 4.0, non-commercial use";

    /** What the run knows beyond the scored outcomes: images without a label, the run date and the wall time. */
    public record RunFacts(int skipped, LocalDate runDate, Duration elapsed) {

        double elapsedSeconds() {
            return Math.round(elapsed.toMillis() / 100.0) / 10.0;
        }
    }

    private final JsonMapper jsonMapper;

    EvaluationWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /** The headline shared by evaluation.md and the summary line. */
    public static String headline(Evaluation evaluation) {
        return format("Evaluated %d labelled images at pass threshold %.2f: %d false passes, good auto-pass %d/%d (%.1f %%), "
                + "defects not passed %d/%d", evaluation.outcomes().size(), evaluation.passThreshold(),
            evaluation.falsePasses(), evaluation.goodPassed(), evaluation.goodCount(), goodAutoPassPercent(evaluation),
            evaluation.defectsNotPassed(), evaluation.defectCount());
    }

    void write(Evaluation evaluation, RunFacts facts, OutputFolder output) {
        output.writeString("evaluation.json", jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(toJson(evaluation, facts)));
        output.writeString("evaluation.md", toMarkdown(evaluation, facts));
    }

    private static double goodAutoPassPercent(Evaluation evaluation) {
        return evaluation.goodCount() == 0 ? 0.0 : 100.0 * evaluation.goodPassed() / evaluation.goodCount();
    }

    private static JsonEvaluation toJson(Evaluation evaluation, RunFacts facts) {
        JsonSummary summary = new JsonSummary(evaluation.outcomes().size(), facts.skipped(), evaluation.judged(),
            evaluation.jevErrors(), evaluation.falsePasses(), evaluation.goodPassed(), evaluation.goodCount(),
            evaluation.defectsNotPassed(), evaluation.defectCount());
        List<JsonClass> classes = evaluation.classRows().stream()
            .map(row -> new JsonClass(row.label().folderName(), row.images(), row.notPassed(), row.suspectedCorrect(), row.passed()))
            .toList();
        List<JsonFalsePass> falsePasses = evaluation.falsePassImages().stream()
            .map(outcome -> new JsonFalsePass(outcome.image(), probabilityOfGood(outcome)))
            .toList();
        PassPolicy policy = new PassPolicy(evaluation.passThreshold());
        List<JsonImage> images = evaluation.outcomes().stream().map(outcome -> toJson(outcome, policy)).toList();
        return new JsonEvaluation(facts.runDate().toString(), evaluation.passThreshold(), evaluation.jevCost(),
            evaluation.costIsLowerBound(), facts.elapsedSeconds(), summary, classes, falsePasses, images);
    }

    private static JsonImage toJson(LabelledOutcome outcome, PassPolicy policy) {
        Inspection inspection = Inspection.of(outcome.image(), outcome.outcome(), policy);
        String label = outcome.label().folderName();
        return switch (outcome.outcome()) {
            case JevOutcome.Judged judged -> new JsonImage(outcome.image(), label, inspection.verdict(), inspection.description(),
                false, judged.judgement().choice(), judged.judgement().probabilities());
            case JevOutcome.Failed failed -> new JsonImage(outcome.image(), label, inspection.verdict(), inspection.description(),
                true, null, null);
        };
    }

    private static double probabilityOfGood(LabelledOutcome outcome) {
        return outcome.outcome() instanceof JevOutcome.Judged judged ? judged.judgement().probabilityOfGood() : 0.0;
    }

    private static String toMarkdown(Evaluation evaluation, RunFacts facts) {
        StringBuilder md = new StringBuilder();
        md.append("# Transistor pass-rule evaluation\n\n");
        md.append("Run date: ").append(facts.runDate()).append(".\n\n");
        md.append(headline(evaluation)).append(".\n\n");
        md.append("skipped (no label): ").append(facts.skipped()).append("\n\n");
        md.append(format("Jev errors: %d; images judged: %d.\n\n", evaluation.jevErrors(), evaluation.judged()));
        md.append(format("Jev cost: $%.5f%s. Elapsed: %.1f s.\n\n", evaluation.jevCost(),
            evaluation.costIsLowerBound() ? " (lower bound: failed attempts are not counted)" : "", facts.elapsedSeconds()));
        appendFalsePasses(md, evaluation);
        appendClassTable(md, evaluation);
        md.append("## Evidence notes\n\n").append(DISCLOSURE).append("\n\n");
        return md.append(ATTRIBUTION).append('\n').toString();
    }

    private static void appendFalsePasses(StringBuilder md, Evaluation evaluation) {
        md.append("## False passes\n\n");
        if (evaluation.falsePassImages().isEmpty()) {
            md.append("None.\n");
        }
        evaluation.falsePassImages().forEach(outcome ->
            md.append(format("- %s  P(GOOD)=%.2f\n", outcome.image(), probabilityOfGood(outcome))));
        md.append('\n');
    }

    private static void appendClassTable(StringBuilder md, Evaluation evaluation) {
        md.append("## Per class\n\n");
        md.append("| Class | Images | Not passed | Suspected class correct | Passed |\n|---|---|---|---|---|\n");
        evaluation.classRows().forEach(row -> md.append(format("| %s | %d | %d | %d | %d |\n",
            row.label().folderName(), row.images(), row.notPassed(), row.suspectedCorrect(), row.passed())));
        md.append('\n');
    }

    private static String format(String pattern, Object... args) {
        return String.format(Locale.ROOT, pattern, args);
    }

    record JsonEvaluation(String runDate, double passThreshold, double jevCost, boolean costIsLowerBound, double elapsedSeconds,
                          JsonSummary summary, List<JsonClass> classes, List<JsonFalsePass> falsePassImages,
                          List<JsonImage> images) {
    }

    record JsonSummary(int labelled, int skipped, int judged, int jevErrors, int falsePasses, int goodPassed, int goodTotal,
                       int defectsNotPassed, int defectTotal) {
    }

    record JsonClass(String label, int images, int notPassed, int suspectedCorrect, int passed) {
    }

    record JsonFalsePass(String path, double pGood) {
    }

    record JsonImage(String path, String label, Verdict verdict, String description, boolean jevError, String jevChoice,
                     Map<String, Double> probabilities) {
    }
}
