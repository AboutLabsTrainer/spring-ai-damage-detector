package de.aboutlabs.damagedetector.acceptance.steps;

import de.aboutlabs.damagedetector.acceptance.support.DetectorWorld;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ContainerCheckId;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ContainerFinding;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/** Each When starts one check of scripts/container-check.sh, each Then reads one of its named findings. */
public class ContainerSteps {

    private final DetectorWorld world;

    public ContainerSteps(DetectorWorld world) {
        this.world = world;
    }

    @Given("a good part and a part with a cut lead to inspect")
    public void theDemoImages() {
        world.container().usesTheDemoImages();
    }

    @When("Jonas runs the inspection in Docker")
    public void jonasRunsTheInspection() {
        world.container().runs(ContainerCheckId.PARITY);
    }

    @When("Jonas runs the evaluation in Docker")
    public void jonasRunsTheEvaluation() {
        world.container().runs(ContainerCheckId.EVALUATION);
    }

    @When("Jonas checks the Docker image he wants to share")
    public void jonasChecksTheImage() {
        world.container().runs(ContainerCheckId.IMAGE_SCAN);
    }

    @When("Jonas runs the inspection in Docker with a marked key and details on the console")
    public void jonasRunsWithAMarkedKey() {
        world.container().runs(ContainerCheckId.RUNTIME_KEY);
    }

    @When("Jonas runs the inspection in Docker without his API key")
    public void jonasRunsWithoutAKey() {
        world.container().runs(ContainerCheckId.MISSING_KEY);
    }

    @When("Jonas compares the run endings of Docker and the jar")
    public void jonasComparesTheRunEndings() {
        world.container().runs(ContainerCheckId.EXIT_CODES);
    }

    @When("Jonas deletes the results of a Docker run")
    public void jonasDeletesTheResults() {
        world.container().runs(ContainerCheckId.OWNERSHIP);
    }

    @When("Jonas runs the inspection in Docker without an output folder")
    public void jonasForgetsTheOutputFolder() {
        world.container().runs(ContainerCheckId.FORGOTTEN_MOUNT);
    }

    @Then("the usual report and the scrap record of the cut lead appear on his machine")
    public void theReportFilesAreThere() {
        world.container().confirms(ContainerFinding.REPORT_FILES);
    }

    @Then("the verdict counts equal those of the jar run on the same Jev answers")
    public void theVerdictCountsAreEqual() {
        world.container().confirms(ContainerFinding.VERDICT_COUNTS_EQUAL);
    }

    @Then("the usual evaluation appears on his machine")
    public void theEvaluationFilesAreThere() {
        world.container().confirms(ContainerFinding.EVALUATION_FILES);
    }

    @Then("neither its configuration, its history nor its files hold an API key or a key file")
    public void theImageHoldsNoKey() {
        world.container().confirms(ContainerFinding.IMAGE_HOLDS_NO_KEY);
    }

    @Then("his own key file is unchanged afterwards")
    public void theRealKeyFileIsUntouched() {
        world.container().confirms(ContainerFinding.REAL_ENV_UNTOUCHED);
    }

    @Then("the marked key appears nowhere in the results or in what Docker printed")
    public void theRunLeavesNoKey() {
        world.container().confirms(ContainerFinding.NO_KEY_IN_RUN_OUTPUT);
    }

    @Then("he is told to set the key and the run ends with the refusal signal")
    public void theMissingKeyIsRefused() {
        world.container().confirms(ContainerFinding.MISSING_KEY_REFUSED);
    }

    @Then("Docker ends with the nothing-judged signal when Jev rejects every call")
    public void dockerEndsWithNothingJudged() {
        world.container().confirms(ContainerFinding.NOTHING_JUDGED_SIGNAL);
    }

    @Then("Docker ends with the false-pass signal when a defect passes in an evaluation")
    public void dockerEndsWithFalsePass() {
        world.container().confirms(ContainerFinding.FALSE_PASS_SIGNAL);
    }

    @Then("everything goes without extra rights")
    public void theOutputCanBeDeleted() {
        world.container().confirms(ContainerFinding.OUTPUT_DELETABLE);
    }

    @Then("he is told the output folder cannot be written and the run ends with the refusal signal")
    public void theForgottenOutputIsRefused() {
        world.container().confirms(ContainerFinding.FORGOTTEN_MOUNT_REFUSED);
    }
}
