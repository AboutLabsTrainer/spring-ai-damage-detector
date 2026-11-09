package de.aboutlabs.damagedetector.acceptance.steps;

import de.aboutlabs.damagedetector.acceptance.support.DetectorWorld;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ImagePath;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ImageVerdict;
import io.cucumber.java.en.Then;

/** What Petra finds after a batch run. */
public class BatchReportSteps {

    private final DetectorWorld world;

    public BatchReportSteps(DetectorWorld world) {
        this.world = world;
    }

    @Then("the inspection report counts {int} PASS, {int} SCRAP, {int} REVIEW and {int} Jev error(s)")
    public void theReportCounts(int pass, int scrap, int review, int jevErrors) {
        world.batch().counts(pass, scrap, review, jevErrors);
    }

    @Then("{int} of the images were judged by Jev")
    public void imagesWereJudged(int images) {
        world.batch().judged(images);
    }

    @Then("{image} is documented as {verdict} with the description {string}")
    public void isDocumented(ImagePath image, ImageVerdict verdict, String description) {
        world.batch().documentedAs(image, verdict, description);
    }

    @Then("{image} is documented as {verdict} with a description starting {string}")
    public void isDocumentedStarting(ImagePath image, ImageVerdict verdict, String start) {
        world.batch().documentedWithDescriptionStarting(image, verdict, start);
    }

    @Then("{image} is marked as a Jev error without a Jev choice")
    public void isMarkedAsJevError(ImagePath image) {
        world.batch().markedAsJevError(image);
    }

    @Then("the reported Jev cost is {cost}")
    public void theReportedCostIs(String amount) {
        world.batch().cost(amount);
    }

    @Then("the cost is shown as a lower bound")
    public void theCostIsALowerBound() {
        world.batch().costIsALowerBound();
    }

    @Then("the cost is not shown as a lower bound")
    public void theCostIsExact() {
        world.batch().costIsExact();
    }

    @Then("the inspection report states the pass threshold {double}")
    public void theReportStatesTheThreshold(double threshold) {
        world.batch().statesThreshold(threshold);
    }

    @Then("the output folder holds no finished report")
    public void theOutputHoldsNoFinishedReport() {
        world.batch().isIncomplete();
    }

    @Then("the details of the failure are in the log")
    public void theDetailsAreInTheLog() {
        world.batch().logHoldsTheFailureDetails();
    }

    @Then("the marked key appears nowhere in the console output, the log or the reports")
    public void theMarkedKeyAppearsNowhere() {
        world.batch().nothingContains(world.run().apiKeyInUse());
    }
}
