// SCAFFOLD: true
package de.aboutlabs.damagedetector.domain;

/** Ground truth of a labelled image. It is used only for scoring and never reaches Jev (C11). */
public enum Label {
    GOOD,
    BENT_LEAD,
    CUT_LEAD,
    DAMAGED_CASE,
    MISPLACED;

    public String folderName() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    public boolean isDefect() {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }
}
