# Slice 01, US-01. Jonas proves on labelled images that no defect passes.
# The run-ending signals are the exit codes of ADR-005: successfully 0, refusal 1, false-pass 2, nothing-judged 3.
# Every scenario is @pending: DELIVER removes the tag from one scenario at a time.
Feature: See whether any defect passes on the labelled images
  As Jonas, the showcase presenter
  I want the detector scored against the labelled images
  so that I only claim "no defect passes" when the tool has measured it.

  Rule: A labelled set is scored against its folder labels

    Background:
      Given a labelled folder with the images good/000.png, good/001.png, bent_lead/000.png and misplaced/000.png
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      And Jev judges good/001.png GOOD with P(GOOD) 0.85
      And Jev judges bent_lead/000.png CUT_LEAD
      And Jev judges misplaced/000.png GOOD with P(GOOD) 0.45

    @US-01 @driving_port @real-io @kpi-K1 @contract-shape:bounded-change
    Scenario: Presenter sees zero false passes on the labelled set
      When Jonas runs the evaluation with pass threshold 0.86
      Then the evaluation shows 0 false passes
      And the run ends successfully

    @US-01 @driving_port @real-io @kpi-K2 @contract-shape:pure-function
    Scenario: Presenter sees how many good parts pass automatically
      When Jonas runs the evaluation with pass threshold 0.86
      Then the evaluation shows good auto-pass 1 of 2

    @US-01 @driving_port @real-io @contract-shape:pure-function
    Scenario: Presenter sees the recall of each defect class
      When Jonas runs the evaluation with pass threshold 0.86
      Then the evaluation shows recall 1 of 1 for bent_lead
      And the evaluation shows recall 1 of 1 for misplaced

    @US-01 @driving_port @real-io @contract-shape:pure-function
    Scenario: A wrong suspected class does not count as correct
      When Jonas runs the evaluation with pass threshold 0.86
      Then the suspected class of bent_lead/000.png is counted as not correct

    @US-01 @driving_port @real-io @contract-shape:unbounded-preservation
    Scenario: An evaluation writes nothing but its declared files
      When Jonas runs the evaluation with pass threshold 0.86
      Then the output folder holds only what a completed evaluation declares

  Rule: A false pass is named and stops a demo script

    @pending @US-01 @driving_adapter @real-io @error @kpi-K1 @contract-shape:bounded-change
    Scenario: A defect that passes is named as a false pass
      Given a labelled folder with the images good/000.png and damaged_case/002.png
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      And Jev judges damaged_case/002.png GOOD with P(GOOD) 0.87
      When Jonas runs the evaluation with pass threshold 0.86
      Then the evaluation shows 1 false pass
      And the evaluation lists damaged_case/002.png as a false pass with P(GOOD) 0.87
      And the run ends with the false-pass signal

    @pending @US-01 @driving_port @real-io @error @kpi-K1 @contract-shape:bounded-change
    Scenario: A false pass is also reported as a problem
      Given a labelled folder with the images damaged_case/002.png
      And Jev judges damaged_case/002.png GOOD with P(GOOD) 0.87
      When Jonas runs the evaluation with pass threshold 0.86
      Then Jonas is told "1 false passes at pass threshold 0.86: see <output>/evaluation.md"

    @pending @US-01 @driving_port @real-io @error @contract-shape:pure-function
    Scenario Outline: The false-pass line sits exactly at the pass threshold
      Given a labelled folder with the images good/000.png and damaged_case/002.png
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      And Jev judges damaged_case/002.png GOOD with P(GOOD) <p_good>
      When Jonas runs the evaluation with pass threshold 0.86
      Then the evaluation shows <false_passes> false passes
      And damaged_case/002.png has the verdict <verdict> in the evaluation
      And the run ends <ending>

      Examples:
        | p_good | false_passes | verdict | ending                     |
        | 0.85   | 0            | REVIEW  | successfully               |
        | 0.86   | 1            | PASS    | with the false-pass signal |
        | 0.87   | 1            | PASS    | with the false-pass signal |

    @pending @US-01 @driving_port @real-io @error @contract-shape:pure-function
    Scenario: The class table separates recall from the suspected class
      Given a labelled folder with the images misplaced/000.png, misplaced/001.png and misplaced/002.png
      And Jev judges misplaced/000.png MISPLACED
      And Jev judges misplaced/001.png BENT_LEAD
      And Jev judges misplaced/002.png GOOD with P(GOOD) 0.90
      When Jonas runs the evaluation with pass threshold 0.86
      Then the class table shows misplaced with 3 images, 2 not passed, 1 suspected class correct and 1 passed
      And the run ends with the false-pass signal

  Rule: Labels score the run and never reach Jev

    @pending @US-01 @driving_port @real-io @kpi-G1 @contract-shape:unbounded-preservation
    Scenario: Folder names are only used for scoring
      Given a labelled folder with the images damaged_case/002.png, good/017.png and misplaced/006.png
      And Jev judges damaged_case/002.png DAMAGED_CASE
      And Jev judges good/017.png GOOD with P(GOOD) 0.88
      And Jev judges misplaced/006.png MISPLACED
      When Jonas runs the evaluation with pass threshold 0.86
      Then no request to Jev carries an image file name, a relative path, a label with a path separator or the input folder name
      And apart from the image data every request to Jev is identical
      And the evaluation lists 3 images

    @pending @US-01 @driving_port @real-io @error @contract-shape:bounded-change
    Scenario Outline: An image without a label is skipped, counted and not sent to Jev
      Given a labelled folder with the images good/000.png and <unlabelled>
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      When Jonas runs the evaluation with pass threshold 0.86
      Then the evaluation counts 1 image as skipped without a label
      And Jev was called exactly 1 time
      And the run ends successfully

      Examples:
        | unlabelled         |
        | unsorted/017.png   |
        | loose-017.png      |
        | Good/000.png       |
        | good_old/000.png   |

    @US-01 @driving_port @real-io @contract-shape:pure-function
    Scenario: An image in a deeper sub-folder keeps its label
      Given a labelled folder with the images good/shift-1/000.png and cut_lead/shift-1/003.png
      And Jev judges good/shift-1/000.png GOOD with P(GOOD) 0.91
      And Jev judges cut_lead/shift-1/003.png CUT_LEAD
      When Jonas runs the evaluation with pass threshold 0.86
      Then the evaluation shows good auto-pass 1 of 1
      And the evaluation shows recall 1 of 1 for cut_lead

    @pending @US-01 @driving_port @real-io @error @contract-shape:bounded-change
    Scenario Outline: A folder without labels is refused before any cost
      Given a folder that holds <content>
      When Jonas runs the evaluation with pass threshold 0.86
      Then Jonas is told "No labelled images below <input>: expected sub-folders good, bent_lead, cut_lead, damaged_case, misplaced"
      And no Jev call was made
      And the run ends with the refusal signal
      And the output folder holds only what a refusal after the start declares

      Examples:
        | content                        |
        | nothing at all                 |
        | only images in unknown folders |

    @US-01 @driving_port @real-io @contract-shape:bounded-change
    Scenario: A class without images is still shown, with zero rows
      Given a labelled folder with the images good/000.png and good/001.png
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      And Jev judges good/001.png GOOD with P(GOOD) 0.90
      When Jonas runs the evaluation with pass threshold 0.86
      Then the class table shows bent_lead with 0 images, 0 not passed, 0 suspected class correct and 0 passed
      And the class table shows cut_lead with 0 images, 0 not passed, 0 suspected class correct and 0 passed
      And the class table shows damaged_case with 0 images, 0 not passed, 0 suspected class correct and 0 passed
      And the class table shows misplaced with 0 images, 0 not passed, 0 suspected class correct and 0 passed

  Rule: The evaluation is honest evidence

    @pending @US-01 @driving_adapter @real-io @adapter-integration @contract-shape:bounded-change
    Scenario: Evaluation result can be read from the command line
      Given a labelled folder with the images good/000.png and bent_lead/000.png
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      And Jev judges bent_lead/000.png BENT_LEAD
      When Jonas runs the evaluation with pass threshold 0.86
      Then the only summary line reads "Evaluated 2 labelled images at pass threshold 0.86: 0 false passes, good auto-pass 1/1 (100.0 %), defects not passed 1/1, Jev cost $0.00030 -> <output>/evaluation.md"
      And nothing is reported as a problem
      And the run ends successfully

    @pending @US-01 @driving_port @real-io @kpi-K5 @contract-shape:bounded-change
    Scenario: The evaluation keeps its cost and duration as evidence
      Given a labelled folder with the images good/000.png and bent_lead/000.png
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      And Jev judges bent_lead/000.png BENT_LEAD
      When Jonas runs the evaluation with pass threshold 0.86
      Then the evaluation states a Jev cost of $0.00030
      And the evaluation states how many seconds the run took

    @pending @US-01 @driving_port @real-io @contract-shape:bounded-change
    Scenario: The evaluation names its run date
      Given a labelled folder with the images good/000.png
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      And today is 2026-11-06
      When Jonas runs the evaluation with pass threshold 0.86
      Then the evaluation states the run date 2026-11-06

    @pending @US-01 @driving_port @real-io @contract-shape:bounded-change
    Scenario: The evaluation says what its defect images can and cannot prove
      Given a labelled folder with the images good/000.png
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      When Jonas runs the evaluation with pass threshold 0.86
      Then the evaluation says "The defect images were in the SPIKE sample, so the defect side shows reproducibility and is not held-out validation. Jev results can differ from run to run."

    @pending @US-01 @driving_port @real-io @contract-shape:bounded-change
    Scenario: The evaluation names the source of the sample data
      Given a labelled folder with the images good/000.png
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      When Jonas runs the evaluation with pass threshold 0.86
      Then the evaluation ends with the attribution "Sample data: MVTec AD \"transistor\", CC BY-NC-SA 4.0, non-commercial use"

    @pending @US-01 @driving_port @real-io @contract-shape:bounded-change
    Scenario: Running the evaluation again replaces the earlier record
      Given a labelled folder with the images good/000.png
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      And the output folder already holds the file notes.txt
      And today is 2026-11-05
      And Jonas has an earlier evaluation of that folder
      And today is 2026-11-06
      And the labelled folder gains the image good/001.png
      And Jev judges good/001.png GOOD with P(GOOD) 0.90
      When Jonas runs the evaluation with pass threshold 0.86
      Then the evaluation lists 2 images
      And the evaluation states the run date 2026-11-06
      And the earlier run date 2026-11-05 appears nowhere in the evaluation
      And the file notes.txt in the output folder is unchanged
