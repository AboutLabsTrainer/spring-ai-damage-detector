#!/usr/bin/env bash
# US-04 / K4 gate, outside ./mvnw verify. Steps and rules: feature-delta.md, "CI/CD pipeline outline".
#
# Usage: scripts/container-check.sh [--self-test | check ...]     (no argument runs every check)
# Prints one "PASS <finding>" or "FAIL <finding>: <why>" line per finding. Exit 0 only if every finding passed.
#   --self-test   proves the key scan on deliberately bad images before any scenario relies on it.
#   image-scan    builds the image from an isolated copy with a planted canary key and scans it (plus the real key, if any).
#   parity, evaluation, runtime-key, missing-key, exit-codes, ownership, forgotten-mount
#                 run the image and the jar against a stubbed Jev (WireMock 3.13.2, a Docker network, no real key).
#   CONTAINER_CHECK_IMAGES   space-separated fixtures, default "good/000.png cut_lead/000.png" (from src/test/resources).
#   CONTAINER_CHECK_IMAGE    run the checks against this existing image (rollback drill) instead of building one.
#   The jar run needs a host JDK (21). The jar is taken out of the image, so both runs use the same bytes.
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
GUARD_ENV=1
STUB=""
NET=""
CANARY=""
RUN_KEY=""
COPY=""
RUN_IMAGE=""
STUB_PORT=""
RC=0
# Digest read from Docker Hub 2026-11-09; the tag must equal wiremock.version in pom.xml.
WIREMOCK_IMAGE='wiremock/wiremock:3.13.2@sha256:0d4ecb3e4dc8213fd7a4d37d6a78f6e6b553a6d2e15bd51b0999781282ac61b3'
ENV_BEFORE=""

real_env_hash() {
  if [[ -f "$ROOT/.env" ]]; then sha256sum <"$ROOT/.env" | cut -d' ' -f1; else echo absent; fi
}

finish() {
  local code=$?
  trap - EXIT INT TERM
  [[ -n "${KEEP_CTX:-}" ]] && echo "kept $CTX" >&2
  [[ -n "$CTX" && -z "${KEEP_CTX:-}" ]] && { chmod -R u+rwX "$CTX" 2>/dev/null || true; rm -rf "$CTX"; }
  [[ -n "$STUB" ]] && { docker rm -f "$STUB" >/dev/null 2>&1 || true; }
  [[ -n "$NET" ]] && { docker network rm "$NET" >/dev/null 2>&1 || true; }
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

# Copy of the tree with a planted canary, built once as damage-detector:check-<run-id>. Sets CANARY, COPY.
prepare_copy() {
  [[ -z "$COPY" ]] || return 0
  make_context
  CANARY=$(new_canary)
  COPY="$CTX/copy"
  copy_tree "$COPY"
  printf 'OPENROUTER_API_KEY=%s\n' "$CANARY" >"$COPY/.env"
  mkdir -p "$COPY/src/main/resources"
  printf 'OPENROUTER_API_KEY=%s\n' "$CANARY" >"$COPY/src/main/resources/.env"
}

build_check_image() {
  prepare_copy
  [[ -z "$(docker images -q "damage-detector:check-$RUN_ID")" ]] || return 0
  build "$COPY" "damage-detector:check-$RUN_ID" --build-arg GIT_SHA=check
}

image_scan() {
  docker_ready image-holds-no-key || return 1
  local ok=1 key
  prepare_copy

  META_NEEDLES=$(printf '%s\n' "$CANARY" 'sk-or-' 'OPENROUTER_API_KEY')
  FILE_NEEDLES=$CANARY
  build_check_image || return 1
  scan_image "damage-detector:check-$RUN_ID" "$CTX/scan-copy"
  [[ ${#ROUTES[@]} -eq 0 ]] || { echo "FAIL image-holds-no-key: key traces in $(routes_text)"; ok=0; }

  # A stray ARG in the Dockerfile would put a --build-arg value into the history.
  build "$COPY" "damage-detector:check-$RUN_ID-arg" --build-arg GIT_SHA=check --build-arg "OPENROUTER_API_KEY=$CANARY" || return 1
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

# ---- Runs against a stubbed Jev ---------------------------------------------------------------------------------

# Fixtures below $CTX/in, copied from src/test/resources/transistor-images (good-000.png becomes good/000.png).
prepare_input() {
  [[ ! -d "$CTX/in" ]] || return 0
  local entry label name
  for entry in ${CONTAINER_CHECK_IMAGES:-good/000.png cut_lead/000.png}; do
    label=${entry%%/*}
    name=${entry#*/}
    mkdir -p "$CTX/in/$label"
    cp "src/test/resources/transistor-images/${label//_/-}-${name%.*}.png" "$CTX/in/$entry"
  done
}

# Sets RUN_IMAGE, STUB and STUB_PORT. $1 is the finding to blame when the environment is unusable.
prepare_runs() {
  local finding=$1 wanted i
  docker_ready "$finding" || return 1
  command -v curl >/dev/null || { echo "FAIL $finding: curl is needed to drive the Jev stub"; return 1; }
  command -v java >/dev/null || { echo "FAIL $finding: no java on the host for the jar run"; return 1; }
  make_context
  prepare_input
  [[ -n "$RUN_KEY" ]] || RUN_KEY=$(new_canary)
  if [[ -n "${CONTAINER_CHECK_IMAGE:-}" ]]; then
    RUN_IMAGE=$CONTAINER_CHECK_IMAGE
  else
    build_check_image || { echo "FAIL $finding: the image did not build"; return 1; }
    RUN_IMAGE="damage-detector:check-$RUN_ID"
  fi
  [[ ! -f "$CTX/app.jar" ]] && extract_jar
  [[ -z "$STUB" ]] || return 0
  wanted=$(sed -n 's#.*<wiremock.version>\(.*\)</wiremock.version>.*#\1#p' pom.xml | head -n 1)
  [[ "$WIREMOCK_IMAGE" == "wiremock/wiremock:$wanted@"* ]] \
    || { echo "FAIL $finding: the WireMock image tag differs from wiremock.version $wanted in pom.xml"; return 1; }
  NET="dd-check-$RUN_ID"
  docker network create "$NET" >/dev/null
  STUB="dd-wiremock-$RUN_ID"
  docker run -d --name "$STUB" --network "$NET" --network-alias wiremock -p 127.0.0.1::8080 "$WIREMOCK_IMAGE" --disable-banner --disable-http2-plain >/dev/null
  STUB_PORT=$(docker port "$STUB" 8080/tcp | head -n 1 | sed 's/.*://')
  for i in $(seq 1 120); do
    curl -sf "http://127.0.0.1:$STUB_PORT/__admin/mappings" >/dev/null && return 0
    sleep 0.5
  done
  echo "FAIL $finding: the Jev stub did not come up"
  return 1
}

extract_jar() {
  local cid
  cid=$(docker create "$RUN_IMAGE")
  docker cp "$cid:/app/app.jar" "$CTX/app.jar" >/dev/null
  docker rm "$cid" >/dev/null
}

# set_stubs normal|reject|falsepass: replaces every mapping and clears the request journal.
#   normal: good folder -> GOOD (p 0.98), other folders -> their own defect class. reject: 401 on every call.
#   falsepass: every image is judged GOOD.
set_stubs() {
  python3 -I - "$1" "$CTX/in" >"$CTX/stubs-$1.json" <<'PY'
import base64, glob, json, os, sys
mode, root = sys.argv[1], sys.argv[2]
PATH = "/api/alpha/decisions"
def answer(choice, probs):
    return {"status": 200, "headers": {"Content-Type": "application/json"},
            "jsonBody": {"model": "typesafe/jev-1.13-20260917",
                         "answers": {"defect": {"type": "choice", "choice": choice, "probabilities": probs, "confidence": 0.41}},
                         "usage": {"input_tokens": 3505, "output_tokens": 65, "cost": 0.0001}, "provider": "TypeSafe"}}
mappings = []
if mode == "reject":
    mappings.append({"request": {"method": "POST", "urlPath": PATH}, "response": {"status": 401}})
else:
    for image in sorted(glob.glob(root + "/*/*.png")):
        label = os.path.basename(os.path.dirname(image))
        url = "data:image/png;base64," + base64.b64encode(open(image, "rb").read()).decode()
        if label == "good" or mode == "falsepass":
            reply = answer("GOOD", {"GOOD": 0.98, "MISPLACED": 0.005, "DAMAGED_CASE": 0.005, "CUT_LEAD": 0.005, "BENT_LEAD": 0.005})
        else:
            reply = answer(label.upper(), {"GOOD": 0.12, label.upper(): 0.53})
        mappings.append({"request": {"method": "POST", "urlPath": PATH,
                                     "bodyPatterns": [{"matchesJsonPath": {"expression": "$.state[1].image_url.url", "equalTo": url}}]},
                         "response": reply})
json.dump({"mappings": mappings, "importOptions": {"duplicatePolicy": "OVERWRITE", "deleteAllNotInImport": True}}, sys.stdout)
PY
  curl -sf -X POST -H 'Content-Type: application/json' --data-binary "@$CTX/stubs-$1.json" \
    "http://127.0.0.1:$STUB_PORT/__admin/mappings/import" >/dev/null
  curl -sf -X DELETE "http://127.0.0.1:$STUB_PORT/__admin/requests" >/dev/null
}

stub_calls() {
  curl -sf -X POST -H 'Content-Type: application/json' -d '{"method":"POST"}' \
    "http://127.0.0.1:$STUB_PORT/__admin/requests/count" | python3 -I -c 'import json, sys; print(json.load(sys.stdin)["count"])'
}

# docker_run <log> <out|none> <key|none> <args...>: the documented command; RC holds the exit code.
# The key goes in through the environment (-e NAME only), never through an argument.
docker_run() {
  local log=$1 out=$2 key=$3
  shift 3
  local mounts=(-v "$CTX/in:/in:ro") keyarg=()
  [[ $out == none ]] || mounts+=(-v "$out:/out")
  [[ $key == none ]] || keyarg=(-e OPENROUTER_API_KEY)
  RC=0
  (
    if [[ $key == none ]]; then unset OPENROUTER_API_KEY; else export OPENROUTER_API_KEY=$key; fi
    docker run --rm --user "$(id -u):$(id -g)" --network "$NET" "${keyarg[@]}" "${mounts[@]}" "$RUN_IMAGE" "$@"
  ) >"$log" 2>&1 || RC=$?
}

# jar_run <log> <key> <args...>: the same jar the image holds, on the host.
jar_run() {
  local log=$1 key=$2
  shift 2
  RC=0
  ( export OPENROUTER_API_KEY=$key; java -jar "$CTX/app.jar" "$@" ) >"$log" 2>&1 || RC=$?
}

DOCKER_STUB="--detector.jev.base-url=http://wiremock:8080"
jar_stub() { echo "--detector.jev.base-url=http://127.0.0.1:$STUB_PORT"; }

fresh_dir() {
  mkdir -p "$CTX/runs/$1"
  echo "$CTX/runs/$1"
}

file_set() {
  (cd "$1" && find . -type f | sort)
}

# summary <report.json|evaluation.json>: the counts that must agree, never dates or paths.
summary() {
  python3 -I -c 'import json, sys
s = json.load(open(sys.argv[1]))
s = s.get("summary", s)
print({k: s.get(k) for k in ("total", "pass", "scrap", "review", "judged", "jevErrors")})' "$1"
}

parity() {
  prepare_runs report-files || return 1
  set_stubs normal
  local d j ok=1 fileset_equal=0 expected
  d=$(fresh_dir parity-docker)
  j=$(fresh_dir parity-jar)
  docker_run "$CTX/parity-docker.log" "$d" "$RUN_KEY" --input=/in --output=/out "$DOCKER_STUB"
  local docker_rc=$RC
  jar_run "$CTX/parity-jar.log" "$RUN_KEY" --input="$CTX/in" --output="$j" "$(jar_stub)"
  expected=(report.md report.json scrap/cut_lead/000.png scrap/cut_lead/000.png.txt)
  local f missing=""
  for f in "${expected[@]}"; do [[ -f "$d/$f" ]] || missing+=" $f"; done
  [[ "$(file_set "$d")" == "$(file_set "$j")" ]] && fileset_equal=1
  if [[ $docker_rc -eq 0 && -z "$missing" && $fileset_equal -eq 1 ]]; then echo "PASS report-files"
  else echo "FAIL report-files: exit $docker_rc, missing:${missing:- none}, same files as the jar: $fileset_equal"; ok=0; fi
  local sd sj
  sd=$(summary "$d/report.json" 2>/dev/null || echo unreadable)
  sj=$(summary "$j/report.json" 2>/dev/null || echo unreadable)
  if [[ "$sd" == "$sj" && "$sd" != unreadable && $RC -eq 0 ]]; then echo "PASS verdict-counts-equal"
  else echo "FAIL verdict-counts-equal: docker $sd, jar $sj"; ok=0; fi
  [[ $ok -eq 1 ]]
}

evaluation() {
  prepare_runs evaluation-files || return 1
  set_stubs normal
  local d j
  d=$(fresh_dir evaluation-docker)
  j=$(fresh_dir evaluation-jar)
  docker_run "$CTX/evaluation-docker.log" "$d" "$RUN_KEY" --evaluate --input=/in --output=/out/eval "$DOCKER_STUB"
  local docker_rc=$RC
  jar_run "$CTX/evaluation-jar.log" "$RUN_KEY" --evaluate --input="$CTX/in" --output="$j/eval" "$(jar_stub)"
  if [[ $docker_rc -eq 0 && -f "$d/eval/evaluation.md" && -f "$d/eval/evaluation.json" \
        && "$(file_set "$d")" == "$(file_set "$j")" ]]; then
    echo "PASS evaluation-files"
  else
    echo "FAIL evaluation-files: exit $docker_rc; docker wrote: $(file_set "$d" | tr '\n' ' ')"
    return 1
  fi
}

runtime_key() {
  prepare_runs no-key-in-run-output || return 1
  set_stubs normal
  local d key
  d=$(fresh_dir runtime-key)
  key=$(new_canary)
  docker_run "$CTX/runtime-key.log" "$d" "$key" --debug --input=/in --output=/out "$DOCKER_STUB"
  if [[ $RC -ne 0 || ! -f "$d/report.md" || ! -s "$CTX/runtime-key.log" ]]; then
    echo "FAIL no-key-in-run-output: the stub run did not produce a report and console output (exit $RC), so the scan proves nothing"
    return 1
  fi
  if printf '%s\n' "$key" | grep -rqF -f - "$d" "$CTX/runtime-key.log"; then
    echo "FAIL no-key-in-run-output: the marked key appears in the results or in the console"
    return 1
  fi
  echo "PASS no-key-in-run-output"
}

missing_key() {
  prepare_runs missing-key-refused || return 1
  set_stubs normal
  local d
  d=$(fresh_dir missing-key)
  docker_run "$CTX/missing-key.log" "$d" none --input=/in --output=/out "$DOCKER_STUB"
  if [[ $RC -eq 1 && "$(cat "$CTX/missing-key.log")" == "No Jev API key: set the OPENROUTER_API_KEY environment variable" \
        && -z "$(file_set "$d" | grep -v '^\./damage-detector\.log$' || true)" && "$(stub_calls)" == 0 ]]; then
    echo "PASS missing-key-refused"
  else
    echo "FAIL missing-key-refused: exit $RC, said: $(head -c 200 "$CTX/missing-key.log" | tr '\n' ' '), files written: $(file_set "$d" | tr '\n' ' '), Jev calls: $(stub_calls)"
    return 1
  fi
}

exit_codes() {
  prepare_runs nothing-judged-signal || return 1
  local d j ok=1 docker_rc jar_rc
  set_stubs reject
  d=$(fresh_dir exit-reject-docker)
  j=$(fresh_dir exit-reject-jar)
  docker_run "$CTX/exit-reject-docker.log" "$d" "$RUN_KEY" --input=/in --output=/out "$DOCKER_STUB"
  docker_rc=$RC
  jar_run "$CTX/exit-reject-jar.log" "$RUN_KEY" --input="$CTX/in" --output="$j" "$(jar_stub)"
  jar_rc=$RC
  [[ $docker_rc -eq 3 && $jar_rc -eq 3 ]] && echo "PASS nothing-judged-signal" \
    || { echo "FAIL nothing-judged-signal: Docker ended with $docker_rc, the jar with $jar_rc, both should end with 3"; ok=0; }

  set_stubs falsepass
  d=$(fresh_dir exit-falsepass-docker)
  j=$(fresh_dir exit-falsepass-jar)
  docker_run "$CTX/exit-falsepass-docker.log" "$d" "$RUN_KEY" --evaluate --input=/in --output=/out/eval "$DOCKER_STUB"
  docker_rc=$RC
  jar_run "$CTX/exit-falsepass-jar.log" "$RUN_KEY" --evaluate --input="$CTX/in" --output="$j/eval" "$(jar_stub)"
  jar_rc=$RC
  [[ $docker_rc -eq 2 && $jar_rc -eq 2 ]] && echo "PASS false-pass-signal" \
    || { echo "FAIL false-pass-signal: Docker ended with $docker_rc, the jar with $jar_rc, both should end with 2"; ok=0; }
  [[ $ok -eq 1 ]]
}

ownership() {
  prepare_runs output-deletable || return 1
  set_stubs normal
  local d foreign
  d=$(fresh_dir ownership)
  docker_run "$CTX/ownership.log" "$d" "$RUN_KEY" --input=/in --output=/out "$DOCKER_STUB"
  foreign=$(find "$d" ! -user "$(id -u)" | head -n 3 | tr '\n' ' ')
  if [[ $RC -eq 0 && -f "$d/report.md" && -z "$foreign" ]] && rm -rf "$d" 2>/dev/null && [[ ! -e "$d" ]]; then
    echo "PASS output-deletable"
  else
    echo "FAIL output-deletable: exit $RC, files not owned by the invoking user: ${foreign:-none}"
    return 1
  fi
}

forgotten_mount() {
  prepare_runs forgotten-mount-refused || return 1
  set_stubs normal
  docker_run "$CTX/forgotten-mount.log" none "$RUN_KEY" --input=/in --output=/out "$DOCKER_STUB"
  if [[ $RC -eq 1 && "$(wc -l <"$CTX/forgotten-mount.log")" -eq 1 && "$(cat "$CTX/forgotten-mount.log")" == "Cannot write to output folder /out"* \
        && "$(stub_calls)" == 0 ]]; then
    echo "PASS forgotten-mount-refused"
  else
    echo "FAIL forgotten-mount-refused: exit $RC, said: $(head -c 200 "$CTX/forgotten-mount.log" | tr '\n' ' '), Jev calls: $(stub_calls)"
    return 1
  fi
}


requested=("${@:-${ORDER[@]}}")
for check in "${requested[@]}"; do
  if [[ "$check" != "--self-test" && -z "${FINDINGS[$check]:-}" ]]; then
    echo "Unknown check: ${check} (known: --self-test ${ORDER[*]})" >&2
    exit 64
  fi
done
ENV_BEFORE=$(real_env_hash)

failed=0
for check in "${requested[@]}"; do
  case "$check" in
    --self-test) self_test || failed=1 ;;
    image-scan) image_scan || failed=1 ;;
    runtime-key) runtime_key || failed=1 ;;
    missing-key) missing_key || failed=1 ;;
    exit-codes) exit_codes || failed=1 ;;
    forgotten-mount) forgotten_mount || failed=1 ;;
    *) "$check" || failed=1 ;;
  esac
done
exit "$failed"
