// SCAFFOLD: true
package de.aboutlabs.damagedetector.cli;

import de.aboutlabs.damagedetector.probe.ProbeResult;

/** The one pure mapping from a refusal or an exception to one stderr line and an exit code. It prints nothing (ADR-005). */
public class ErrorFeedback {

    public record Feedback(String line, int exitCode) {
    }

    public Feedback feedbackFor(ProbeResult.Refused refusal) {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    public Feedback feedbackFor(Throwable failure) {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    /** Before bootstrap step 5 no log exists, so the line has no "See log" clause. */
    public Feedback feedbackBeforeLog(Throwable failure) {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    /** The exit-2 line (Q7). {@code reportPath} is shown as the user typed the output folder. */
    public Feedback falsePasses(int count, double threshold, String reportPath) {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    public Feedback noImageJudged(int total) {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }

    /** Exit code of a completed run; precedence 3 over 2 (exit 1 never reaches this point). */
    public int exitCodeOfCompletedRun(int judged, int falsePasses) {
        throw new AssertionError("Not yet implemented -- RED scaffold");
    }
}
