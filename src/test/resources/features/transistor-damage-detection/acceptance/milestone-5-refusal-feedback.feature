# Slice 05, US-05, the INV-5 rows it owns (D24). Slice order is 01, 02, 03, 05, 04, so this runs before milestone 4.
# Texts are pinned in ADR-005 decision 1 and DDD-33. Paths appear as typed (UC-5, not yet user-confirmed); the one place
# that decides how a path is shown is Workspace.resolve.
#
# States of a start: arguments checked -> paths checked -> output created -> output probed -> log set -> started.
# Each refusal below is the illegal-event exit of one state. Before "log set" a refusal leaves nothing behind.
Feature: A run that cannot work is refused at once, with a clear reason
  As Petra, the QS inspector
  I want one clear line when a run cannot work
  so that I fix the cause and start again, knowing nothing was charged or judged.

  @US-05 @driving_adapter @real-io @error @kpi-K6 @contract-shape:unbounded-preservation
  Scenario Outline: A mistake on the command line is refused before anything is written
    Given an input folder with the images good/000.png
    When Petra starts the detector <mistake>
    Then Petra is told "<message>"
    And no Jev call was made
    And the run ends with the refusal signal
    And the output folder holds only what a refusal before writing declares

    Examples:
      | mistake                                                                       | message                                                                                                   |
      | without an input folder                                                       | Usage: java -jar damage-detector.jar [--evaluate] --input=<folder> --output=<folder> (missing --input)    |
      | without an output folder                                                      | Usage: java -jar damage-detector.jar [--evaluate] --input=<folder> --output=<folder> (missing --output)   |
      | with the misspelt option --evalute                                            | Usage: java -jar damage-detector.jar [--evaluate] --input=<folder> --output=<folder> (unknown option --evalute) |
      | with a value after --evaluate                                                 | Usage: java -jar damage-detector.jar [--evaluate] --input=<folder> --output=<folder> (--evaluate takes no value) |
      | with an input folder that does not exist                                      | Input folder does not exist: <input>                                                                      |
      | with a file as the input folder                                               | Input folder does not exist: <input>                                                                      |
      | with the output folder equal to the input folder                              | Output folder <output> is the same as or inside input folder <input>: choose an output outside the input  |
      | with the output folder inside the input folder                                | Output folder <output> is the same as or inside input folder <input>: choose an output outside the input  |
      | with an output folder that climbs back into the input folder                  | Output folder <output> is the same as or inside input folder <input>: choose an output outside the input  |
      | with an output folder reached through a shortcut into the input               | Output folder <output> is the same as or inside input folder <input>: choose an output outside the input  |
      | with an output folder that climbs into the input through a missing folder     | Output folder <output> is the same as or inside input folder <input>: choose an output outside the input  |

  @US-05 @driving_adapter @real-io @error @kpi-K6 @contract-shape:bounded-change
  Scenario Outline: An input folder without images is refused
    Given a folder that holds <content>
    When Petra runs the batch inspection
    Then Petra is told "No PNG/JPEG images below <input>"
    And no Jev call was made
    And the run ends with the refusal signal
    And the output folder holds only what a refusal after the start declares

    Examples:
      | content        |
      | nothing at all |
      | only notes     |

  @US-05 @driving_adapter @real-io @error @kpi-K6 @contract-shape:bounded-change
  Scenario Outline: A missing API key is refused before any cost
    Given a labelled folder with the images good/000.png
    And the API key is <key>
    When <person> runs the <use_case>
    Then <person> is told "No Jev API key: set the OPENROUTER_API_KEY environment variable"
    And no Jev call was made
    And the run ends with the refusal signal
    And the output folder holds only what a refusal after the start declares

    Examples:
      | key     | person | use_case         |
      | missing | Petra  | batch inspection |
      | blank   | Petra  | batch inspection |
      | missing | Jonas  | evaluation       |

  @US-05 @driving_adapter @real-io @error @kpi-K6 @contract-shape:unbounded-preservation
  Scenario Outline: An output folder that cannot be written is refused
    Given an input folder with the images good/000.png
    And the output folder <obstacle>
    When Petra runs the batch inspection
    Then Petra is told "Cannot write to output folder <output>: <cause>"
    And no Jev call was made
    And the run ends with the refusal signal
    And the output folder holds only what a refusal before writing declares

    Examples:
      | obstacle          | cause             |
      | is read-only      | permission denied |
      | is a file         | not a folder      |
      | lies below a file | not a folder      |

  @US-05 @driving_adapter @real-io @error @kpi-K6 @contract-shape:bounded-change
  Scenario Outline: An unusable pass threshold is refused
    Given an input folder with the images good/000.png
    When Petra runs the batch inspection with the pass threshold setting <setting>
    Then Petra is told "Pass threshold must be a number from 0 to 1: set detector.pass-threshold (was <setting>)"
    And no Jev call was made
    And the run ends with the refusal signal
    And the output folder holds only what a refusal after the start declares

    Examples:
      | setting |
      | 1.5     |
      | -0.1    |
      | abc     |

  @US-05 @driving_adapter @real-io @error @kpi-K6 @contract-shape:bounded-change
  Scenario Outline: An unusable Jev address is refused before any cost
    Given an input folder with the images good/000.png
    And the Jev address is "<address>"
    When Petra runs the batch inspection
    Then Petra is told "Jev base URL cannot be used: set detector.jev.base-url to an http or https address"
    And the run ends with the refusal signal
    And the output folder holds only what a refusal after the start declares

    Examples:
      | address        |
      | not a url      |
      | ftp://jev.test |

  @US-05 @driving_adapter @real-io @error @kpi-K6 @contract-shape:bounded-change
  Scenario: An unexpected failure gives one line and leaves the details in the log
    Given an input folder with the images good/000.png
    And Jev judges good/000.png GOOD with P(GOOD) 0.91
    And the output folder already holds a folder named report.json
    When Petra runs the batch inspection
    Then Petra is told something starting with "Inspection failed: "
    And Petra is told something ending with ". See log for details."
    And the details of the failure are in the log
    And the run ends with the refusal signal
