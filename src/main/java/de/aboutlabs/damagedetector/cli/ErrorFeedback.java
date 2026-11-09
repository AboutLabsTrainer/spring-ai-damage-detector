package de.aboutlabs.damagedetector.cli;

import de.aboutlabs.damagedetector.probe.ProbeResult;
import java.util.List;
import java.util.Locale;

/** The one pure mapping from a refusal or an exception to one stderr line and an exit code. It prints nothing (ADR-005). */
public class ErrorFeedback {

    public record Feedback(String line, int exitCode) {
    }

    private static final int REFUSED = 1;
    private static final int FALSE_PASS = 2;
    private static final int NOTHING_JUDGED = 3;

    public Feedback feedbackFor(ProbeResult.Refused refusal) {
        List<String> details = refusal.details();
        String line = switch (refusal.reason()) {
            case USAGE -> "Usage: java -jar damage-detector.jar [--evaluate] --input=<folder> --output=<folder> ("
                + details.getFirst() + ")";
            case INPUT_MISSING -> "Input folder does not exist: " + details.getFirst();
            case NO_IMAGES -> "No PNG/JPEG images below " + details.getFirst();
            case NO_LABELLED_IMAGES -> "No labelled images below " + details.getFirst()
                + ": expected sub-folders good, bent_lead, cut_lead, damaged_case, misplaced";
            case OUTPUT_SAME_AS_OR_INSIDE_INPUT -> "Output folder " + details.get(0) + " is the same as or inside input folder "
                + details.get(1) + ": choose an output outside the input";
            case OUTPUT_NOT_WRITABLE -> "Cannot write to output folder " + details.get(0) + ": " + details.get(1);
            case NO_API_KEY -> "No Jev API key: set the OPENROUTER_API_KEY environment variable";
            case JEV_BASE_URL_INVALID -> "Jev base URL cannot be used: set detector.jev.base-url to an http or https address";
            case THRESHOLD_INVALID -> "Pass threshold must be a number from 0 to 1: set detector.pass-threshold (was "
                + details.getFirst() + ")";
        };
        return new Feedback(line, REFUSED);
    }

    public Feedback feedbackFor(Throwable failure) {
        return new Feedback(feedbackBeforeLog(failure).line() + " See log for details.", REFUSED);
    }

    /** Before bootstrap step 5 no log exists, so the line has no "See log" clause. */
    public Feedback feedbackBeforeLog(Throwable failure) {
        return new Feedback("Inspection failed: " + shortCause(failure) + ".", REFUSED);
    }

    private static String shortCause(Throwable failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return failure.getClass().getSimpleName();
        }
        return message.lines().findFirst().orElseThrow();
    }

    /** The exit-2 line (Q7). {@code reportPath} is shown as the user typed the output folder. */
    public Feedback falsePasses(int count, double threshold, String reportPath) {
        return new Feedback(String.format(Locale.ROOT, "%d false passes at pass threshold %.2f: see %s", count, threshold, reportPath),
            FALSE_PASS);
    }

    public Feedback noImageJudged(int total) {
        return new Feedback("0 of " + total + " images judged by Jev - check OPENROUTER_API_KEY and network", NOTHING_JUDGED);
    }
}
