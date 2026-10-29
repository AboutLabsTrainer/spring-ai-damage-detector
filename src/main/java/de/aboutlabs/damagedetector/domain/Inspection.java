package de.aboutlabs.damagedetector.domain;

import java.util.Locale;

/** One inspected image: where it came from (relative to the input folder), Jev's judgement and the verdict. */
public record Inspection(String image, Verdict verdict, JevJudgement judgement, double passThreshold) {

    public static Inspection of(String image, JevJudgement judgement, PassPolicy policy) {
        return new Inspection(image, policy.judge(judgement), judgement, policy.passThreshold());
    }

    /** Short human-readable reason. Defect classes are always presented as suspected, never as certain. */
    public String description() {
        double pGood = judgement.probabilityOfGood();
        return switch (verdict) {
            case PASS -> format("GOOD; P(GOOD)=%.2f", pGood);
            case SCRAP -> format("Suspected %s (p=%.2f); P(GOOD)=%.2f", judgement.choice(), judgement.probabilityOfChoice(), pGood);
            case REVIEW -> format("Leans GOOD but below the pass threshold %.2f; P(GOOD)=%.2f", passThreshold, pGood);
        };
    }

    private static String format(String pattern, Object... args) {
        return String.format(Locale.ROOT, pattern, args);
    }
}
