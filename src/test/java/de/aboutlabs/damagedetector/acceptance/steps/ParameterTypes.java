package de.aboutlabs.damagedetector.acceptance.steps;

import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ApiKey;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.FolderContent;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.GroundTruth;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ImagePath;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ImageVerdict;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.Interruption;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.JevClass;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.JevError;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.OutputObstacle;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.RunDeclaration;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.RunEnding;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.StartMistake;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.UnreadableAnswer;
import io.cucumber.java.ParameterType;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** Turns Gherkin wording into typed domain values, so step methods never see a raw string where a type exists. */
public class ParameterTypes {

    private static final String IMAGE = "[A-Za-z0-9_./-]+\\.(?:png|jpg|JPG|jpeg)";

    @ParameterType(IMAGE)
    public ImagePath image(String path) {
        return new ImagePath(path);
    }

    @ParameterType(IMAGE + "(?:(?:, | and )" + IMAGE + ")*")
    public List<ImagePath> images(String paths) {
        return ImagePath.listOf(paths);
    }

    @ParameterType("GOOD|MISPLACED|DAMAGED_CASE|CUT_LEAD|BENT_LEAD")
    public JevClass jevClass(String name) {
        return JevClass.valueOf(name);
    }

    @ParameterType("good|bent_lead|cut_lead|damaged_case|misplaced")
    public GroundTruth label(String folderName) {
        return GroundTruth.ofFolder(folderName);
    }

    @ParameterType("PASS|SCRAP|REVIEW")
    public ImageVerdict verdict(String name) {
        return ImageVerdict.valueOf(name);
    }

    @ParameterType("\"(\\d{3}) ([^\"]+)\"")
    public JevError jevError(String status, String reasonPhrase) {
        return new JevError(Integer.parseInt(status), reasonPhrase);
    }

    @ParameterType("successfully|with the refusal signal|with the false-pass signal|with the nothing-judged signal")
    public RunEnding runEnding(String phrase) {
        return RunEnding.ofPhrase(phrase);
    }

    @ParameterType("a completed batch|a completed evaluation|a refusal after the start|a refusal before writing|an interrupted run")
    public RunDeclaration runDeclaration(String phrase) {
        return RunDeclaration.ofPhrase(phrase);
    }

    @ParameterType("no probabilities|an empty P\\(GOOD\\)|no P\\(GOOD\\)|a P\\(GOOD\\) of 1\\.7|a P\\(GOOD\\) of -0\\.2"
        + "|a P\\(GOOD\\) that is not a number|the unknown class SCRATCHED|no defect answer|a garbled reply|an empty reply")
    public UnreadableAnswer unreadableAnswer(String phrase) {
        return UnreadableAnswer.ofPhrase(phrase);
    }

    @ParameterType("without an input folder|without an output folder|with an input folder that does not exist"
        + "|with a file as the input folder|with the output folder equal to the input folder"
        + "|with the output folder inside the input folder|with an output folder that climbs back into the input folder"
        + "|with an output folder reached through a shortcut into the input"
        + "|with an output folder that climbs into the input through a missing folder|with the misspelt option --evalute"
        + "|with a value after --evaluate")
    public StartMistake startMistake(String phrase) {
        return StartMistake.ofPhrase(phrase);
    }

    @ParameterType("is read-only|is a file|lies below a file")
    public OutputObstacle outputObstacle(String phrase) {
        return OutputObstacle.ofPhrase(phrase);
    }

    @ParameterType("nothing at all|only images in unknown folders|only notes")
    public FolderContent folderContent(String phrase) {
        return FolderContent.ofPhrase(phrase);
    }

    @ParameterType("missing|blank|a marked key")
    public ApiKey apiKey(String phrase) {
        return ApiKey.ofPhrase(phrase);
    }

    @ParameterType("Ctrl-C|a stop request")
    public Interruption interruption(String phrase) {
        return Interruption.ofPhrase(phrase);
    }

    @ParameterType("\\$\\d+\\.\\d{5}")
    public String cost(String amount) {
        return amount;
    }

    @ParameterType("\\d{4}-\\d{2}-\\d{2}")
    public LocalDate isoDate(String date) {
        return LocalDate.parse(date);
    }

    @ParameterType("\\d+ s(?:, \\d+ s)*")
    public List<Long> waits(String phrase) {
        return Arrays.stream(phrase.split(", ")).map(wait -> Long.valueOf(wait.replace(" s", ""))).toList();
    }
}
