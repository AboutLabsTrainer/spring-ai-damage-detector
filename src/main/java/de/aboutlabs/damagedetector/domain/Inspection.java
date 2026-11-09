package de.aboutlabs.damagedetector.domain;

import java.util.Locale;

/** One inspected image: where it came from (relative to the input folder), what Jev said and the verdict. */
public record Inspection(String image, Verdict verdict, JevOutcome outcome, double passThreshold) {

    public static Inspection of(String image, JevOutcome outcome, PassPolicy policy) {
        return new Inspection(image, policy.judge(outcome), outcome, policy.passThreshold());
    }

    public boolean judged() {
        return outcome instanceof JevOutcome.Judged;
    }

    /** What the judged attempt cost; the cost of a failed attempt is unknown and not counted (DDD-15). */
    public double cost() {
        return outcome instanceof JevOutcome.Judged judged ? judged.judgement().cost() : 0.0;
    }

    /** Short human-readable reason. Defect classes are always presented as suspected, never as certain. */
    public String description() {
        return switch (outcome) {
            case JevOutcome.Judged judged -> describe(judged.judgement());
            case JevOutcome.Failed failed -> describe(failed);
        };
    }

    private String describe(JevJudgement judgement) {
        double pGood = judgement.probabilityOfGood();
        return switch (verdict) {
            case PASS -> format("GOOD; P(GOOD)=%.2f", pGood);
            case SCRAP -> format("Suspected %s (p=%.2f); P(GOOD)=%.2f", judgement.choice(), judgement.probabilityOfChoice(), pGood);
            case REVIEW -> format("Leans GOOD but below the pass threshold %.2f; P(GOOD)=%.2f", passThreshold, pGood);
        };
    }

    private static String describe(JevOutcome.Failed failed) {
        String attempts = failed.attempts() == 1 ? "1 attempt" : failed.attempts() + " attempts";
        return "Jev call failed after " + attempts + ": " + failed.reason();
    }

    private static String format(String pattern, Object... args) {
        return String.format(Locale.ROOT, pattern, args);
    }
}
