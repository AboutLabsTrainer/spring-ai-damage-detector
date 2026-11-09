package de.aboutlabs.damagedetector.evaluate;

import de.aboutlabs.damagedetector.domain.Evaluation;
import de.aboutlabs.damagedetector.domain.Label;
import de.aboutlabs.damagedetector.domain.LabelledOutcome;
import de.aboutlabs.damagedetector.domain.PassPolicy;
import de.aboutlabs.damagedetector.inspection.ImageFolder;
import de.aboutlabs.damagedetector.inspection.Inspector;
import de.aboutlabs.damagedetector.inspection.OutputFolder;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Scores a labelled folder (C12 evaluate mode): resolves each label, inspects every labelled image through the shared
 * inspection path (DDD-3), scores the outcomes and hands them to the writer. No direct IO. It is the only place where a
 * label is resolved; images without a label are counted and never sent to Jev (DDD-13).
 */
@Service
public class LabelledEvaluation {

    private static final char SEPARATOR = '/';

    private final Inspector inspector;
    private final EvaluationWriter writer;
    private final Clock clock;

    LabelledEvaluation(Inspector inspector, EvaluationWriter writer, Clock clock) {
        this.inspector = inspector;
        this.writer = writer;
        this.clock = clock;
    }

    /** The first path segment, matched exactly against the five folder names (DDD-13). */
    public static Optional<Label> labelOf(String relativePath) {
        int end = relativePath.indexOf(SEPARATOR);
        if (end < 0) {
            return Optional.empty();
        }
        String firstSegment = relativePath.substring(0, end);
        return Arrays.stream(Label.values()).filter(label -> label.folderName().equals(firstSegment)).findFirst();
    }

    /**
     * Scores every labelled image and writes the evaluation. Empty when the folder holds no labelled image: then Jev is
     * not called and nothing is written (NO_LABELLED_IMAGES, ADR-005).
     */
    public Optional<Evaluation> run(ImageFolder images, OutputFolder output, PassPolicy policy) {
        Instant start = clock.instant();
        List<String> paths = images.imagePaths();
        Map<String, Label> labelled = labelled(paths);
        if (labelled.isEmpty()) {
            return Optional.empty();
        }
        List<LabelledOutcome> outcomes = labelled.entrySet().stream()
            .map(entry -> inspect(images, entry.getKey(), entry.getValue(), policy))
            .toList();
        Evaluation evaluation = Evaluation.of(outcomes, policy.passThreshold());
        Instant end = clock.instant();
        writer.write(evaluation, new EvaluationWriter.RunFacts(paths.size() - outcomes.size(),
            LocalDate.ofInstant(start, clock.getZone()), Duration.between(start, end)), output);
        return Optional.of(evaluation);
    }

    /** The labelled images in folder order; the label is resolved here and nowhere else. */
    private static Map<String, Label> labelled(List<String> paths) {
        Map<String, Label> labelled = new LinkedHashMap<>();
        paths.forEach(path -> labelOf(path).ifPresent(label -> labelled.put(path, label)));
        return labelled;
    }

    private LabelledOutcome inspect(ImageFolder images, String image, Label label, PassPolicy policy) {
        return new LabelledOutcome(image, label, inspector.inspect(images, image, policy).outcome());
    }
}
