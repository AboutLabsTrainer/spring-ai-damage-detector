package de.aboutlabs.damagedetector.domain;

/**
 * Zero-false-pass rule (C10): PASS only if Jev chose GOOD with P(GOOD) at or above the threshold,
 * SCRAP if Jev chose any defect, REVIEW when Jev leans GOOD but is not sure enough.
 */
public record PassPolicy(double passThreshold) {

    public PassPolicy {
        if (passThreshold < 0.0 || passThreshold > 1.0) {
            throw new IllegalArgumentException("pass threshold must be within [0, 1] but was " + passThreshold);
        }
    }

    public Verdict judge(JevJudgement judgement) {
        if (!judgement.choseGood()) {
            return Verdict.SCRAP;
        }
        return judgement.probabilityOfGood() >= passThreshold ? Verdict.PASS : Verdict.REVIEW;
    }
}
