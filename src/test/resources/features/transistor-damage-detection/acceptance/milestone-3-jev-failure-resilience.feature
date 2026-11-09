# Slice 03, US-03, INV-1 to INV-4 and INV-6. Petra's batch finishes whatever Jev does.
# Oracle: UC-1 and its amendment (only transient failures are retried), UC-2 (the signals), UC-4 (time limit),
# DDD-26 (readable answer). Refusals moved to milestone 5 with US-05 (D24).
# Waits are recorded by a fake sleeper in the in-process scenarios; @driving_adapter scenarios run the real jar.
#
# States of one image at Jev: asked -> judged (end) | failed for good (end) | waiting -> asked again.
#   A transient failure goes to waiting while attempts remain, otherwise to failed for good.
#   A permanent failure, or a request to wait beyond the cap, goes straight to failed for good.
# States of a run: images inspected -> reports written. An interrupt ends the first state without any report.
Feature: The batch finishes even when Jev fails for some images
  As Petra, the QS inspector
  I want a complete report even when Jev fails for some images
  so that I only look at the images Jev could not judge, instead of starting again.

  Rule: A failed image goes to REVIEW and the batch goes on

    @US-03 @driving_port @real-io @error @kpi-K3 @contract-shape:bounded-change
    Scenario: Batch completes and counts a failed image
      Given an input folder with the images good/000.png, good/042.png and bent_lead/000.png
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      And Jev judges bent_lead/000.png BENT_LEAD
      And Jev fails with "503 Service Unavailable" on every attempt for good/042.png
      When Petra runs the batch inspection
      Then the inspection report counts 1 PASS, 1 SCRAP, 1 REVIEW and 1 Jev error
      And good/042.png is documented as REVIEW with the description "Jev call failed after 3 attempts: HTTP 503 Service Unavailable"
      And Jev was asked about good/042.png exactly 3 times
      And the run ends successfully
      And the output folder holds only what a completed batch declares

    @US-03 @driving_port @real-io @error @contract-shape:bounded-change
    Scenario: A brief Jev outage does not change the verdict
      Given an input folder with the images bent_lead/003.png
      And Jev fails once with "503 Service Unavailable" and then judges bent_lead/003.png BENT_LEAD
      When Petra runs the batch inspection
      Then the inspection report counts 0 PASS, 1 SCRAP, 0 REVIEW and 0 Jev errors
      And Jev was asked about bent_lead/003.png exactly 2 times

    @US-03 @driving_port @real-io @error @kpi-K3 @contract-shape:bounded-change
    Scenario Outline: A failure that may pass goes away is retried, then the image goes to REVIEW
      Given an input folder with the images good/000.png
      And Jev fails with "<failure>" on every attempt for good/000.png
      When Petra runs the batch inspection
      Then good/000.png is documented as REVIEW with the description "Jev call failed after 3 attempts: HTTP <failure>"
      And Jev was asked about good/000.png exactly 3 times

      Examples:
        | failure                   |
        | 408 Request Timeout       |
        | 429 Too Many Requests     |
        | 500 Internal Server Error |
        | 502 Bad Gateway           |
        | 503 Service Unavailable   |
        | 504 Gateway Timeout       |

    @US-03 @driving_port @real-io @error @contract-shape:bounded-change
    Scenario Outline: A failure that cannot go away is not retried
      Given an input folder with the images good/000.png
      And Jev fails with "<failure>" on every attempt for good/000.png
      When Petra runs the batch inspection
      Then good/000.png is documented as REVIEW with the description "Jev call failed after 1 attempt: HTTP <failure>"
      And Jev was asked about good/000.png exactly 1 time
      And no wait happened between attempts

      Examples:
        | failure          |
        | 400 Bad Request  |
        | 401 Unauthorized |
        | 403 Forbidden    |
        | 404 Not Found    |

    @US-03 @driving_port @real-io @error @adapter-integration @contract-shape:bounded-change
    Scenario: An image Jev does not answer in time goes to REVIEW and the batch goes on
      Given an input folder with the images good/042.png and good/043.png
      And Jev gets 1 s to answer
      And Jev does not answer in time for good/042.png
      And Jev judges good/043.png GOOD with P(GOOD) 0.91
      When Petra runs the batch inspection
      Then good/042.png is documented as REVIEW with the description "Jev call failed after 3 attempts: timeout after 1 s"
      And the inspection report counts 1 PASS, 0 SCRAP, 1 REVIEW and 1 Jev error

    @US-03 @driving_port @real-io @error @adapter-integration @contract-shape:bounded-change
    Scenario: An image whose connection breaks goes to REVIEW
      Given an input folder with the images good/000.png
      And the connection to Jev breaks on every attempt for good/000.png
      When Petra runs the batch inspection
      Then good/000.png is documented as REVIEW with a description starting "Jev call failed after 3 attempts: "
      And Jev was asked about good/000.png exactly 3 times

    @US-03 @driving_port @real-io @error @contract-shape:bounded-change
    Scenario Outline: An answer that cannot be trusted never becomes a verdict
      Given an input folder with the images good/000.png
      And Jev answers good/000.png with <answer> on every attempt
      When Petra runs the batch inspection
      Then good/000.png is documented as REVIEW with the description "Jev call failed after 3 attempts: unreadable answer"
      And the inspection report counts 0 PASS, 0 SCRAP, 1 REVIEW and 1 Jev error
      And Jev was asked about good/000.png exactly 3 times

      Examples:
        | answer                         |
        | no probabilities               |
        | an empty P(GOOD)               |
        | no P(GOOD)                     |
        | a P(GOOD) of 1.7               |
        | a P(GOOD) of -0.2              |
        | a P(GOOD) that is not a number |
        | the unknown class SCRATCHED    |
        | no defect answer               |
        | a garbled reply                |
        | an empty reply                 |

    @US-03 @driving_port @real-io @error @contract-shape:bounded-change
    Scenario: A garbled answer followed by a good one gives the normal verdict
      Given an input folder with the images bent_lead/000.png
      And Jev answers bent_lead/000.png with a garbled reply once and then judges it BENT_LEAD
      When Petra runs the batch inspection
      Then the inspection report counts 0 PASS, 1 SCRAP, 0 REVIEW and 0 Jev errors

  Rule: Two failures in a batch of ten leave a complete and consistent report

    Background:
      Given an input folder with the images good/000.png, good/001.png, good/002.png, good/003.png, good/004.png, good/005.png, bent_lead/000.png, cut_lead/000.png, misplaced/000.png and damaged_case/000.png
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      And Jev judges good/001.png GOOD with P(GOOD) 0.92
      And Jev judges good/002.png GOOD with P(GOOD) 0.90
      And Jev judges good/003.png GOOD with P(GOOD) 0.85
      And Jev fails with "503 Service Unavailable" on every attempt for good/004.png
      And Jev fails with "500 Internal Server Error" on every attempt for good/005.png
      And Jev judges bent_lead/000.png BENT_LEAD
      And Jev judges cut_lead/000.png CUT_LEAD
      And Jev judges misplaced/000.png MISPLACED
      And Jev judges damaged_case/000.png DAMAGED_CASE

    @US-03 @driving_port @real-io @error @kpi-K3 @contract-shape:bounded-change
    Scenario: The batch of ten reports every image
      When Petra runs the batch inspection
      Then the inspection report counts 3 PASS, 4 SCRAP, 3 REVIEW and 2 Jev errors
      And the run ends successfully

    @US-03 @driving_port @real-io @error @kpi-K3 @contract-shape:bounded-change
    Scenario: The report says how many images Jev judged
      When Petra runs the batch inspection
      Then 8 of the images were judged by Jev

    @US-03 @driving_port @real-io @error @kpi-K3 @contract-shape:bounded-change
    Scenario: A failed image carries no Jev choice
      When Petra runs the batch inspection
      Then good/004.png is marked as a Jev error without a Jev choice
      And good/005.png is marked as a Jev error without a Jev choice

  Rule: Petra is warned when nothing could be judged

    @US-03 @driving_adapter @real-io @error @kpi-K3 @contract-shape:bounded-change
    Scenario: Petra is warned when no image could be judged
      Given an input folder with the images good/000.png, good/001.png, bent_lead/000.png and misplaced/000.png
      And Jev rejects every call with "401 Unauthorized"
      When Petra runs the batch inspection
      Then the inspection report counts 0 PASS, 0 SCRAP, 4 REVIEW and 4 Jev errors
      And Petra is told "0 of 4 images judged by Jev - check OPENROUTER_API_KEY and network"
      And Jev was called exactly 4 times
      And the run ends with the nothing-judged signal
      And the output folder holds only what a completed batch declares

    @US-03 @driving_port @real-io @error @kpi-K3 @contract-shape:bounded-change
    Scenario: Petra is warned when Jev cannot be reached at all
      Given an input folder with the images good/000.png and good/001.png
      And Jev cannot be reached at all
      When Petra runs the batch inspection
      Then the inspection report counts 0 PASS, 0 SCRAP, 2 REVIEW and 2 Jev errors
      And Petra is told "0 of 2 images judged by Jev - check OPENROUTER_API_KEY and network"
      And the run ends with the nothing-judged signal

    @US-03 @driving_port @real-io @error @contract-shape:bounded-change
    Scenario: The evaluation warns when no image could be judged
      Given a labelled folder with the images good/000.png and bent_lead/000.png
      And Jev rejects every call with "401 Unauthorized"
      When Jonas runs the evaluation with pass threshold 0.86
      Then Jonas is told "0 of 2 images judged by Jev - check OPENROUTER_API_KEY and network"
      And the run ends with the nothing-judged signal
      And the output folder holds only what a completed evaluation declares

    @US-03 @driving_port @real-io @error @contract-shape:bounded-change
    Scenario: The evaluation counts a failed image as not passed at every threshold
      Given a labelled folder with the images good/000.png, good/001.png and bent_lead/000.png
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      And Jev fails with "503 Service Unavailable" on every attempt for good/001.png
      And Jev fails with "503 Service Unavailable" on every attempt for bent_lead/000.png
      When Jonas runs the evaluation with pass threshold 0.86
      Then the evaluation shows 0 false passes
      And the evaluation shows recall 1 of 1 for bent_lead
      And the sweep row 0.50 shows 0 false passes and good auto-pass 1 of 2
      And good/001.png is marked as a Jev error in the evaluation
      And the evaluation counts 2 Jev errors and 1 image judged
      And the run ends successfully

  Rule: Waits grow, stop at the longest allowed wait and respect Jev's own request

    @US-03 @driving_port @real-io @contract-shape:bounded-change
    Scenario: The waits between attempts grow
      Given an input folder with the images good/000.png
      And Jev fails with "503 Service Unavailable" on every attempt for good/000.png
      When Petra runs the batch inspection
      Then the waits between attempts were 1 s, 2 s

    @US-03 @driving_port @real-io @contract-shape:bounded-change
    Scenario: The waits stop growing at the longest allowed wait
      Given an input folder with the images good/000.png
      And Jev fails with "503 Service Unavailable" on every attempt for good/000.png
      And a batch may try 6 times per image
      When Petra runs the batch inspection
      Then the waits between attempts were 1 s, 2 s, 4 s, 8 s, 10 s

    @US-03 @driving_port @real-io @error @contract-shape:bounded-change
    Scenario Outline: The number of attempts is configurable
      Given an input folder with the images good/000.png
      And Jev fails with "503 Service Unavailable" on every attempt for good/000.png
      And a batch may try <attempts> times per image
      When Petra runs the batch inspection
      Then good/000.png is documented as REVIEW with the description "Jev call failed after <wording>: HTTP 503 Service Unavailable"
      And Jev was asked about good/000.png exactly <attempts> times

      Examples:
        | attempts | wording    |
        | 1        | 1 attempt  |
        | 2        | 2 attempts |
        | 5        | 5 attempts |

    @US-03 @driving_port @real-io @contract-shape:bounded-change
    Scenario Outline: Jev's own request to wait is honoured up to the longest allowed wait
      Given an input folder with the images bent_lead/000.png
      And Jev asks to wait <seconds> s with "429 Too Many Requests" and then judges bent_lead/000.png BENT_LEAD
      When Petra runs the batch inspection
      Then the inspection report counts 0 PASS, 1 SCRAP, 0 REVIEW and 0 Jev errors
      And the waits between attempts were <seconds> s

      Examples:
        | seconds |
        | 4       |
        | 10      |

    @US-03 @driving_port @real-io @error @contract-shape:bounded-change
    Scenario: A request to wait longer than allowed ends the attempts at once
      Given an input folder with the images good/042.png and good/043.png
      And Jev asks to wait 120 s with "429 Too Many Requests" on every attempt for good/042.png
      And Jev judges good/043.png GOOD with P(GOOD) 0.91
      When Petra runs the batch inspection
      Then good/042.png is documented as REVIEW with the description "Jev call failed after 1 attempt: HTTP 429 Too Many Requests (Retry-After 120 s exceeds max-backoff)"
      And Jev was asked about good/042.png exactly 1 time
      And no wait happened between attempts
      And the inspection report counts 1 PASS, 0 SCRAP, 1 REVIEW and 1 Jev error

  Rule: The cost is exact only when no attempt failed

    @US-03 @driving_port @real-io @error @contract-shape:bounded-change
    Scenario: After a failed image the cost is shown as a lower bound
      Given an input folder with the images good/000.png and bent_lead/000.png
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      And Jev fails with "503 Service Unavailable" on every attempt for bent_lead/000.png
      When Petra runs the batch inspection
      Then the reported Jev cost is $0.00010
      And the cost is shown as a lower bound

    @US-03 @driving_port @real-io @error @contract-shape:bounded-change
    Scenario: A success after a failed attempt also shows the cost as a lower bound
      Given an input folder with the images bent_lead/003.png
      And Jev fails once with "503 Service Unavailable" and then judges bent_lead/003.png BENT_LEAD
      When Petra runs the batch inspection
      Then the reported Jev cost is $0.00020
      And the cost is shown as a lower bound

    @US-03 @driving_port @real-io @contract-shape:bounded-change
    Scenario: Without failed attempts the cost is exact
      Given an input folder with the images good/000.png
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      When Petra runs the batch inspection
      Then the inspection report counts 1 PASS, 0 SCRAP, 0 REVIEW and 0 Jev errors
      And the reported Jev cost is $0.00010
      And the cost is not shown as a lower bound

    @US-03 @driving_port @real-io @error @contract-shape:bounded-change
    Scenario: After failed attempts the evaluation shows its cost as a lower bound
      Given a labelled folder with the images good/000.png and bent_lead/000.png
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      And Jev fails with "503 Service Unavailable" on every attempt for bent_lead/000.png
      When Jonas runs the evaluation with pass threshold 0.86
      Then the evaluation shows its cost as a lower bound

  Rule: An interrupted run leaves no half-written report

    @US-03 @driving_adapter @real-io @error @contract-shape:bounded-change
    Scenario Outline: An interrupted run leaves no finished report
      Given an input folder with the images good/000.png and good/001.png
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      And Jev is still thinking about good/001.png
      When Petra interrupts the batch inspection with <interruption> during good/001.png
      Then the run ends interrupted by <interruption>
      And the output folder holds no finished report
      And the output folder holds only what an interrupted run declares

      Examples:
        | interruption   |
        | Ctrl-C         |
        | a stop request |

  Rule: What Petra sees and leaves behind is the same with details on the console

    @US-03 @driving_adapter @real-io @adapter-integration @contract-shape:bounded-change
    Scenario: Batch result can be read from the command line
      Given an input folder with the images good/000.png and bent_lead/000.png
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      And Jev judges bent_lead/000.png BENT_LEAD
      When Petra runs the batch inspection
      Then the only summary line reads "Inspected 2 images: 1 PASS, 1 SCRAP, 0 REVIEW (REVIEW share 0.0 %; 0 Jev errors in REVIEW), Jev cost $0.00030 -> <output>/report.md"
      And nothing is reported as a problem
      And the run ends successfully
      And the output folder holds only what a completed batch declares

    @US-03 @driving_adapter @real-io @error @contract-shape:unbounded-preservation
    Scenario Outline: The API key never shows up in what the detector leaves behind
      Given an input folder with the images good/000.png and bent_lead/000.png
      And the API key is a marked key
      And Jev <jev_behaviour>
      When Petra runs the batch inspection <console>
      Then the inspection report counts 0 PASS, 0 SCRAP, 2 REVIEW and 2 Jev errors
      And the output folder holds only what a completed batch declares
      And the marked key appears nowhere in the console output, the log or the reports

      Examples:
        | jev_behaviour                                     | console                     |
        | rejects every call with "401 Unauthorized"        | with details on the console |
        | rejects every call with "503 Service Unavailable" | as usual                    |
        | rejects every call with "503 Service Unavailable" | with details on the console |

    @US-03 @driving_adapter @real-io @contract-shape:bounded-change
    Scenario: Details on the console change nothing about the result
      Given an input folder with the images good/000.png and bent_lead/000.png
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      And Jev judges bent_lead/000.png BENT_LEAD
      When Petra runs the batch inspection with details on the console
      Then the inspection report counts 1 PASS, 1 SCRAP, 0 REVIEW and 0 Jev errors
      And the run ends successfully

    @US-03 @driving_port @real-io @contract-shape:bounded-change
    Scenario: PNG and JPEG images are inspected and other files are ignored
      Given an input folder with the images good/000.png, good/001.jpg, good/002.JPG and good/003.jpeg
      And the input folder also holds the file notes.txt
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      And Jev judges good/001.jpg GOOD with P(GOOD) 0.91
      And Jev judges good/002.JPG GOOD with P(GOOD) 0.91
      And Jev judges good/003.jpeg GOOD with P(GOOD) 0.91
      When Petra runs the batch inspection
      Then the inspection report counts 4 PASS, 0 SCRAP, 0 REVIEW and 0 Jev errors

    @US-03 @driving_port @real-io @contract-shape:bounded-change
    Scenario: A folder that already holds files keeps them
      Given an input folder with the images good/000.png
      And Jev judges good/000.png GOOD with P(GOOD) 0.91
      And the output folder already holds the file notes.txt
      When Petra runs the batch inspection
      Then the file notes.txt in the output folder is unchanged
      And the output folder holds only what a completed batch declares
