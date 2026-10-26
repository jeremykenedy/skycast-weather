#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS="$ROOT/build/tools"
mkdir -p "$TOOLS"
fetch() {
  local file="$1" url="$2" expected="$3"
  if [[ ! -f "$file" ]]; then
    curl --proto '=https' --proto-redir '=https' -fsSL --retry 3 --connect-timeout 20 "$url" -o "$file"
  fi
  printf '%s  %s\n' "$expected" "$file" | shasum -a 256 -c - >/dev/null
}
fetch "$TOOLS/jacoco-agent.jar" \
  https://repo.maven.apache.org/maven2/org/jacoco/org.jacoco.agent/0.8.15/org.jacoco.agent-0.8.15-runtime.jar \
  fb5b0036a0899ea97edfa0fc2c7985b55f3f7c5695028163e5e60d4f3cf6075d
fetch "$TOOLS/jacoco-cli.jar" \
  https://repo.maven.apache.org/maven2/org/jacoco/org.jacoco.cli/0.8.15/org.jacoco.cli-0.8.15-nodeps.jar \
  d2b74b20b415163c1f53261e7c5ef4445b788327094208ff821e0c2baf9bc8f1
bash "$ROOT/test.sh"
rm -f "$ROOT/build/weather.exec"
for suite in WeatherTest WeatherClientTest; do
  java "-javaagent:$TOOLS/jacoco-agent.jar=destfile=$ROOT/build/weather.exec" \
    -cp "$ROOT/build/host-classes:$ROOT/build/test-classes:$TOOLS/json-host.jar" "com.jeremykenedy.skycastweather.$suite"
done
# Android supplies org.json at runtime; the pinned host-only jar is not bundled.
# Include explicit pure content/cache/options/text production classes only.
# Android storage, UI, lifecycle scheduling and media wrappers require native verification.
java -jar "$TOOLS/jacoco-cli.jar" report "$ROOT/build/weather.exec" \
  --classfiles "$ROOT/build/host-classes" --sourcefiles "$ROOT/src" \
  --xml "$ROOT/build/weather-coverage.xml" --html "$ROOT/build/weather-coverage"
python3 "$ROOT/scripts/check-coverage.py" "$ROOT/build/weather-coverage.xml"
