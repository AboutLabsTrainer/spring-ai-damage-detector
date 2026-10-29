@walking_skeleton @driving_port
Feature: Batch inspection of transistor images
  As a QS inspector of the transistor line
  I want every image in a folder judged PASS, SCRAP or REVIEW
  so that good parts pass and scrap is documented with its image and a description.

  Scenario: A good-looking part passes and a part with a cut lead is documented as scrap
    Given an input folder with one good-looking transistor image and one transistor image with a cut lead
    And Jev judges the good-looking image GOOD with P(GOOD) 0.91
    And Jev judges the cut-lead image CUT_LEAD with probability 0.53 and P(GOOD) 0.12
    When the inspector runs the batch inspection on the input folder
    Then the report shows 1 PASS, 1 SCRAP and 0 REVIEW
    And the cut-lead image is copied to the scrap folder with the description "Suspected CUT_LEAD (p=0.53); P(GOOD)=0.12"
    And no file or folder name was sent to Jev
