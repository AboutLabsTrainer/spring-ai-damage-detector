package de.aboutlabs.damagedetector.jev;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** The Jev question and class criteria validated in the spike (variant A). Changing these texts changes the classifier. */
final class DefectQuestion {

    static final String KEY = "defect";

    static final String SCENE = "Top-view inspection photo of a through-hole transistor placed on a copper stripboard "
        + "(vertical copper strips with a regular grid of round dark holes).";

    static final String INSTRUCTIONS = SCENE + " Classify the inspected part into exactly one class. Inspect the body "
        + "outline and each of the three leads from body to tip. Choose GOOD only if there is no defect at all.";

    static final Map<String, String> CRITERIA = criteria();

    private DefectQuestion() {
    }

    private static Map<String, String> criteria() {
        Map<String, String> criteria = new LinkedHashMap<>();
        criteria.put("GOOD", "Flawless part. Black TO-92 transistor body (flat front face, often with a round marking) is intact "
            + "with clean straight edges, sits upright, centred and not rotated above the hole rows. Exactly three "
            + "full-length metal leads exit the bottom edge; the outer two are symmetrically joggled/bent outward and "
            + "all three end inside three adjacent holes of the same row. Any doubt about any lead or the body means NOT good.");
        criteria.put("MISPLACED", "Transistor body is shifted sideways or vertically, rotated/tilted, or missing entirely, so the "
            + "part is not where it belongs and the lead tips do not end inside the three target holes "
            + "(e.g. leads end on copper between holes), even though the leads themselves may look normal.");
        criteria.put("DAMAGED_CASE", "The black plastic body is damaged: chipped corner or edge, crack, broken-off piece, melted, "
            + "dented or deformed shape, exposed lighter material or missing chunk of the case.");
        criteria.put("CUT_LEAD", "At least one lead is cut off / shortened: it ends abruptly (often with a ragged or flat cut "
            + "tip) before reaching its hole, so it is visibly shorter than the others and does not reach the hole row.");
        criteria.put("BENT_LEAD", "At least one lead is bent in a wrong direction: it is deformed sideways or outward so its tip "
            + "points away from or past its hole and ends outside the hole (e.g. on copper or beyond the hole row), "
            + "while the body itself is in the normal position.");
        return Collections.unmodifiableMap(criteria);
    }
}
