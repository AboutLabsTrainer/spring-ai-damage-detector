#!/usr/bin/env bash
# US-04 / K4 gate, outside ./mvnw verify. Steps and rules: feature-delta.md, "CI/CD pipeline outline".
#
# Usage: scripts/container-check.sh [--self-test | check ...]     (no argument runs every check)
# Prints one "PASS <finding>" or "FAIL <finding>: <why>" line per finding. Exit 0 only if every finding passed.
#   --self-test   proves the key scan on deliberately bad images before any scenario relies on it.
#   image-scan    builds the image from an isolated copy with a planted canary key and scans it (plus the real key, if any).
# Not yet built (slice 04 half B): parity, evaluation, runtime-key, missing-key, exit-codes, ownership, forgotten-mount.
#
# Safety contract:
#   - The working tree is never written. The real .env is never written, moved or copied, and never printed.
#   - Its sha256 is recorded first and compared on every exit path (finding real-env-untouched).
#   - CTX=$(mktemp -d) with a trap on EXIT INT TERM. The copy has no .env*; the canary is planted in the copy only.
#     Check images are tagged damage-detector:check-<run-id> or ...:selftest-<run-id>-*, never :0.1.0.
#   - The real-key scan builds from the untouched original and hands the key to grep on stdin, never as an argument.
set -euo pipefail

ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$ROOT"

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

RUN_ID="$(date +%Y%m%d%H%M%S)-$$"
CTX=""
TAGS=()
GUARD_ENV=0
ENV_BEFORE=""

real_env_hash() {
  if [[ -f "$ROOT/.env" ]]; then sha256sum <"$ROOT/.env" | cut -d' ' -f1; else echo absent; fi
}

finish() {
  local code=$?
  trap - EXIT INT TERM
  [[ -n "$CTX" ]] && { chmod -R u+rwX "$CTX" 2>/dev/null || true; rm -rf "$CTX"; }
  [[ ${#TAGS[@]} -gt 0 ]] && { docker rmi -f "${TAGS[@]}" >/dev/null 2>&1 || true; }
  if [[ $GUARD_ENV -eq 1 ]]; then
    if [[ "$(real_env_hash)" == "$ENV_BEFORE" ]]; then
      echo "PASS real-env-untouched"
    else
      echo "FAIL real-env-untouched: the real .env changed during the check"
      code=1
    fi
  fi
  exit "$code"
}
trap finish EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

not_yet_implemented() {
  local finding
  for finding in ${FINDINGS[$1]}; do
    echo "FAIL ${finding}: Not yet implemented -- half B of slice 04"
  done
  return 1
}

docker_ready() {
  docker info >/dev/null 2>&1 || { echo "FAIL $1: Docker is not reachable"; return 1; }
}

make_context() {
  [[ -n "$CTX" ]] || CTX=$(mktemp -d)
}

# Copy of the tree without .env*, images/, out/, target/ (ignored files never reach it).
copy_tree() {
  local dest=$1
  mkdir -p "$dest"
  git ls-files -co --exclude-standard -z \
    | while IFS= read -r -d '' f; do [ -e "$f" ] && printf '%s\0' "$f"; done \
    | tar --null --exclude='.env' --exclude='.env.*' -T - -cf - | tar -xf - -C "$dest"
}

new_canary() {
  echo "sk-or-v1-canary-$(od -An -N16 -tx1 /dev/urandom | tr -d ' \n')"
}

# build <dir> <tag> [docker build args...]; the log stays in CTX, only its tail is shown on failure
build() {
  local dir=$1 tag=$2
  shift 2
  TAGS+=("$tag")
  if ! docker build -t "$tag" "$@" "$dir" >"$CTX/build-${tag##*:}.log" 2>&1; then
    tail -n 5 "$CTX/build-${tag##*:}.log" >&2
    return 1
  fi
}

# scan_image <tag> <workdir>: META_NEEDLES search inspect and history, FILE_NEEDLES search layers and unpacked jars.
# Needles come in on stdin of grep (builtin printf), never as arguments. Sets ROUTES to the findings.
scan_image() {
  local tag=$1 w=$2 blob n=0 layer jar
  ROUTES=()
  mkdir -p "$w/save" "$w/layers"
  docker image inspect "$tag" >"$w/inspect.json"
  docker history --no-trunc "$tag" >"$w/history.txt"
  printf '%s\n' "$META_NEEDLES" | grep -qF -f - "$w/inspect.json" && ROUTES+=(configuration) || true
  printf '%s\n' "$META_NEEDLES" | grep -qF -f - "$w/history.txt" && ROUTES+=(history) || true

  docker save "$tag" | tar -xf - -C "$w/save"
  while IFS= read -r -d '' blob; do
    tar -tf "$blob" >/dev/null 2>&1 || continue
    n=$((n + 1))
    mkdir -p "$w/layers/$n"
    tar -xf "$blob" -C "$w/layers/$n" --no-same-owner --no-same-permissions 2>/dev/null || true
  done < <(find "$w/save" -type f \( -path '*/blobs/*' -o -name layer.tar \) -print0)
  chmod -R u+rwX "$w/layers" 2>/dev/null || true

  if find "$w/layers" \( -name .env -o -name '.env.*' -o -name '.wh..env' -o -name '.wh..env.*' \) -print -quit | grep -q .; then
    ROUTES+=(layer-env-file)
  fi
  if printf '%s\n' "$FILE_NEEDLES" | grep -rqF -f - "$w/layers" "$w/save" 2>/dev/null; then
    ROUTES+=(layer-key)
  fi
  while IFS= read -r -d '' jar; do
    unpack_jar "$jar" "$w/jar-$((++n))"
    if find "$w/jar-$n" -name '.env*' -print -quit | grep -q .; then ROUTES+=(jar-env-file); fi
    if printf '%s\n' "$FILE_NEEDLES" | grep -rqF -f - "$w/jar-$n" 2>/dev/null; then ROUTES+=(jar-key); fi
  done < <(find "$w/layers" -name app.jar -type f -print0)
}

unpack_jar() {
  mkdir -p "$2"
  if command -v unzip >/dev/null; then unzip -q -o "$1" -d "$2" || true
  else python3 -I -m zipfile -e "$1" "$2" || true; fi
}

has_route() {
  local route
  for route in "${ROUTES[@]}"; do [[ "$route" == "$1" ]] && return 0; done
  return 1
}

routes_text() {
  local IFS=,
  echo "${ROUTES[*]}"
}

image_scan() {
  docker_ready image-holds-no-key || return 1
  make_context
  local canary copy ok=1 key
  canary=$(new_canary)
  copy="$CTX/copy"
  copy_tree "$copy"
  printf 'OPENROUTER_API_KEY=%s\n' "$canary" >"$copy/.env"
  mkdir -p "$copy/src/main/resources"
  printf 'OPENROUTER_API_KEY=%s\n' "$canary" >"$copy/src/main/resources/.env"

  META_NEEDLES=$(printf '%s\n' "$canary" 'sk-or-' 'OPENROUTER_API_KEY')
  FILE_NEEDLES=$canary
  build "$copy" "damage-detector:check-$RUN_ID" --build-arg GIT_SHA=check || return 1
  scan_image "damage-detector:check-$RUN_ID" "$CTX/scan-copy"
  [[ ${#ROUTES[@]} -eq 0 ]] || { echo "FAIL image-holds-no-key: key traces in $(routes_text)"; ok=0; }

  # A stray ARG in the Dockerfile would put a --build-arg value into the history.
  build "$copy" "damage-detector:check-$RUN_ID-arg" --build-arg GIT_SHA=check --build-arg "OPENROUTER_API_KEY=$canary" || return 1
  scan_image "damage-detector:check-$RUN_ID-arg" "$CTX/scan-arg"
  [[ ${#ROUTES[@]} -eq 0 ]] || { echo "FAIL image-holds-no-key: a build argument leaves traces in $(routes_text)"; ok=0; }

  key=$(sed -n 's/^OPENROUTER_API_KEY=//p' "$ROOT/.env" 2>/dev/null | head -n 1 | tr -d '\r' || true)
  if [[ ${#key} -ge 8 ]]; then
    META_NEEDLES=$key
    FILE_NEEDLES=$key
    build "$ROOT" "damage-detector:check-$RUN_ID-real" --build-arg GIT_SHA=check || return 1
    scan_image "damage-detector:check-$RUN_ID-real" "$CTX/scan-real"
    [[ ${#ROUTES[@]} -eq 0 ]] || { echo "FAIL image-holds-no-key: the real key is in the shareable image ($(routes_text))"; ok=0; }
  fi
  key=""
  [[ $ok -eq 1 ]] && echo "PASS image-holds-no-key"
  [[ $ok -eq 1 ]]
}

# Each bad image holds a key in one way the scan must see; the clean one must pass.
self_test() {
  docker_ready self-test || return 1
  make_context
  local canary d name expected ok=1 base
  canary=$(new_canary)
  META_NEEDLES=$(printf '%s\n' "$canary" 'sk-or-' 'OPENROUTER_API_KEY')
  FILE_NEEDLES=$canary
  base=$(sed -n 's/^FROM \(eclipse-temurin:[^ ]*-jre-[^ ]*\).*/\1/p' "$ROOT/Dockerfile" | head -n 1)

  declare -A DOCKERFILES=(
    [configuration]=$'FROM scratch\nENV OPENROUTER_API_KEY='"$canary"
    [history]=$'FROM scratch\nARG OPENROUTER_API_KEY\nENV STRAY=1'
    [layer-env-file]=$'FROM '"$base"$'\nCOPY .env /app/.env\nRUN rm /app/.env'
    [layer-key]=$'FROM scratch\nCOPY note.txt /note.txt'
    [jar-key]=$'FROM scratch\nCOPY app.jar /app/app.jar'
    [clean]=$'FROM scratch\nCOPY note.txt /note.txt'
  )
  for name in configuration history layer-env-file layer-key jar-key clean; do
    d="$CTX/self/$name"
    mkdir -p "$d"
    printf '%s\n' "${DOCKERFILES[$name]}" >"$d/Dockerfile"
    printf 'OPENROUTER_API_KEY=%s\n' "$canary" >"$d/.env"
    if [[ $name == clean ]]; then echo hello >"$d/note.txt"; else printf 'key=%s\n' "$canary" >"$d/note.txt"; fi
    python3 -I -c 'import sys, zipfile
z = zipfile.ZipFile(sys.argv[1], "w", zipfile.ZIP_DEFLATED)
z.writestr("BOOT-INF/classes/k.yml", "key: " + sys.argv[2])
z.close()' "$d/app.jar" "$canary"
    local args=()
    [[ $name == history ]] && args=(--build-arg "OPENROUTER_API_KEY=$canary")
    build "$d" "damage-detector:selftest-$RUN_ID-$name" "${args[@]}" || { echo "FAIL self-test-$name: bad image did not build"; ok=0; continue; }
    scan_image "damage-detector:selftest-$RUN_ID-$name" "$CTX/scan-self-$name"
    if [[ $name == clean ]]; then
      [[ ${#ROUTES[@]} -eq 0 ]] && echo "PASS self-test-clean-image-passes" || { echo "FAIL self-test-clean-image-passes: $(routes_text)"; ok=0; }
    elif has_route "$name"; then
      echo "PASS self-test-detects-$name"
    else
      echo "FAIL self-test-detects-$name: the scan found ${ROUTES[*]:-nothing}"
      ok=0
    fi
  done
  [[ $ok -eq 1 ]]
}

requested=("${@:-${ORDER[@]}}")
for check in "${requested[@]}"; do
  if [[ "$check" != "--self-test" && -z "${FINDINGS[$check]:-}" ]]; then
    echo "Unknown check: ${check} (known: --self-test ${ORDER[*]})" >&2
    exit 64
  fi
  if [[ "$check" == "--self-test" || "$check" == "image-scan" ]]; then GUARD_ENV=1; fi
done
ENV_BEFORE=$(real_env_hash)

failed=0
for check in "${requested[@]}"; do
  case "$check" in
    --self-test) self_test || failed=1 ;;
    image-scan) image_scan || failed=1 ;;
    *) not_yet_implemented "$check" || failed=1 ;;
  esac
done
exit "$failed"
