package de.aboutlabs.damagedetector.acceptance.steps;

import de.aboutlabs.damagedetector.acceptance.support.DetectorWorld;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ApiKey;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.FolderContent;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ImagePath;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.OutputObstacle;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.time.LocalDate;
import java.util.List;

/** Preconditions of a run: the folders, the settings and the date. */
public class FolderSteps {

    private static final String EARLIER_NOTE = "written by Petra before the run";

    private final DetectorWorld world;

    public FolderSteps(DetectorWorld world) {
        this.world = world;
    }

    @Given("a labelled folder with the images {images}")
    public void aLabelledFolder(List<ImagePath> images) {
        world.folders().placeAll(images);
    }

    @Given("an input folder with the images {images}")
    public void anInputFolder(List<ImagePath> images) {
        world.folders().placeAll(images);
    }

    @Given("the labelled folder gains the image {image}")
    public void theLabelledFolderGains(ImagePath image) {
        world.folders().place(image);
    }

    @Given("a folder that holds {folderContent}")
    public void aFolderThatHolds(FolderContent content) {
        world.folders().fillWith(content);
    }

    @Given("the input folder also holds the file {word}")
    public void theInputFolderAlsoHolds(String fileName) {
        world.folders().writeInput(fileName, EARLIER_NOTE);
    }

    @Given("the output folder already holds the file {word}")
    public void theOutputFolderAlreadyHolds(String fileName) {
        world.folders().writeOutput(fileName, EARLIER_NOTE);
    }

    @Given("the output folder already holds a folder named {word}")
    public void theOutputFolderAlreadyHoldsAFolder(String folderName) {
        world.folders().folderInOutput(folderName);
    }

    @Given("the output folder {outputObstacle}")
    public void theOutputFolderCannotBeWritten(OutputObstacle obstacle) {
        world.folders().obstruct(obstacle);
    }

    @Given("today is {isoDate}")
    public void todayIs(LocalDate date) {
        world.capabilities().today(date);
    }

    @Given("a batch may try {int} times per image")
    public void aBatchMayTry(int attempts) {
        world.run().attemptsPerImage(attempts);
    }

    @Given("Jev gets {int} s to answer")
    public void jevGetsTimeToAnswer(int seconds) {
        world.run().answerTimeLimitSeconds(seconds);
    }

    @Given("the API key is {apiKey}")
    public void theApiKeyIs(ApiKey key) {
        world.run().apiKey(key);
    }

    @Given("the Jev address is {string}")
    public void theJevAddressIs(String address) {
        world.run().jevAddress(address);
    }

    @Then("the file {word} in the output folder is unchanged")
    public void theFileIsUnchanged(String fileName) {
        world.batch().keptOutputFile(fileName, EARLIER_NOTE);
    }
}
