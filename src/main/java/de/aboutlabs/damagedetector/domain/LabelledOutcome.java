package de.aboutlabs.damagedetector.domain;

/** What one labelled image scored: its relative path, its ground truth and what Jev said. */
public record LabelledOutcome(String image, Label label, JevOutcome outcome) {

    /** Whether Jev's chosen class names the ground truth; a failed image never does. */
    public boolean suspectedClassCorrect() {
        return outcome instanceof JevOutcome.Judged judged && judged.judgement().choice().equals(label.name());
    }
}
