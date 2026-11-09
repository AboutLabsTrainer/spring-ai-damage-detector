package de.aboutlabs.damagedetector.domain;

/** Either Jev judged the image or every allowed attempt failed. PassPolicy maps Failed to REVIEW (ADR-003). */
public sealed interface JevOutcome {

    /** Whether a failed attempt lies behind this outcome, so that its cost is unknown (DDD-15). */
    boolean hadFailedAttempt();

    /** {@code attempts} above 1 means earlier attempts failed, so the reported cost is a lower bound (DDD-15). */
    record Judged(JevJudgement judgement, int attempts) implements JevOutcome {

        @Override
        public boolean hadFailedAttempt() {
            return attempts > 1;
        }
    }

    record Failed(int attempts, String reason) implements JevOutcome {

        @Override
        public boolean hadFailedAttempt() {
            return true;
        }
    }
}
