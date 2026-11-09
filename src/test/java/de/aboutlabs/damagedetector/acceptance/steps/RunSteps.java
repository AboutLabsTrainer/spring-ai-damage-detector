package de.aboutlabs.damagedetector.acceptance.steps;

import de.aboutlabs.damagedetector.acceptance.support.DetectorWorld;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ImagePath;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.Interruption;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.RunDeclaration;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.RunEnding;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.StartMistake;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/** Starting the detector, and what the user sees at the end of a run. */
public class RunSteps {

    private final DetectorWorld world;

    public RunSteps(DetectorWorld world) {
        this.world = world;
    }

    @Given("Jonas has an earlier evaluation of that folder")
    public void jonasHasAnEarlierEvaluation() {
        world.run().evaluation();
    }

    @When("Jonas/Petra runs the evaluation")
    public void runsTheEvaluation() {
        world.run().evaluation();
    }

    @When("Jonas/Petra runs the evaluation with pass threshold {double}")
    public void runsTheEvaluationAt(double threshold) {
        world.run().evaluationAt(threshold);
    }

    @When("Jonas/Petra runs the batch inspection with the pass threshold setting {word}")
    public void runsTheBatchWithSetting(String setting) {
        world.run().batchWithThresholdSetting(setting);
    }

    @When("Petra interrupts the batch inspection with {interruption} during {image}")
    public void interruptsTheBatch(Interruption interruption, ImagePath image) {
        world.run().batchInterruptedDuring(image, interruption);
    }

    @When("Jonas/Petra runs the batch inspection( as usual)")
    public void runsTheBatch() {
        world.run().batch();
    }

    @When("Jonas/Petra runs the batch inspection with pass threshold {double}")
    public void runsTheBatchAt(double threshold) {
        world.run().batchAt(threshold);
    }

    @When("Jonas/Petra runs the batch inspection with details on the console")
    public void runsTheBatchWithConsoleDetails() {
        world.run().batchWithConsoleDetails();
    }

    @When("Jonas/Petra starts the detector {startMistake}")
    public void startsTheDetector(StartMistake mistake) {
        world.run().startsWith(mistake);
    }

    @Then("the run ends {runEnding}")
    public void theRunEnds(RunEnding ending) {
        world.run().endedWith(ending);
    }

    @Then("Jonas/Petra is told {string}")
    public void isTold(String message) {
        world.run().toldExactly(message);
    }

    @Then("Jonas/Petra is told something starting with {string}")
    public void isToldSomethingStartingWith(String start) {
        world.run().toldSomethingStartingWith(start);
    }

    @Then("Jonas/Petra is told something ending with {string}")
    public void isToldSomethingEndingWith(String end) {
        world.run().toldSomethingEndingWith(end);
    }

    @Then("Jonas/Petra is told something containing {string}")
    public void isToldSomethingContaining(String fragment) {
        world.run().toldSomethingContaining(fragment);
    }

    @Then("the run ends interrupted by {interruption}")
    public void theRunEndsInterrupted(Interruption interruption) {
        world.run().endedInterruptedBy(interruption);
    }

    @Then("nothing is reported as a problem")
    public void nothingIsReportedAsAProblem() {
        world.run().reportedNoProblem();
    }

    @Then("the only summary line reads {string}")
    public void theOnlySummaryLineReads(String line) {
        world.run().onlySummaryLineIs(line);
    }

    @Then("the output folder holds only what {runDeclaration} declares")
    public void theOutputHoldsOnlyTheDeclaredFiles(RunDeclaration declaration) {
        world.run().changedOnly(declaration);
    }
}
