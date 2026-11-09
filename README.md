# spring-ai-damage-detector

A Spring Boot command-line tool that checks top-view photos of through-hole transistors for defects. Each image goes to
the Jev decision model through the OpenRouter Decisions API. The tool lets a part pass only when Jev answers GOOD with
P(GOOD) at or above the pass threshold (default 0.87: one step of margin over 0.86, the lowest threshold with 0 false passes on the real 253-image sweep, because Jev varies from run to run). Every other image is flagged for scrap or review.

## Requirements

- Java 21
- An OpenRouter API key with credit, in the environment variable `OPENROUTER_API_KEY`. Do not pass the key as an
  argument.

## Build

```sh
./mvnw clean verify
```

## Inspect a folder (batch mode)

```sh
java -jar target/damage-detector-0.1.0-SNAPSHOT.jar --input=<image folder> --output=<output folder>
```

Writes `report.md` and `report.json`. Images that do not pass are copied to `scrap/` or `review/`, each with a text file
holding its verdict.

## Evaluate the pass rule on labelled images

```sh
java -jar target/damage-detector-0.1.0-SNAPSHOT.jar --evaluate --input=images --output=out/eval
```

The first sub-folder of each image is its label: `good`, `bent_lead`, `cut_lead`, `damaged_case` or `misplaced`.
Images without a label are skipped and counted, and they are not sent to Jev. The label is used only for scoring. It is
never sent to Jev. The run writes `evaluation.md` and `evaluation.json`: false passes, the good auto-pass rate, a
per-class table, a threshold sweep, the run date, the Jev cost and the elapsed seconds. A rerun into the same folder replaces both files.

The sweep shows, for every threshold from 0.50 to 0.99, the false passes and the good parts that pass automatically.
It is recomputed from the same Jev answers, so it costs no extra Jev calls. `evaluation.md` shows 0.80 to 0.95 and the
configured threshold, and names the lowest threshold with 0 false passes. `evaluation.json` holds every threshold. The
tool never changes the threshold: you decide.

To use a different pass threshold, add `--detector.pass-threshold=0.90`.

A run that cannot work is refused before any image is judged, with one line that names the cause and exit 1: a pass
threshold that is not a number from 0 to 1, a folder without PNG/JPEG images, a missing or blank API key, or a Jev
address that is not an http or https address. Any unexpected failure gives one line `Inspection failed: <cause>. See log for
details.` and exit 1; the details are in `damage-detector.log` in the output folder.

## Exit codes

| Code | Meaning |
|---|---|
| 0 | The run completed. In evaluate mode this also means no defect passed. |
| 1 | Refused or unexpected error, for example a missing key, an unusable folder or a folder without labels. One line on stderr. |
| 2 | Evaluate mode: at least one defect passed at the configured pass threshold (a false pass). The reports are written. |
| 3 | No image could be judged. The reports are written and the line `0 of <n> images judged by Jev - check OPENROUTER_API_KEY and network` is printed. Exit 3 wins over exit 2. |
| 130 | Interrupted with Ctrl-C. No finished report: there is no `report.md` or `evaluation.md`. |
| 143 | Interrupted by a stop request (SIGTERM, for example `docker stop`). No finished report. |
| 137 | Killed (SIGKILL, for example out of memory or `docker kill`). The JVM cannot catch it, so there is no line and no finished report. |

Every report is written whole under its final name, `report.md` (or `evaluation.md`) last. A folder without that summary is
incomplete. After an interrupted run, rerun into a fresh output folder.

## Sample data

The images come from the MVTec AD dataset, category "transistor", and are not part of this repository.
MVTec AD is © MVTec Software GmbH and is licensed under
[CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/), for non-commercial use only.
Paul Bergmann, Kilian Batzner, Michael Fauser, David Sattlegger, Carsten Steger: "The MVTec Anomaly Detection Dataset:
A Comprehensive Real-World Dataset for Unsupervised Anomaly Detection", IJCV 2021.
