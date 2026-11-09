package de.aboutlabs.damagedetector.domain;

import java.util.Locale;

/** Ground truth of a labelled image. It is used only for scoring and never reaches Jev (C11). */
public enum Label {
    GOOD,
    BENT_LEAD,
    CUT_LEAD,
    DAMAGED_CASE,
    MISPLACED;

    /** The sub-folder name below the input folder, e.g. {@code bent_lead}. */
    public String folderName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean isDefect() {
        return this != GOOD;
    }
}
