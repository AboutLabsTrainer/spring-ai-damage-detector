package de.aboutlabs.damagedetector.batch;

import de.aboutlabs.damagedetector.domain.BatchReport;
import de.aboutlabs.damagedetector.domain.Inspection;
import de.aboutlabs.damagedetector.domain.Jev;
import de.aboutlabs.damagedetector.domain.PassPolicy;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Service;

/** Inspects every image below the input folder and writes the report to the output folder (C12 batch mode). */
@Service
class BatchInspection {

    private final ImageFolder imageFolder;
    private final Jev jev;
    private final PassPolicy passPolicy;
    private final ReportWriter reportWriter;

    BatchInspection(ImageFolder imageFolder, Jev jev, PassPolicy passPolicy, ReportWriter reportWriter) {
        this.imageFolder = imageFolder;
        this.jev = jev;
        this.passPolicy = passPolicy;
        this.reportWriter = reportWriter;
    }

    BatchReport run(Path input, Path output) {
        List<Inspection> inspections = imageFolder.imagesBelow(input).stream()
            .map(image -> inspect(input, image))
            .toList();
        BatchReport report = new BatchReport(inspections);
        reportWriter.write(report, input, output);
        return report;
    }

    private Inspection inspect(Path input, Path image) {
        // Only the bytes go to Jev; the relative path stays local and only appears in the report (C11).
        String relativePath = input.relativize(image).toString().replace('\\', '/');
        return Inspection.of(relativePath, jev.judge(readBytes(image), ImageFolder.mediaTypeOf(image)), passPolicy);
    }

    private static byte[] readBytes(Path image) {
        try {
            return Files.readAllBytes(image);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
