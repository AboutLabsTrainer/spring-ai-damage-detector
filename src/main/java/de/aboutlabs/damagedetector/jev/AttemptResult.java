package de.aboutlabs.damagedetector.jev;

import de.aboutlabs.damagedetector.domain.JevJudgement;
import java.time.Duration;
import java.util.Optional;

/** How one attempt ended. The classification stays in jev/, so the domain outcome stays free of it (ADR-004). */
sealed interface AttemptResult {

    record Answered(JevJudgement judgement) implements AttemptResult {
    }

    record Transient(String reason, Optional<Duration> retryAfter) implements AttemptResult {
    }

    record Permanent(String reason) implements AttemptResult {
    }
}
