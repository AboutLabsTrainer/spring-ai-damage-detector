// SCAFFOLD: true
package de.aboutlabs.damagedetector.domain;

/** Either Jev judged the image or every allowed attempt failed. PassPolicy maps Failed to REVIEW (ADR-003). */
public sealed interface JevOutcome {

    /** {@code attempts} above 1 means earlier attempts failed, so the reported cost is a lower bound (DDD-15). */
    record Judged(JevJudgement judgement, int attempts) implements JevOutcome {

        public Judged(JevJudgement judgement) {
            this(judgement, 1);
        }
    }

    record Failed(int attempts, String reason) implements JevOutcome {
    }
}
