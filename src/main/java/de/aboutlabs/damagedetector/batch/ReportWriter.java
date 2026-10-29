package de.aboutlabs.damagedetector.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.aboutlabs.damagedetector.domain.BatchReport;
import de.aboutlabs.damagedetector.domain.Inspection;
import de.aboutlabs.damagedetector.domain.PassPolicy;
import de.aboutlabs.damagedetector.domain.Verdict;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Driven adapter: writes report.json, report.md and copies every SCRAP/REVIEW image (plus a .txt description)
 * into scrap/ or review/, so each bad case is documented with its image and a text (C6).
 */
@Component
class ReportWriter {

    private final ObjectMapper objectMapper;
    private final PassPolicy passPolicy;

    ReportWriter(ObjectMapper objectMapper, PassPolicy passPolicy) {
        this.objectMapper = objectMapper;
        this.passPolicy = passPolicy;
    }

    void write(BatchReport report, Path input, Path output) {
        try {
            Files.createDirectories(output);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.resolve("report.json").toFile(), toJson(report));
            Files.writeString(output.resolve("report.md"), toMarkdown(report));
            for (Inspection inspection : report.notPassed()) {
                document(inspection, input, output);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String copyPathOf(Inspection inspection) {
        return inspection.verdict().name().toLowerCase(Locale.ROOT) + "/" + inspection.image();
    }

    private void document(Inspection inspection, Path input, Path output) throws IOException {
        Path copy = output.resolve(copyPathOf(inspection));
        Files.createDirectories(copy.getParent());
        Files.copy(input.resolve(inspection.image()), copy, StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(Path.of(copy + ".txt"), inspection.verdict() + ": " + inspection.description() + System.lineSeparator());
    }

    private JsonReport toJson(BatchReport report) {
        JsonSummary summary = new JsonSummary(report.inspections().size(), report.count(Verdict.PASS),
            report.count(Verdict.SCRAP), report.count(Verdict.REVIEW), report.reviewShare(), report.totalCost(),
            passPolicy.passThreshold());
        List<JsonImage> images = report.inspections().stream()
            .map(inspection -> new JsonImage(inspection.image(), inspection.verdict(), inspection.description(),
                inspection.judgement().choice(), inspection.judgement().probabilities(),
                inspection.judgement().confidence(), inspection.judgement().cost()))
            .toList();
        return new JsonReport(summary, images);
    }

    private String toMarkdown(BatchReport report) {
        StringBuilder md = new StringBuilder();
        md.append("# Transistor inspection report\n\n");
        md.append(format("Pass rule: PASS only if Jev chooses GOOD with P(GOOD) >= %.2f. Any defect choice is SCRAP. "
            + "GOOD below the threshold goes to REVIEW (a human decides).%n", passPolicy.passThreshold()));
        md.append("Defect classes are what Jev suspects, not certain findings; check the image.\n\n");
        md.append("| Verdict | Count |\n|---|---|\n");
        for (Verdict verdict : Verdict.values()) {
            md.append("| ").append(verdict).append(" | ").append(report.count(verdict)).append(" |\n");
        }
        md.append("| Total | ").append(report.inspections().size()).append(" |\n\n");
        md.append(format("REVIEW share: %.1f %%. Total Jev cost: $%.5f.%n%n", report.reviewShare() * 100, report.totalCost()));
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

    record JsonSummary(int total, long pass, long scrap, long review, double reviewShare, double totalCost,
                       double passThreshold) {
    }

    record JsonImage(String path, Verdict verdict, String description, String jevChoice,
                     Map<String, Double> probabilities, double confidence, double cost) {
    }
}
