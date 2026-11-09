package de.aboutlabs.damagedetector.domain;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.IntStream;

/** Pure metrics over one Jev pass at the configured threshold (DDD-4). */
public final class Evaluation {

    /** One row of the per-class table. "Not passed" is the recall of DDD-16. */
    public record ClassRow(Label label, int images, int notPassed, int suspectedCorrect, int passed) {
    }

    private static final int SWEEP_FIRST_HUNDREDTH = 50;
    private static final int SWEEP_LAST_HUNDREDTH = 99;
    static final double SWEEP_LAST = SWEEP_LAST_HUNDREDTH / 100.0;

    private final List<LabelledOutcome> outcomes;
    private final PassPolicy policy;

    private Evaluation(List<LabelledOutcome> outcomes, PassPolicy policy) {
        this.outcomes = List.copyOf(outcomes);
        this.policy = policy;
    }

    public static Evaluation of(List<LabelledOutcome> outcomes, double configuredThreshold) {
        return new Evaluation(outcomes, new PassPolicy(configuredThreshold));
    }

    public List<LabelledOutcome> outcomes() {
        return outcomes;
    }

    public double passThreshold() {
        return policy.passThreshold();
    }

    public Verdict verdictOf(LabelledOutcome outcome) {
        return policy.judge(outcome.outcome());
    }

    public int falsePasses() {
        return falsePassImages().size();
    }

    public List<LabelledOutcome> falsePassImages() {
        return defects().stream().filter(this::passed).toList();
    }

    public int goodPassed() {
        return (int) goods().stream().filter(this::passed).count();
    }

    public int goodCount() {
        return goods().size();
    }

    public int defectsNotPassed() {
        return defectCount() - falsePasses();
    }

    public int defectCount() {
        return defects().size();
    }

    /** One row per defect class in declaration order, also for a class without images. */
    public List<ClassRow> classRows() {
        return Arrays.stream(Label.values()).filter(Label::isDefect).map(this::rowOf).toList();
    }

    public int judged() {
        return (int) outcomes.stream().filter(outcome -> outcome.outcome() instanceof JevOutcome.Judged).count();
    }

    public int jevErrors() {
        return outcomes.size() - judged();
    }

    /** The cost of judged attempts; failed attempts are unknown and not counted (DDD-15). */
    public double jevCost() {
        return outcomes.stream().mapToDouble(Evaluation::costOf).sum();
    }

    public boolean costIsLowerBound() {
        return outcomes.stream().anyMatch(outcome -> outcome.outcome().hadFailedAttempt());
    }

    /** Every hundredth from 0.50 to 0.99 plus the configured threshold, recomputed from the stored outcomes. */
    public ThresholdSweep sweep() {
        TreeSet<Double> thresholds = new TreeSet<>();
        IntStream.rangeClosed(SWEEP_FIRST_HUNDREDTH, SWEEP_LAST_HUNDREDTH).forEach(hundredths -> thresholds.add(hundredths / 100.0));
        thresholds.add(passThreshold());
        return ThresholdSweep.of(thresholds.stream().map(this::sweepRowAt).toList());
    }

    private ThresholdSweep.Row sweepRowAt(double threshold) {
        Evaluation atThreshold = of(outcomes, threshold);
        return new ThresholdSweep.Row(threshold, atThreshold.falsePasses(), atThreshold.goodPassed(), atThreshold.goodCount());
    }

    private ClassRow rowOf(Label label) {
        List<LabelledOutcome> ofLabel = outcomes.stream().filter(outcome -> outcome.label() == label).toList();
        int passed = (int) ofLabel.stream().filter(this::passed).count();
        int suspectedCorrect = (int) ofLabel.stream().filter(LabelledOutcome::suspectedClassCorrect).count();
        return new ClassRow(label, ofLabel.size(), ofLabel.size() - passed, suspectedCorrect, passed);
    }

    private boolean passed(LabelledOutcome outcome) {
        return verdictOf(outcome) == Verdict.PASS;
    }

    private List<LabelledOutcome> goods() {
        return outcomes.stream().filter(outcome -> !outcome.label().isDefect()).toList();
    }

    private List<LabelledOutcome> defects() {
        return outcomes.stream().filter(outcome -> outcome.label().isDefect()).toList();
    }

    private static double costOf(LabelledOutcome outcome) {
        return outcome.outcome() instanceof JevOutcome.Judged judged ? judged.judgement().cost() : 0.0;
    }
}
