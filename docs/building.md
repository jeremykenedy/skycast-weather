# Building and checks

Use JDK 21, Android SDK platform 36 and build-tools 36.0.0, Python 3.10 or newer, curl, zip, OpenSSL and ShellCheck. ANDROID_HOME can select the SDK. The build script uses Java8 language/runtime API signatures plus the Android SDK API stubs, then D8 desugaring. Runtime requires API 23 and H.264 decoding. Physical Fire TV API 28 and Google TV API 34 emulator evidence is qualified in [Verification](verification.md); other devices remain unverified.

Run:

~~~sh
bash scripts/test-coverage.sh
bash scripts/test-python-coverage.sh
bash scripts/check-style.sh
python3 scripts/check-docs.py
bash build.sh --unsigned
python3 scripts/check-artifact.py build/skycast-weather-unsigned.apk
~~~

The host JSON jar is pinned by SHA-256 and used only for tests. Android supplies org.json at runtime; no jar or native dependency is bundled. Coverage includes the explicit pure production content, transport, location, cache, options and text classes. The current report covers every line and branch in that boundary. Android storage, UI, lifecycle scheduling and graphics wrappers require native tests and are not presented as host-covered code. Installer logic has its own 100% line/branch gate. Test doubles are compiled separately from production coverage classes.

For a local signed build:

~~~sh
bash build.sh
~~~

The first signed build creates a unique protected key under ~/.android/skycast-weather.jks and a separate protected password file. Restore both for updates; do not replace a missing key. SKYCAST_WEATHER_KEYSTORE and SKYCAST_WEATHER_KEYPASS can select explicit paths. Keep signing material outside Git and public runners. CI only builds unsigned review artifacts.

The artifact checker verifies exact package/version, SDK requirements, Dream/provider declarations, INTERNET-only permission, disabled backup/cleartext traffic, and absence of bundled jars/native libraries, bundled film checksums and uncompressed media entries. It does not prove runtime request destinations, native lifecycle behavior, automatic idle activation, sustained performance or physical support. Those need the documented device gates.
