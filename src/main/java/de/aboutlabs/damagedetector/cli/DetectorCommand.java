package de.aboutlabs.damagedetector.cli;

import de.aboutlabs.damagedetector.batch.BatchInspection;
import de.aboutlabs.damagedetector.domain.BatchReport;
import de.aboutlabs.damagedetector.domain.PassPolicy;
import de.aboutlabs.damagedetector.domain.Verdict;
import de.aboutlabs.damagedetector.inspection.ImageFolder;
import de.aboutlabs.damagedetector.inspection.OutputFolder;
import java.nio.file.Path;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Driving adapter: {@code java -jar damage-detector.jar --input=<folder> --output=<folder>}. It builds its per-run objects
 * from the arguments itself, because the walking skeleton starts Spring without the bootstrap.
 */
@Component
class DetectorCommand implements ApplicationRunner {

    private final BatchInspection batchInspection;
    private final PassPolicy passPolicy;

    DetectorCommand(BatchInspection batchInspection, PassPolicy passPolicy) {
        this.batchInspection = batchInspection;
        this.passPolicy = passPolicy;
    }

    @Override
    public void run(ApplicationArguments args) {
        CliArguments arguments = CliArguments.of(args.getSourceArgs());
        ImageFolder images = ImageFolder.rootedAt(Path.of(arguments.input()));
        OutputFolder output = OutputFolder.rootedAt(Path.of(arguments.output()));
        BatchReport report = batchInspection.run(images, output, passPolicy);
        System.out.println(summaryLine(report, arguments.output()));
    }

    /** The path stays as the user typed it (UC-5). */
    private static String summaryLine(BatchReport report, String typedOutput) {
        return String.format(Locale.ROOT,
            "Inspected %d images: %d PASS, %d SCRAP, %d REVIEW (REVIEW share %.1f %%; %d Jev errors in REVIEW), Jev cost $%.5f%s -> %s",
            report.inspections().size(), report.count(Verdict.PASS), report.count(Verdict.SCRAP),
            report.count(Verdict.REVIEW), report.reviewShare() * 100, report.jevErrors(), report.jevCost(),
            report.costIsLowerBound() ? " (lower bound)" : "", Path.of(typedOutput).resolve("report.md"));
    }
}
