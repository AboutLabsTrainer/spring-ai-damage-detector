package de.aboutlabs.damagedetector.acceptance.support;

import static org.assertj.core.api.Assertions.assertThat;

import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ContainerCheckId;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ContainerFinding;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ImagePath;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/** Drives scripts/container-check.sh (policy: docker run). Runs only under the @container tag, never in ./mvnw verify. */
public final class ContainerCheck {

    private static final List<ImagePath> DEMO_IMAGES = List.of(new ImagePath("good/000.png"), new ImagePath("cut_lead/000.png"));

    private String images = "";
    private String findings = "";

    public void usesTheDemoImages() {
        images = DEMO_IMAGES.stream().map(ImagePath::value).collect(Collectors.joining(" "));
    }

    public void runs(ContainerCheckId check) {
        ProcessBuilder builder = new ProcessBuilder("bash", "scripts/container-check.sh", check.id()).redirectErrorStream(true);
        builder.environment().put("CONTAINER_CHECK_IMAGES", images);
        try {
            Process process = builder.start();
            findings = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitValue = process.waitFor();
            assertThat(exitValue).as("container-check exit value, findings: %s", findings).isZero();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    public void confirms(ContainerFinding finding) {
        assertThat(findings.lines()).as("container-check findings").contains("PASS " + finding.id());
    }
}
