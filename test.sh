#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TOOLS="$ROOT/build/tools"
rm -rf "$ROOT/build/host-classes" "$ROOT/build/test-classes"
mkdir -p "$TOOLS" "$ROOT/build/host-classes" "$ROOT/build/test-classes"
if [[ ! -f "$TOOLS/json-host.jar" ]]; then
  curl --proto '=https' --proto-redir '=https' -fsSL --retry 3 \
    https://repo.maven.apache.org/maven2/org/json/json/20250517/json-20250517.jar -o "$TOOLS/json-host.jar"
fi
printf '%s  %s\n' 3ea61b2a06e31edf1c91134fe9106b0ebb16628be169f3db75bc7a2b06b45796 "$TOOLS/json-host.jar" | shasum -a 256 -c - >/dev/null
javac -cp "$TOOLS/json-host.jar" -d "$ROOT/build/host-classes" \
  "$ROOT/src/com/jeremykenedy/skycastweather/WeatherOptions.java" \
  "$ROOT/src/com/jeremykenedy/skycastweather/WeatherCache.java" \
  "$ROOT/src/com/jeremykenedy/skycastweather/WeatherText.java" \
  "$ROOT/src/com/jeremykenedy/skycastweather/SkyNoise.java" \
  "$ROOT/src/com/jeremykenedy/skycastweather/SkyHistory.java" \
  "$ROOT/src/com/jeremykenedy/skycastweather/WeatherSnapshot.java" \
  "$ROOT/src/com/jeremykenedy/skycastweather/WeatherLocation.java" \
  "$ROOT/src/com/jeremykenedy/skycastweather/WeatherClient.java"
javac -cp "$TOOLS/json-host.jar:$ROOT/build/host-classes" -d "$ROOT/build/test-classes" "$ROOT/tests/WeatherTest.java" "$ROOT/tests/WeatherClientTest.java"
for suite in WeatherTest WeatherClientTest; do
  java -cp "$ROOT/build/host-classes:$ROOT/build/test-classes:$TOOLS/json-host.jar" "com.jeremykenedy.skycastweather.$suite"
done
