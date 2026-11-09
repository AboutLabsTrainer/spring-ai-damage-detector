package de.aboutlabs.damagedetector.cli;

import de.aboutlabs.damagedetector.batch.BatchInspection;
import de.aboutlabs.damagedetector.domain.BatchReport;
import de.aboutlabs.damagedetector.domain.Evaluation;
import de.aboutlabs.damagedetector.domain.PassPolicy;
import de.aboutlabs.damagedetector.domain.Verdict;
import de.aboutlabs.damagedetector.evaluate.EvaluationWriter;
import de.aboutlabs.damagedetector.evaluate.LabelledEvaluation;
import de.aboutlabs.damagedetector.inspection.ImageFolder;
import de.aboutlabs.damagedetector.inspection.OutputFolder;
import de.aboutlabs.damagedetector.probe.ProbeResult;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

/**
 * Driving adapter: {@code java -jar damage-detector.jar [--evaluate] --input=<folder> --output=<folder>}. It builds its per-run objects
 * from the arguments itself, because the walking skeleton starts Spring without the bootstrap. The exit code of the run
 * reaches the launcher as an {@link ExitCodeGenerator}; the stderr line comes from the pure {@link ErrorFeedback}.
 */
@Component
class DetectorCommand implements ApplicationRunner, ExitCodeGenerator {

    private static final int COMPLETED = 0;

    private final BatchInspection batchInspection;
    private final LabelledEvaluation labelledEvaluation;
    private final PassPolicy passPolicy;
    private final ErrorFeedback errorFeedback = new ErrorFeedback();
    private int exitCode = COMPLETED;

    DetectorCommand(BatchInspection batchInspection, LabelledEvaluation labelledEvaluation, PassPolicy passPolicy) {
        this.batchInspection = batchInspection;
        this.labelledEvaluation = labelledEvaluation;
        this.passPolicy = passPolicy;
    }

    @Override
    public void run(ApplicationArguments args) {
        CliArguments arguments = CliArguments.of(args.getSourceArgs());
        ImageFolder images = ImageFolder.rootedAt(Path.of(arguments.input()));
        OutputFolder output = OutputFolder.rootedAt(Path.of(arguments.output()));
        if (arguments.evaluate()) {
            evaluate(images, output, arguments);
            return;
        }
        BatchReport report = batchInspection.run(images, output, passPolicy);
        System.out.println(summaryLine(report, arguments.output()));
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    private void evaluate(ImageFolder images, OutputFolder output, CliArguments arguments) {
        Optional<Evaluation> evaluation = labelledEvaluation.run(images, output, passPolicy);
        if (evaluation.isEmpty()) {
            report(errorFeedback.feedbackFor(
                new ProbeResult.Refused(ProbeResult.Reason.NO_LABELLED_IMAGES, List.of(arguments.input()))));
            return;
        }
        Evaluation scored = evaluation.get();
        System.out.println(summaryLine(scored, arguments.output()));
        if (scored.falsePasses() > 0) {
            report(errorFeedback.falsePasses(scored.falsePasses(), scored.passThreshold(), evaluationPath(arguments.output())));
        }
    }

    private void report(ErrorFeedback.Feedback feedback) {
        System.err.println(feedback.line());
        exitCode = feedback.exitCode();
    }

    /** The path stays as the user typed it (UC-5). */
    private static String summaryLine(BatchReport report, String typedOutput) {
        return String.format(Locale.ROOT,
            "Inspected %d images: %d PASS, %d SCRAP, %d REVIEW (REVIEW share %.1f %%; %d Jev errors in REVIEW), Jev cost $%.5f%s -> %s",
            report.inspections().size(), report.count(Verdict.PASS), report.count(Verdict.SCRAP),
            report.count(Verdict.REVIEW), report.reviewShare() * 100, report.jevErrors(), report.jevCost(),
            costNote(report.costIsLowerBound()), Path.of(typedOutput).resolve("report.md"));
    }

    private static String summaryLine(Evaluation evaluation, String typedOutput) {
        return String.format(Locale.ROOT, "%s, Jev cost $%.5f%s -> %s", EvaluationWriter.headline(evaluation),
            evaluation.jevCost(), costNote(evaluation.costIsLowerBound()), evaluationPath(typedOutput));
    }

    private static String evaluationPath(String typedOutput) {
        return Path.of(typedOutput).resolve("evaluation.md").toString();
    }

    private static String costNote(boolean costIsLowerBound) {
        return costIsLowerBound ? " (lower bound)" : "";
    }
}
