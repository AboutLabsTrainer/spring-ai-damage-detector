package de.aboutlabs.damagedetector.domain;

import java.util.List;

/** All inspections of one batch run plus the summary figures QS reports on. */
public record BatchReport(List<Inspection> inspections) {

    public BatchReport {
        inspections = List.copyOf(inspections);
    }

    public long count(Verdict verdict) {
        return inspections.stream().filter(inspection -> inspection.verdict() == verdict).count();
    }

    public double reviewShare() {
        return inspections.isEmpty() ? 0.0 : (double) count(Verdict.REVIEW) / inspections.size();
    }

    public long judged() {
        return inspections.stream().filter(Inspection::judged).count();
    }

    public long jevErrors() {
        return inspections.size() - judged();
    }

    public double jevCost() {
        return inspections.stream().mapToDouble(Inspection::cost).sum();
    }

    public boolean costIsLowerBound() {
        return inspections.stream().anyMatch(inspection -> inspection.outcome().hadFailedAttempt());
    }

    public List<Inspection> notPassed() {
        return inspections.stream().filter(inspection -> inspection.verdict() != Verdict.PASS).toList();
    }
}
