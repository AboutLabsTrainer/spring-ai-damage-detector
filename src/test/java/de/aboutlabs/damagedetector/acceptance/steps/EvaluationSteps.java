package de.aboutlabs.damagedetector.acceptance.steps;

import de.aboutlabs.damagedetector.acceptance.support.DetectorWorld;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.GroundTruth;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ImagePath;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ImageVerdict;
import io.cucumber.java.en.Then;
import java.time.LocalDate;

/** What Jonas finds after an evaluation. */
public class EvaluationSteps {

    private final DetectorWorld world;

    public EvaluationSteps(DetectorWorld world) {
        this.world = world;
    }

    @Then("the evaluation shows {int} false pass(es)")
    public void theEvaluationShowsFalsePasses(int count) {
        world.evaluation().falsePasses(count);
    }

    @Then("the evaluation shows good auto-pass {int} of {int}")
    public void theEvaluationShowsGoodAutoPass(int passed, int total) {
        world.evaluation().goodAutoPass(passed, total);
    }

    @Then("the evaluation shows recall {int} of {int} for {label}")
    public void theEvaluationShowsRecall(int notPassed, int total, GroundTruth label) {
        world.evaluation().recall(label, notPassed, total);
    }

    @Then("the suspected class of {image} is counted as not correct")
    public void theSuspectedClassIsNotCorrect(ImagePath image) {
        world.evaluation().suspectedClassNotCorrect(image);
    }

    @Then("the class table shows {label} with {int} image(s), {int} not passed, {int} suspected class correct and {int} passed")
    public void theClassTableShows(GroundTruth label, int images, int notPassed, int suspectedCorrect, int passed) {
        world.evaluation().classRow(label, images, notPassed, suspectedCorrect, passed);
    }

    @Then("the evaluation lists {image} as a false pass with P\\(GOOD) {double}")
    public void theEvaluationListsFalsePass(ImagePath image, double pGood) {
        world.evaluation().listsFalsePass(image, pGood);
    }

    @Then("{image} has the verdict {verdict} in the evaluation")
    public void hasTheVerdict(ImagePath image, ImageVerdict verdict) {
        world.evaluation().verdict(image, verdict);
    }

    @Then("the evaluation counts {int} image(s) as skipped without a label")
    public void theEvaluationCountsSkipped(int count) {
        world.evaluation().skippedWithoutLabel(count);
    }

    @Then("the evaluation lists {int} image(s)")
    public void theEvaluationListsImages(int count) {
        world.evaluation().listsImages(count);
    }

    @Then("{image} is marked as a Jev error in the evaluation")
    public void isMarkedAsJevErrorInTheEvaluation(ImagePath image) {
        world.evaluation().markedAsJevError(image);
    }

    @Then("the evaluation states a Jev cost of {cost}")
    public void theEvaluationStatesCost(String amount) {
        world.evaluation().cost(amount);
    }

    @Then("the evaluation states how many seconds the run took")
    public void theEvaluationStatesDuration() {
        world.evaluation().statesDuration();
    }

    @Then("the evaluation states the run date {isoDate}")
    public void theEvaluationStatesRunDate(LocalDate date) {
        world.evaluation().runDate(date.toString());
    }

    @Then("the evaluation ends with the attribution {string}")
    public void theEvaluationEndsWith(String attribution) {
        world.evaluation().endsWith(attribution);
    }

    @Then("the evaluation says {string}")
    public void theEvaluationSays(String sentence) {
        world.evaluation().says(sentence);
    }

    @Then("the evaluation says the lowest threshold with 0 false passes is {word}")
    public void theEvaluationNamesTheSafeThreshold(String threshold) {
        world.evaluation().lowestSafeThreshold(threshold);
    }

    @Then("the evaluation says no threshold is safe")
    public void theEvaluationSaysNoThresholdIsSafe() {
        world.evaluation().noThresholdIsSafe();
    }

    @Then("the evaluation counts {int} Jev error(s) and {int} image(s) judged")
    public void theEvaluationCountsJevErrors(int errors, int judged) {
        world.evaluation().countsJevErrors(errors, judged);
    }

    @Then("the evaluation shows its cost as a lower bound")
    public void theEvaluationCostIsALowerBound() {
        world.evaluation().costIsALowerBound();
    }

    @Then("the earlier run date {isoDate} appears nowhere in the evaluation")
    public void theEarlierRunDateIsGone(LocalDate date) {
        world.evaluation().noLongerMentions(date.toString());
    }

    @Then("the sweep row {double} shows {int} false pass(es) and good auto-pass {int} of {int}")
    public void theSweepRowShows(double threshold, int falsePasses, int goodPassed, int goodTotal) {
        world.evaluation().sweepRow(threshold, falsePasses, goodPassed, goodTotal);
    }

    @Then("the sweep record holds every threshold from 0.50 to 0.99")
    public void theSweepRecordIsComplete() {
        world.evaluation().sweepCoversEveryHundredth();
    }

    @Then("the readable sweep table shows {double} to {double} and the configured threshold {double}")
    public void theReadableSweepShows(double first, double last, double configured) {
        world.evaluation().readableSweepShows(first, last, configured);
    }
}
