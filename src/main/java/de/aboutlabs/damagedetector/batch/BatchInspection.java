package de.aboutlabs.damagedetector.batch;

import de.aboutlabs.damagedetector.domain.BatchReport;
import de.aboutlabs.damagedetector.domain.Inspection;
import de.aboutlabs.damagedetector.domain.PassPolicy;
import de.aboutlabs.damagedetector.inspection.ImageFolder;
import de.aboutlabs.damagedetector.inspection.Inspector;
import de.aboutlabs.damagedetector.inspection.OutputFolder;
import java.util.List;
import org.springframework.stereotype.Service;

/** Inspects every image below the input folder and writes the report to the output folder (C12 batch mode). */
@Service
public class BatchInspection {

    private final Inspector inspector;
    private final ReportWriter reportWriter;

    BatchInspection(Inspector inspector, ReportWriter reportWriter) {
        this.inspector = inspector;
        this.reportWriter = reportWriter;
    }

    public BatchReport run(ImageFolder images, OutputFolder output, PassPolicy policy) {
        List<Inspection> inspections = images.imagePaths().stream()
            .map(image -> inspector.inspect(images, image, policy))
            .toList();
        BatchReport report = new BatchReport(inspections);
        reportWriter.write(report, policy, images, output);
        return report;
    }
}
