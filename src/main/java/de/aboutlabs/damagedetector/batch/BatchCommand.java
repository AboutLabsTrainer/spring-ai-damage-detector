package de.aboutlabs.damagedetector.batch;

import de.aboutlabs.damagedetector.domain.BatchReport;
import de.aboutlabs.damagedetector.domain.Verdict;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Driving adapter: {@code java -jar damage-detector.jar --input=<folder> --output=<folder>}. */
@Component
class BatchCommand implements ApplicationRunner {

    private final BatchInspection batchInspection;

    BatchCommand(BatchInspection batchInspection) {
        this.batchInspection = batchInspection;
    }

    @Override
    public void run(ApplicationArguments args) {
        Path input = folderOption(args, "input");
        Path output = folderOption(args, "output");
        BatchReport report = batchInspection.run(input, output);
        System.out.println(String.format(Locale.ROOT,
            "Inspected %d images: %d PASS, %d SCRAP, %d REVIEW (REVIEW share %.1f %%), Jev cost $%.5f -> %s",
            report.inspections().size(), report.count(Verdict.PASS), report.count(Verdict.SCRAP),
            report.count(Verdict.REVIEW), report.reviewShare() * 100, report.totalCost(), output.resolve("report.md")));
    }

    private static Path folderOption(ApplicationArguments args, String name) {
        List<String> values = args.getOptionValues(name);
        if (values == null || values.size() != 1 || values.getFirst().isBlank()) {
            throw new IllegalArgumentException("Usage: java -jar damage-detector.jar --input=<folder> --output=<folder> (missing --" + name + ")");
        }
        return Path.of(values.getFirst()).toAbsolutePath().normalize();
    }
}
