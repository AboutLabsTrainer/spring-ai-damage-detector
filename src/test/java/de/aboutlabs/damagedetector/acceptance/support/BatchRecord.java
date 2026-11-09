package de.aboutlabs.damagedetector.acceptance.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ImagePath;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ImageVerdict;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** What a batch run left for Petra: report.md, report.json, the copies and their descriptions, the summary line. */
public final class BatchRecord {

    private final Workspace workspace;
    private final DetectorRun run;

    BatchRecord(Workspace workspace, DetectorRun run) {
        this.workspace = workspace;
        this.run = run;
    }

    /** All three views must agree, and INV-4 must hold (DoD 5). */
    public void counts(int pass, int scrap, int review, int jevErrors) {
        assertThat((Integer) json().read("$.summary.pass")).as("PASS").isEqualTo(pass);
        assertThat((Integer) json().read("$.summary.scrap")).as("SCRAP").isEqualTo(scrap);
        assertThat((Integer) json().read("$.summary.review")).as("REVIEW").isEqualTo(review);
        assertThat((Integer) json().read("$.summary.total")).as("INV-4 total").isEqualTo(pass + scrap + review);
        assertThat((Integer) json().read("$.summary.jevErrors")).as("Jev errors").isEqualTo(jevErrors);
        assertThat(jevErrors).as("INV-4 errors within REVIEW").isLessThanOrEqualTo(review);
        assertThat(markdown())
            .contains("| PASS | " + pass + " |")
            .contains("| SCRAP | " + scrap + " |")
            .contains("| REVIEW | " + review + " |")
            .contains("Jev errors: " + jevErrors + "; images judged: ");
        assertThat(run.summaryLines()).as("summary line").anySatisfy(line -> assertThat(line)
            .startsWith("Inspected " + (pass + scrap + review) + " images: " + pass + " PASS, " + scrap + " SCRAP, " + review + " REVIEW (")
            .contains("; " + jevErrors + " Jev errors in REVIEW)"));
    }

    public void judged(int images) {
        assertThat((Integer) json().read("$.summary.judged")).as("judged images").isEqualTo(images);
        assertThat(markdown()).contains("; images judged: " + images + ".");
    }

    public void documentedAs(ImagePath image, ImageVerdict verdict, String description) {
        copyIsPresent(image, verdict);
        assertThat(descriptionFileOf(image, verdict)).content().isEqualToIgnoringWhitespace(verdict + ": " + description);
        assertThat(markdown()).contains(image.value()).contains(description);
        assertThat(entryOf(image)).containsEntry("verdict", verdict.name()).containsEntry("description", description);
    }

    public void documentedWithDescriptionStarting(ImagePath image, ImageVerdict verdict, String start) {
        copyIsPresent(image, verdict);
        assertThat(descriptionFileOf(image, verdict)).content().startsWith(verdict + ": " + start);
    }

    public void markedAsJevError(ImagePath image) {
        assertThat(entryOf(image)).containsEntry("jevError", true);
        assertThat(entryOf(image).get("jevChoice")).as("no choice for a failed image").isNull();
        assertThat(entryOf(image).get("probabilities")).as("no probabilities for a failed image").isNull();
        assertThat(entryOf(image).get("confidence")).as("no confidence for a failed image").isNull();
        assertThat(entryOf(image).get("cost")).as("no cost for a failed image").isNull();
    }

    public void cost(String amount) {
        Number jevCost = json().read("$.summary.jevCost");
        assertThat(jevCost).as("summary.jevCost").isNotNull();
        assertThat(jevCost.doubleValue()).isCloseTo(Double.parseDouble(amount.substring(1)), within(1e-9));
        assertThat(markdown()).contains("Total Jev cost: " + amount);
        assertThat(run.summaryLines()).anySatisfy(line -> assertThat(line).contains("Jev cost " + amount));
    }

    public void costIsALowerBound() {
        assertThat((Boolean) json().read("$.summary.costIsLowerBound")).as("summary.costIsLowerBound").isTrue();
        assertThat(markdown()).contains("(lower bound: failed attempts are not counted)");
        assertThat(run.summaryLines()).anySatisfy(line -> assertThat(line).contains("(lower bound)"));
    }

    public void costIsExact() {
        assertThat((Boolean) json().read("$.summary.costIsLowerBound")).as("summary.costIsLowerBound").isFalse();
        assertThat(markdown()).doesNotContain("lower bound");
        assertThat(run.summaryLines()).allSatisfy(line -> assertThat(line).doesNotContain("lower bound"));
    }

    public void statesThreshold(double threshold) {
        assertThat(((Number) json().read("$.summary.passThreshold")).doubleValue()).isEqualTo(threshold);
    }

    /**
     * INV-6: report.md is published last, so its absence is what marks a run as incomplete. An interrupt in the final
     * write phase may leave report.json, but only whole, because every write is atomic.
     */
    public void isIncomplete() {
        assertThat(workspace.output().resolve("report.md")).doesNotExist();
        Path reportJson = workspace.output().resolve("report.json");
        if (Files.exists(reportJson)) {
            assertThat((Object) JsonPath.parse(textOf(reportJson)).read("$.summary")).as("a whole report.json").isNotNull();
        }
    }

    public void keptOutputFile(String fileName, String content) {
        assertThat(workspace.output().resolve(fileName)).content().isEqualTo(content);
    }

    public void logHoldsTheFailureDetails() {
        assertThat(workspace.output().resolve("damage-detector.log")).content().contains("\tat ");
    }

    /** INV-3: scans the console and the raw bytes of every file in the output folder. */
    public void nothingContains(String secret) {
        assertThat(run.console()).doesNotContain(secret);
        workspace.outputTree().forEach(file -> assertThat(lenientTextOf(workspace.output().resolve(file))).as(file).doesNotContain(secret));
    }

    private void copyIsPresent(ImagePath image, ImageVerdict verdict) {
        Path copy = workspace.output().resolve(verdict.folder()).resolve(image.value());
        assertThat(copy).hasSameBinaryContentAs(workspace.input().resolve(image.value()));
    }

    private Path descriptionFileOf(ImagePath image, ImageVerdict verdict) {
        return workspace.output().resolve(verdict.folder()).resolve(image.value() + ".txt");
    }

    private Map<String, Object> entryOf(ImagePath image) {
        List<Map<String, Object>> images = json().read("$.images");
        assertThat(images).as("images in report.json").isNotNull();
        return images.stream().filter(entry -> image.value().equals(entry.get("path"))).findFirst().orElseThrow(
            () -> new AssertionError(image.value() + " is not in report.json"));
    }

    private DocumentContext json() {
        return JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS))
            .parse(textOf(workspace.output().resolve("report.json")));
    }

    private String markdown() {
        return textOf(workspace.output().resolve("report.md"));
    }

    private static String textOf(Path file) {
        assertThat(file).as("a file the run should have written").exists();
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Copies of images are binary, so this decodes leniently: bad sequences are replaced, an ASCII key stays intact. */
    private static String lenientTextOf(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
