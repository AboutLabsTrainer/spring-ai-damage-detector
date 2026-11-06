package de.aboutlabs.damagedetector.acceptance.support;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

/** The nouns of the Gherkin, typed once (Mandate 12). Each phrase enum maps the exact wording of the feature files. */
public final class DomainTypes {

    private DomainTypes() {
    }

    /** A relative image path below the input folder. */
    public record ImagePath(String value) {

        public static List<ImagePath> listOf(String phrase) {
            return Arrays.stream(phrase.split(", | and ")).map(ImagePath::new).toList();
        }

        public String fileName() {
            return value.substring(value.lastIndexOf('/') + 1);
        }

        public boolean isGood() {
            return value.startsWith("good");
        }

        public String mediaType() {
            return value.toLowerCase(java.util.Locale.ROOT).endsWith(".png") ? "image/png" : "image/jpeg";
        }
    }

    /** The classes Jev chooses from. */
    public enum JevClass {
        GOOD, MISPLACED, DAMAGED_CASE, CUT_LEAD, BENT_LEAD;

        public boolean isGroundTruthOf(ImagePath image) {
            return image.value().startsWith(name().toLowerCase(java.util.Locale.ROOT) + "/");
        }
    }

    /** The ground truth, as a folder name. Only the tests and the evaluation scoring know it. */
    public enum GroundTruth {
        GOOD("good"), BENT_LEAD("bent_lead"), CUT_LEAD("cut_lead"), DAMAGED_CASE("damaged_case"), MISPLACED("misplaced");

        private final String folderName;

        GroundTruth(String folderName) {
            this.folderName = folderName;
        }

        public String folderName() {
            return folderName;
        }

        public static GroundTruth ofFolder(String folderName) {
            return Arrays.stream(values()).filter(label -> label.folderName.equals(folderName)).findFirst().orElseThrow();
        }

        public static List<String> folderNames() {
            return Arrays.stream(values()).map(GroundTruth::folderName).toList();
        }
    }

    public enum ImageVerdict {
        PASS, SCRAP, REVIEW;

        public String folder() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** An error answer of Jev, as the feature files quote it: "503 Service Unavailable". */
    public record JevError(int status, String reasonPhrase) {
    }

    /** How a run ended, in the user's words; the exit codes of ADR-005. */
    public enum RunEnding {
        SUCCESS("successfully", 0),
        REFUSAL("with the refusal signal", 1),
        FALSE_PASS("with the false-pass signal", 2),
        NOTHING_JUDGED("with the nothing-judged signal", 3);

        private final String phrase;
        private final int exitCode;

        RunEnding(String phrase, int exitCode) {
            this.phrase = phrase;
            this.exitCode = exitCode;
        }

        public int exitCode() {
            return exitCode;
        }

        public static RunEnding ofPhrase(String phrase) {
            return Arrays.stream(values()).filter(ending -> ending.phrase.equals(phrase)).findFirst().orElseThrow();
        }
    }

    /** The declared write universe per run type (brief.md, DDD-29). */
    public enum RunDeclaration {
        COMPLETED_BATCH("a completed batch", List.of("report.md", "report.json", "damage-detector.log"),
            List.of("scrap/", "review/")),
        COMPLETED_EVALUATION("a completed evaluation", List.of("evaluation.md", "evaluation.json", "damage-detector.log"),
            List.of()),
        REFUSED_AFTER_START("a refusal after the start", List.of("damage-detector.log"), List.of()),
        REFUSED_BEFORE_WRITING("a refusal before writing", List.of(), List.of()),
        INTERRUPTED("an interrupted run", List.of("damage-detector.log"), List.of("scrap/", "review/"));

        private final String phrase;
        private final List<String> files;
        private final List<String> folders;

        RunDeclaration(String phrase, List<String> files, List<String> folders) {
            this.phrase = phrase;
            this.files = files;
            this.folders = folders;
        }

        public List<String> requiredFiles() {
            return files;
        }

        public boolean allows(String relativePath) {
            return files.contains(relativePath) || folders.stream().anyMatch(relativePath::startsWith)
                || (this == INTERRUPTED && (relativePath.endsWith(".tmp") || relativePath.equals("report.json")));
        }

        /** An interrupt in the final write phase may leave report.json and one temp file; other runs leave no temp file (INV-6). */
        public int tempFilesAllowed() {
            return this == INTERRUPTED ? 1 : 0;
        }

        public String describe() {
            return "declared for " + phrase;
        }

        public static RunDeclaration ofPhrase(String phrase) {
            return Arrays.stream(values()).filter(declaration -> declaration.phrase.equals(phrase)).findFirst().orElseThrow();
        }
    }

    /** Answers that are well-formed enough to arrive, but must not become a verdict (DDD-26). */
    public enum UnreadableAnswer {
        NO_PROBABILITIES("no probabilities", answer("\"choice\": \"GOOD\", \"confidence\": 0.4")),
        EMPTY_P_GOOD("an empty P(GOOD)", answer("\"choice\": \"GOOD\", \"probabilities\": {\"GOOD\": null}, \"confidence\": 0.4")),
        NO_P_GOOD("no P(GOOD)", answer("\"choice\": \"GOOD\", \"probabilities\": {\"CUT_LEAD\": 0.1}, \"confidence\": 0.4")),
        P_GOOD_TOO_HIGH("a P(GOOD) of 1.7", answer("\"choice\": \"GOOD\", \"probabilities\": {\"GOOD\": 1.7}, \"confidence\": 0.4")),
        P_GOOD_NEGATIVE("a P(GOOD) of -0.2", answer("\"choice\": \"GOOD\", \"probabilities\": {\"GOOD\": -0.2}, \"confidence\": 0.4")),
        P_GOOD_NOT_A_NUMBER("a P(GOOD) that is not a number", answer("\"choice\": \"GOOD\", \"probabilities\": {\"GOOD\": NaN}, \"confidence\": 0.4")),
        UNKNOWN_CLASS("the unknown class SCRATCHED", answer("\"choice\": \"SCRATCHED\", \"probabilities\": {\"GOOD\": 0.9, \"SCRATCHED\": 0.1}, \"confidence\": 0.4")),
        NO_DEFECT_ANSWER("no defect answer", "{\"answers\": {}, \"usage\": {\"cost\": 0.0001}}"),
        GARBLED("a garbled reply", "<html>Bad gateway page</html>"),
        EMPTY("an empty reply", "");

        private final String phrase;
        private final String body;

        UnreadableAnswer(String phrase, String body) {
            this.phrase = phrase;
            this.body = body;
        }

        public ResponseDefinitionBuilder reply() {
            return aResponse().withHeader("Content-Type", "application/json").withBody(body);
        }

        public static UnreadableAnswer ofPhrase(String phrase) {
            return Arrays.stream(values()).filter(answer -> answer.phrase.equals(phrase)).findFirst().orElseThrow();
        }

        private static String answer(String defect) {
            return "{\"answers\": {\"defect\": {\"type\": \"choice\", " + defect + "}}, \"usage\": {\"cost\": 0.0001}}";
        }
    }

    /** What is wrong with a command line (INV-5). */
    public enum StartMistake {
        NO_INPUT("without an input folder"),
        NO_OUTPUT("without an output folder"),
        INPUT_DOES_NOT_EXIST("with an input folder that does not exist"),
        INPUT_IS_A_FILE("with a file as the input folder"),
        OUTPUT_EQUALS_INPUT("with the output folder equal to the input folder"),
        OUTPUT_INSIDE_INPUT("with the output folder inside the input folder"),
        OUTPUT_CLIMBS_BACK("with an output folder that climbs back into the input folder"),
        OUTPUT_THROUGH_SHORTCUT("with an output folder reached through a shortcut into the input"),
        OUTPUT_THROUGH_MISSING_FOLDER("with an output folder that climbs into the input through a missing folder"),
        MISSPELT_OPTION("with the misspelt option --evalute"),
        VALUE_AFTER_EVALUATE("with a value after --evaluate");

        private final String phrase;

        StartMistake(String phrase) {
            this.phrase = phrase;
        }

        public static StartMistake ofPhrase(String phrase) {
            return Arrays.stream(values()).filter(mistake -> mistake.phrase.equals(phrase)).findFirst().orElseThrow();
        }
    }

    /** Why the output folder cannot be written. */
    public enum OutputObstacle {
        READ_ONLY("is read-only"), IS_A_FILE("is a file"), BELOW_A_FILE("lies below a file");

        private final String phrase;

        OutputObstacle(String phrase) {
            this.phrase = phrase;
        }

        public static OutputObstacle ofPhrase(String phrase) {
            return Arrays.stream(values()).filter(obstacle -> obstacle.phrase.equals(phrase)).findFirst().orElseThrow();
        }
    }

    /** What a folder holds when it has nothing to inspect. */
    public enum FolderContent {
        NOTHING("nothing at all"), ONLY_UNKNOWN_FOLDERS("only images in unknown folders"), ONLY_NOTES("only notes");

        private final String phrase;

        FolderContent(String phrase) {
            this.phrase = phrase;
        }

        public static FolderContent ofPhrase(String phrase) {
            return Arrays.stream(values()).filter(content -> content.phrase.equals(phrase)).findFirst().orElseThrow();
        }
    }

    /** How a running batch is stopped from outside; the exit codes come from the JVM (DEVOPS interrupt contract). */
    public enum Interruption {
        CTRL_C("Ctrl-C", "INT", 130), STOP_REQUEST("a stop request", "TERM", 143);

        private final String phrase;
        private final String signal;
        private final int exitCode;

        Interruption(String phrase, String signal, int exitCode) {
            this.phrase = phrase;
            this.signal = signal;
            this.exitCode = exitCode;
        }

        public String signal() {
            return signal;
        }

        public int exitCode() {
            return exitCode;
        }

        public static Interruption ofPhrase(String phrase) {
            return Arrays.stream(values()).filter(interruption -> interruption.phrase.equals(phrase)).findFirst().orElseThrow();
        }
    }

    public enum ApiKey {
        MISSING("missing"), BLANK("blank"), MARKED("a marked key");

        private final String phrase;

        ApiKey(String phrase) {
            this.phrase = phrase;
        }

        public static ApiKey ofPhrase(String phrase) {
            return Arrays.stream(values()).filter(key -> key.phrase.equals(phrase)).findFirst().orElseThrow();
        }
    }

    /** The checks of scripts/container-check.sh. */
    public enum ContainerCheckId {
        IMAGE_SCAN("image-scan"), PARITY("parity"), EVALUATION("evaluation"), RUNTIME_KEY("runtime-key"),
        MISSING_KEY("missing-key"), EXIT_CODES("exit-codes"), OWNERSHIP("ownership"), FORGOTTEN_MOUNT("forgotten-mount");

        private final String id;

        ContainerCheckId(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    /** The named findings of scripts/container-check.sh. */
    public enum ContainerFinding {
        IMAGE_HOLDS_NO_KEY("image-holds-no-key"), REAL_ENV_UNTOUCHED("real-env-untouched"), REPORT_FILES("report-files"), VERDICT_COUNTS_EQUAL("verdict-counts-equal"),
        EVALUATION_FILES("evaluation-files"), NO_KEY_IN_RUN_OUTPUT("no-key-in-run-output"),
        MISSING_KEY_REFUSED("missing-key-refused"), NOTHING_JUDGED_SIGNAL("nothing-judged-signal"),
        FALSE_PASS_SIGNAL("false-pass-signal"), OUTPUT_DELETABLE("output-deletable"),
        FORGOTTEN_MOUNT_REFUSED("forgotten-mount-refused");

        private final String id;

        ContainerFinding(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }
}
