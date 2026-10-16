#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
FORMATTER="$ROOT/build/tools/google-java-format.jar"
mkdir -p "$ROOT/build/tools"
if [[ ! -f "$FORMATTER" ]]; then
  curl --proto '=https' --proto-redir '=https' -fsSL --retry 3 \
    https://github.com/google/google-java-format/releases/download/v1.37.0/google-java-format-1.37.0-all-deps.jar \
    -o "$FORMATTER"
fi
printf '%s  %s\n' 834b2a0c38cb774953322a84b5ca3f2f40dd3156650b3cd44d3b744345962f7a "$FORMATTER" | shasum -a 256 -c - >/dev/null
java -jar "$FORMATTER" --dry-run --set-exit-if-changed src/com/jeremykenedy/skycastweather/*.java tests/*.java
"$ROOT/build/python-coverage/bin/ruff" check .
"$ROOT/build/python-coverage/bin/ruff" format --check .
shellcheck build.sh test.sh scripts/*.sh
