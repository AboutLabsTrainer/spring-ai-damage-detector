package de.aboutlabs.damagedetector.inspection;

import de.aboutlabs.damagedetector.domain.Inspection;
import de.aboutlabs.damagedetector.domain.Jev;
import de.aboutlabs.damagedetector.domain.JevOutcome;
import de.aboutlabs.damagedetector.domain.PassPolicy;
import org.springframework.stereotype.Service;

/** Judges one image. Only its bytes go to Jev; the relative path stays local and only appears in the report (C11). */
@Service
public class Inspector {

    private final Jev jev;

    Inspector(Jev jev) {
        this.jev = jev;
    }

    public Inspection inspect(ImageFolder images, String image, PassPolicy policy) {
        JevOutcome outcome = new JevOutcome.Judged(jev.judge(images.bytesOf(image), ImageFolder.mediaTypeOf(image)));
        return Inspection.of(image, outcome, policy);
    }
}
