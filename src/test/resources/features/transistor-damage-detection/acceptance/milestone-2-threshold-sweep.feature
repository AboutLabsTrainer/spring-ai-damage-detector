# Slice 02, US-02. Jonas chooses the pass threshold from one Jev pass.
# The scenarios chain: each one moves the highest defect P(GOOD) of the scenario before it.
Feature: Pick a pass threshold that lets no defect through
  As Jonas, the showcase presenter
  I want to see false passes and auto-passed good parts for every threshold
  so that I choose the threshold from evidence, without paying for another run.

  @pending @US-02 @driving_port @real-io @kpi-K2 @contract-shape:pure-function
  Scenario: Presenter sees the trade-off between false passes and auto-passed good parts
    Given a labelled folder with the images damaged_case/005.png, good/000.png, good/001.png and good/017.png
    And Jev judges damaged_case/005.png GOOD with P(GOOD) 0.82
    And Jev judges good/000.png GOOD with P(GOOD) 0.91
    And Jev judges good/001.png GOOD with P(GOOD) 0.85
    And Jev judges good/017.png GOOD with P(GOOD) 0.88
    When Jonas runs the evaluation with pass threshold 0.86
    Then the sweep row 0.80 shows 1 false pass and good auto-pass 3 of 3
    And the sweep row 0.86 shows 0 false passes and good auto-pass 2 of 3
    And the evaluation says the lowest threshold with 0 false passes is 0.83
    And the run ends successfully

  @pending @US-02 @driving_port @real-io @error @kpi-K1 @contract-shape:pure-function
  Scenario: A defect above the configured threshold moves the safe threshold
    Given a labelled folder with the images damaged_case/002.png and good/000.png
    And Jev judges damaged_case/002.png GOOD with P(GOOD) 0.87
    And Jev judges good/000.png GOOD with P(GOOD) 0.91
    When Jonas runs the evaluation with pass threshold 0.86
    Then the sweep row 0.86 shows 1 false pass and good auto-pass 1 of 1
    And the evaluation says the lowest threshold with 0 false passes is 0.88
    And the run ends with the false-pass signal

  @pending @US-02 @driving_port @real-io @error @kpi-K1 @contract-shape:pure-function
  Scenario: Presenter is told when no threshold is safe
    Given a labelled folder with the images misplaced/006.png and good/000.png
    And Jev judges misplaced/006.png GOOD with P(GOOD) 1.00
    And Jev judges good/000.png GOOD with P(GOOD) 0.91
    When Jonas runs the evaluation with pass threshold 0.86
    Then the evaluation says no threshold is safe
    And the run ends with the false-pass signal

  @pending @US-02 @driving_port @real-io @contract-shape:pure-function
  Scenario: Every threshold is safe when no defect is judged GOOD
    Given a labelled folder with the images bent_lead/000.png and good/000.png
    And Jev judges bent_lead/000.png BENT_LEAD
    And Jev judges good/000.png GOOD with P(GOOD) 0.91
    When Jonas runs the evaluation with pass threshold 0.86
    Then the evaluation says the lowest threshold with 0 false passes is 0.50

  @pending @US-02 @driving_port @real-io @kpi-K1 @contract-shape:bounded-change
  Scenario: The sweep costs no extra Jev calls
    Given a labelled folder with the images good/000.png, good/001.png, bent_lead/000.png and misplaced/000.png
    And Jev judges good/000.png GOOD with P(GOOD) 0.91
    And Jev judges good/001.png GOOD with P(GOOD) 0.85
    And Jev judges bent_lead/000.png BENT_LEAD
    And Jev judges misplaced/000.png MISPLACED
    When Jonas runs the evaluation with pass threshold 0.86
    Then Jev was called exactly 4 times
    And the sweep record holds every threshold from 0.50 to 0.99

  @pending @US-02 @driving_port @real-io @contract-shape:pure-function
  Scenario Outline: The row of the configured threshold equals the headline
    Given a labelled folder with the images damaged_case/002.png, good/000.png and good/001.png
    And Jev judges damaged_case/002.png GOOD with P(GOOD) 0.87
    And Jev judges good/000.png GOOD with P(GOOD) 0.91
    And Jev judges good/001.png GOOD with P(GOOD) 0.85
    When Jonas runs the evaluation with pass threshold <threshold>
    Then the sweep row <threshold> shows <false_passes> false passes and good auto-pass <good_passed> of 2
    And the evaluation shows <false_passes> false passes
    And the evaluation shows good auto-pass <good_passed> of 2

    Examples:
      | threshold | false_passes | good_passed |
      | 0.86      | 1            | 1           |
      | 0.875     | 0            | 1           |
      | 0.60      | 1            | 2           |
      | 1.00      | 0            | 0           |

  @pending @US-02 @driving_port @real-io @contract-shape:bounded-change
  Scenario: The sweep record is complete and the readable table stays short
    Given a labelled folder with the images good/000.png
    And Jev judges good/000.png GOOD with P(GOOD) 0.91
    When Jonas runs the evaluation with pass threshold 0.60
    Then the sweep record holds every threshold from 0.50 to 0.99
    And the readable sweep table shows 0.80 to 0.95 and the configured threshold 0.60

  @pending @US-02 @driving_port @real-io @contract-shape:bounded-change
  Scenario: The chosen threshold applies to the next batch
    Given a labelled folder with the images good/000.png and good/001.png
    And Jev judges good/000.png GOOD with P(GOOD) 0.91
    And Jev judges good/001.png GOOD with P(GOOD) 0.87
    And Jonas has an earlier evaluation of that folder
    When Jonas runs the batch inspection with pass threshold 0.88
    Then the inspection report counts 1 PASS, 0 SCRAP, 1 REVIEW and 0 Jev errors
    And good/001.png is documented as REVIEW with the description "Leans GOOD but below the pass threshold 0.88; P(GOOD)=0.87"

  @pending @US-02 @driving_port @real-io @error @contract-shape:unbounded-preservation
  Scenario: The tool never moves the threshold on its own
    Given a labelled folder with the images damaged_case/002.png and good/000.png
    And Jev judges damaged_case/002.png GOOD with P(GOOD) 0.87
    And Jev judges good/000.png GOOD with P(GOOD) 0.91
    And Jonas has an earlier evaluation of that folder
    When Jonas runs the batch inspection
    Then the evaluation says the lowest threshold with 0 false passes is 0.88
    And the inspection report states the pass threshold 0.86
