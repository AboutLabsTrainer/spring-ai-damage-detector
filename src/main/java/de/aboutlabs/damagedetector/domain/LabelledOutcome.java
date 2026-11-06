// SCAFFOLD: true
package de.aboutlabs.damagedetector.domain;

/** What one labelled image scored: its relative path, its ground truth and what Jev said. */
public record LabelledOutcome(String image, Label label, JevOutcome outcome) {
}
