# Slice 04, US-04. These scenarios need Docker, so they stay out of ./mvnw verify (DEVOPS upstream change 3).
# The container check script is the runner: each When starts one of its checks, each Then reads one named finding.
# File names, paths and commands live in the step bodies and the script (Q9). Run with:
#   ./mvnw test -Dtest=WalkingSkeletonAcceptanceTest -Dacceptance.inprocess.tags=@container
Feature: Run the inspection and the evaluation from Docker
  As Jonas, the showcase presenter
  I want one documented Docker command to give the same report as the jar
  so that I can demo on any Docker host and share the image without sharing my key.

  @pending @container @US-04 @real-io @kpi-K4 @contract-shape:bounded-change
  Scenario: Presenter gets the usual report from Docker
    Given a good part and a part with a cut lead to inspect
    When Jonas runs the inspection in Docker
    Then the usual report and the scrap record of the cut lead appear on his machine
    And the verdict counts equal those of the jar run on the same Jev answers

  @pending @container @US-04 @real-io @kpi-K4 @contract-shape:bounded-change
  Scenario: Presenter gets the evaluation from Docker
    Given a good part and a part with a cut lead to inspect
    When Jonas runs the evaluation in Docker
    Then the usual evaluation appears on his machine

  @pending @container @US-04 @real-io @error @kpi-K4 @contract-shape:unbounded-preservation
  Scenario: The shared Docker image carries no API key
    When Jonas checks the Docker image he wants to share
    Then neither its configuration, its history nor its files hold an API key or a key file

  @pending @container @US-04 @real-io @error @contract-shape:unbounded-preservation
  Scenario: Checking the image leaves Jonas's own key file alone
    When Jonas checks the Docker image he wants to share
    Then his own key file is unchanged afterwards

  @pending @container @US-04 @real-io @error @contract-shape:unbounded-preservation
  Scenario: The API key leaves no trace of a Docker run
    Given a good part and a part with a cut lead to inspect
    When Jonas runs the inspection in Docker with a marked key and details on the console
    Then the marked key appears nowhere in the results or in what Docker printed

  @pending @container @US-04 @real-io @error @kpi-K6 @contract-shape:bounded-change
  Scenario: A missing key is reported before any work
    When Jonas runs the inspection in Docker without his API key
    Then he is told to set the key and the run ends with the refusal signal

  @pending @container @US-04 @real-io @error @contract-shape:bounded-change
  Scenario: Docker passes on the nothing-judged signal
    Given a good part and a part with a cut lead to inspect
    When Jonas compares the run endings of Docker and the jar
    Then Docker ends with the nothing-judged signal when Jev rejects every call

  @pending @container @US-04 @real-io @error @contract-shape:bounded-change
  Scenario: Docker passes on the false-pass signal
    Given a good part and a part with a cut lead to inspect
    When Jonas compares the run endings of Docker and the jar
    Then Docker ends with the false-pass signal when a defect passes in an evaluation

  @pending @container @US-04 @real-io @contract-shape:bounded-change
  Scenario: Presenter can delete what Docker wrote
    Given a good part and a part with a cut lead to inspect
    When Jonas deletes the results of a Docker run
    Then everything goes without extra rights

  @pending @container @US-04 @real-io @error @kpi-K6 @contract-shape:bounded-change
  Scenario: A forgotten output folder is refused
    Given a good part and a part with a cut lead to inspect
    When Jonas runs the inspection in Docker without an output folder
    Then he is told the output folder cannot be written and the run ends with the refusal signal
