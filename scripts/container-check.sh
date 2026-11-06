#!/usr/bin/env bash
# SCAFFOLD: true
#
# US-04 / K4 gate, outside ./mvnw verify. DELIVER slice 04 replaces each check body; DISTILL only fixes the check and
# finding names, which milestone-4-docker-run.feature reads. Steps and rules: feature-delta.md, "CI/CD pipeline outline".
#
# Usage: scripts/container-check.sh [check ...]     (no argument runs every check)
# real-env-untouched is the last check on every exit path: the real .env has the same hash as before, or still does not exist.
# Prints one "PASS <finding>" or "FAIL <finding>: <why>" line per finding. Exit 0 only if every finding passed.
#
# Safety contract for slice 04 (steps 1-3 and 7 of the outline), required before any docker build is added here:
#   - Never write to the working tree. The real .env is never written, moved or copied.
#   - Record sha256sum of the real .env first (never print it); compare on every exit path.
#   - CTX=$(mktemp -d) with a trap on EXIT INT TERM. Copy the tree into it without .env*, plant the canary in the copy
#     only, and build from the copy with a check-<run-id> tag, so a check never moves damage-detector:0.1.0.
#   - Real-key scan (demo host only): build from the untouched original, pass the key to grep on stdin, print pass or fail.
set -euo pipefail

declare -A FINDINGS=(
  [image-scan]="image-holds-no-key real-env-untouched"
  [parity]="report-files verdict-counts-equal"
  [evaluation]="evaluation-files"
  [runtime-key]="no-key-in-run-output"
  [missing-key]="missing-key-refused"
  [exit-codes]="nothing-judged-signal false-pass-signal"
  [ownership]="output-deletable"
  [forgotten-mount]="forgotten-mount-refused"
)
ORDER=(image-scan parity evaluation runtime-key missing-key exit-codes ownership forgotten-mount)

not_yet_implemented() {
  local finding
  for finding in ${FINDINGS[$1]}; do
    echo "FAIL ${finding}: Not yet implemented -- RED scaffold"
  done
  return 1
}

failed=0
for check in "${@:-${ORDER[@]}}"; do
  if [[ -z "${FINDINGS[$check]:-}" ]]; then
    echo "Unknown check: ${check} (known: ${ORDER[*]})" >&2
    exit 64
  fi
  not_yet_implemented "$check" || failed=1
done
exit "$failed"
