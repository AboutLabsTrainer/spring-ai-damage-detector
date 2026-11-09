package de.aboutlabs.damagedetector.batch;

import de.aboutlabs.damagedetector.domain.BatchReport;
import de.aboutlabs.damagedetector.domain.Inspection;
import de.aboutlabs.damagedetector.domain.JevJudgement;
import de.aboutlabs.damagedetector.domain.JevOutcome;
import de.aboutlabs.damagedetector.domain.PassPolicy;
import de.aboutlabs.damagedetector.domain.Verdict;
import de.aboutlabs.damagedetector.inspection.ImageFolder;
import de.aboutlabs.damagedetector.inspection.OutputFolder;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Driven adapter: copies every SCRAP/REVIEW image (plus a .txt description) into scrap/ or review/, so each bad case is
 * documented with its image and a text (C6), then writes report.json and report.md. report.md comes last: it marks the
 * run as finished (DDD-34).
 */
@Component
class ReportWriter {

    private final JsonMapper jsonMapper;

    ReportWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    void write(BatchReport report, PassPolicy policy, ImageFolder images, OutputFolder output) {
        report.notPassed().forEach(inspection -> document(inspection, images, output));
        output.writeString("report.json", jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(toJson(report, policy)));
        output.writeString("report.md", toMarkdown(report, policy));
    }

    private static String copyPathOf(Inspection inspection) {
        return inspection.verdict().name().toLowerCase(Locale.ROOT) + "/" + inspection.image();
    }

    private static void document(Inspection inspection, ImageFolder images, OutputFolder output) {
        String copy = copyPathOf(inspection);
        output.writeBytes(copy, images.bytesOf(inspection.image()));
        output.writeString(copy + ".txt", inspection.verdict() + ": " + inspection.description() + System.lineSeparator());
    }

    private static JsonReport toJson(BatchReport report, PassPolicy policy) {
        JsonSummary summary = new JsonSummary(report.inspections().size(), report.count(Verdict.PASS),
            report.count(Verdict.SCRAP), report.count(Verdict.REVIEW), report.judged(), report.jevErrors(),
            report.reviewShare(), report.jevCost(), report.costIsLowerBound(), policy.passThreshold());
        return new JsonReport(summary, report.inspections().stream().map(ReportWriter::toJson).toList());
    }

    private static JsonImage toJson(Inspection inspection) {
        return switch (inspection.outcome()) {
            case JevOutcome.Judged judged -> judgedImage(inspection, judged.judgement());
            case JevOutcome.Failed failed -> new JsonImage(inspection.image(), inspection.verdict(), inspection.description(),
                true, null, null, null, null);
        };
    }

    private static JsonImage judgedImage(Inspection inspection, JevJudgement judgement) {
        return new JsonImage(inspection.image(), inspection.verdict(), inspection.description(), false,
            judgement.choice(), judgement.probabilities(), judgement.confidence(), judgement.cost());
    }

    private static String toMarkdown(BatchReport report, PassPolicy policy) {
        StringBuilder md = new StringBuilder();
        md.append("# Transistor inspection report\n\n");
        md.append(format("Pass rule: PASS only if Jev chooses GOOD with P(GOOD) >= %.2f. Any defect choice is SCRAP. "
            + "GOOD below the threshold goes to REVIEW (a human decides).%n", policy.passThreshold()));
        md.append("Defect classes are what Jev suspects, not certain findings; check the image.\n\n");
        md.append("| Verdict | Count |\n|---|---|\n");
        for (Verdict verdict : Verdict.values()) {
            md.append("| ").append(verdict).append(" | ").append(report.count(verdict)).append(" |\n");
        }
        md.append("| Total | ").append(report.inspections().size()).append(" |\n\n");
        md.append(format("Jev errors: %d; images judged: %d.%n%n", report.jevErrors(), report.judged()));
        md.append(format("REVIEW share: %.1f %%. Total Jev cost: $%.5f%s.%n%n", report.reviewShare() * 100, report.jevCost(),
            report.costIsLowerBound() ? " (lower bound: failed attempts are not counted)" : ""));
        md.append("## Not passed\n\n");
        if (report.notPassed().isEmpty()) {
            return md.append("All images passed.\n").toString();
        }
        md.append("| Image | Verdict | Description | Copy |\n|---|---|---|---|\n");
        for (Inspection inspection : report.notPassed()) {
            md.append("| ").append(cell(inspection.image()))
                .append(" | ").append(inspection.verdict())
                .append(" | ").append(cell(inspection.description()))
                .append(" | [").append(cell(copyPathOf(inspection))).append("](<").append(copyPathOf(inspection)).append(">) |\n");
        }
        return md.toString();
    }

    private static String cell(String text) {
        return text.replace("|", "\\|");
    }

    private static String format(String pattern, Object... args) {
        return String.format(Locale.ROOT, pattern, args);
    }

    record JsonReport(JsonSummary summary, List<JsonImage> images) {
    }

    record JsonSummary(int total, long pass, long scrap, long review, long judged, long jevErrors, double reviewShare,
                       double jevCost, boolean costIsLowerBound, double passThreshold) {
    }

    record JsonImage(String path, Verdict verdict, String description, boolean jevError, String jevChoice,
                     Map<String, Double> probabilities, Double confidence, Double cost) {
    }
}
