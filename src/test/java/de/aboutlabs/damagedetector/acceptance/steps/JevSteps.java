package de.aboutlabs.damagedetector.acceptance.steps;

import de.aboutlabs.damagedetector.acceptance.support.DetectorWorld;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.ImagePath;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.JevClass;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.JevError;
import de.aboutlabs.damagedetector.acceptance.support.DomainTypes.UnreadableAnswer;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.util.List;

/** What Jev answers, and what the detector asked it. */
public class JevSteps {

    private final DetectorWorld world;

    public JevSteps(DetectorWorld world) {
        this.world = world;
    }

    @Given("Jev judges {image} {jevClass} with P\\(GOOD) {double}")
    public void jevJudgesWithProbability(ImagePath image, JevClass choice, double pGood) {
        world.jev().judges(image, choice, pGood);
    }

    @Given("Jev judges {image} {jevClass}")
    public void jevJudges(ImagePath image, JevClass choice) {
        world.jev().judges(image, choice);
    }

    @Given("Jev fails with {jevError} on every attempt for {image}")
    public void jevFailsOnEveryAttempt(JevError error, ImagePath image) {
        world.jev().failsOnEveryAttempt(image, error);
    }

    @Given("Jev fails once with {jevError} and then judges {image} {jevClass}")
    public void jevFailsOnce(JevError error, ImagePath image, JevClass choice) {
        world.jev().failsOnceThenJudges(image, error, choice);
    }

    @Given("Jev rejects every call with {jevError}")
    public void jevRejectsEveryCall(JevError error) {
        world.jev().rejectsEveryCall(error);
    }

    @Given("Jev asks to wait {int} s with {jevError} and then judges {image} {jevClass}")
    public void jevAsksToWaitThenJudges(int seconds, JevError error, ImagePath image, JevClass choice) {
        world.jev().asksToWaitThenJudges(image, seconds, error, choice);
    }

    @Given("Jev asks to wait {int} s with {jevError} on every attempt for {image}")
    public void jevAsksToWait(int seconds, JevError error, ImagePath image) {
        world.jev().asksToWaitOnEveryAttempt(image, seconds, error);
    }

    @Given("Jev does not answer in time for {image}")
    public void jevDoesNotAnswerInTime(ImagePath image) {
        world.jev().answersTooLate(image);
    }

    @Given("Jev is still thinking about {image}")
    public void jevIsStillThinking(ImagePath image) {
        world.jev().keepsThinkingAbout(image);
    }

    @Given("the connection to Jev breaks on every attempt for {image}")
    public void theConnectionBreaks(ImagePath image) {
        world.jev().breaksTheConnection(image);
    }

    @Given("Jev cannot be reached at all")
    public void jevCannotBeReached() {
        world.jev().cannotBeReached();
    }

    @Given("Jev answers {image} with {unreadableAnswer} on every attempt")
    public void jevAnswersUnreadably(ImagePath image, UnreadableAnswer answer) {
        world.jev().answersUnreadably(image, answer);
    }

    @Given("Jev answers {image} with {unreadableAnswer} once and then judges it {jevClass}")
    public void jevAnswersUnreadablyOnce(ImagePath image, UnreadableAnswer answer, JevClass choice) {
        world.jev().answersUnreadablyOnceThenJudges(image, answer, choice);
    }

    @Then("Jev was called exactly {int} time(s)")
    public void jevWasCalled(int times) {
        world.jev().wasCalled(times);
    }

    @Then("no Jev call was made")
    public void noJevCallWasMade() {
        world.jev().wasCalled(0);
    }

    @Then("Jev was asked about {image} exactly {int} time(s)")
    public void jevWasAskedAbout(ImagePath image, int times) {
        world.jev().wasAskedAbout(image, times);
    }

    @Then("no request to Jev carries an image file name, a relative path, a label with a path separator or the input folder name")
    public void noRequestCarriesGroundTruth() {
        world.jev().carriedNoGroundTruth();
    }

    @Then("apart from the image data every request to Jev is identical")
    public void everyRequestIsIdentical() {
        world.jev().sentIdenticalRequestsApartFromTheImage();
    }

    @Then("the waits between attempts were {waits}")
    public void theWaitsWere(List<Long> seconds) {
        world.capabilities().waitedSeconds(seconds);
    }

    @Then("no wait happened between attempts")
    public void noWaitHappened() {
        world.capabilities().neverWaited();
    }
}
