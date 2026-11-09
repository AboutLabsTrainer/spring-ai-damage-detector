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
import java.util.List;
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

    public Evaluation run(ImageFolder images, OutputFolder output, PassPolicy policy) {
        Instant start = clock.instant();
        List<String> paths = images.imagePaths();
        List<LabelledOutcome> outcomes = paths.stream()
            .flatMap(image -> labelOf(image).map(label -> inspect(images, image, label, policy)).stream())
            .toList();
        Evaluation evaluation = Evaluation.of(outcomes, policy.passThreshold());
        Instant end = clock.instant();
        writer.write(evaluation, new EvaluationWriter.RunFacts(paths.size() - outcomes.size(),
            LocalDate.ofInstant(start, clock.getZone()), Duration.between(start, end)), output);
        return evaluation;
    }

    private LabelledOutcome inspect(ImageFolder images, String image, Label label, PassPolicy policy) {
        return new LabelledOutcome(image, label, inspector.inspect(images, image, policy).outcome());
    }
}
