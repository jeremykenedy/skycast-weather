"""Check the built package, required components and content-only permission boundary."""

import os
import re
import subprocess
import sys
import zipfile
from pathlib import Path

sdk = Path(os.environ.get("ANDROID_HOME", Path.home() / "Library/Android/sdk"))
aapt = sdk / "build-tools/36.0.0/aapt"
apk = Path(sys.argv[1])
badging = subprocess.check_output([str(aapt), "dump", "badging", str(apk)], text=True)
if not re.search(
    r"package: name='com\.jeremykenedy\.skycastweather' versionCode='1' versionName='1\.0\.0'",
    badging,
):
    raise SystemExit("Unexpected package or version")
for value in ("sdkVersion:'23'", "targetSdkVersion:'36'", "uses-gl-es: '0x20000'"):
    if value not in badging:
        raise SystemExit("Unexpected SDK or graphics requirement: " + value)
permissions = subprocess.check_output([str(aapt), "dump", "permissions", str(apk)], text=True)
requested = re.findall(r"uses-permission: name='([^']+)'", permissions)
if requested != ["android.permission.INTERNET"]:
    raise SystemExit("Only the content request INTERNET permission is allowed")
manifest = subprocess.check_output(
    [str(aapt), "dump", "xmltree", str(apk), "AndroidManifest.xml"], text=True
)
for value in (
    "android.permission.BIND_DREAM_SERVICE",
    "android.service.dreams.DreamService",
    ".WeatherDreamService",
    "com.jeremykenedy.skycastweather.settings",
):
    if value not in manifest:
        raise SystemExit("Missing required DreamService or settings provider: " + value)
for setting in ("allowBackup", "usesCleartextTraffic"):
    if not re.search(rf"android:{setting}[^\n]*=\(type 0x12\)0x0", manifest):
        raise SystemExit("Expected disabled setting: " + setting)
with zipfile.ZipFile(apk) as archive:
    if any(name.endswith((".jar", ".so")) for name in archive.namelist()):
        raise SystemExit("Unexpected bundled runtime library")
print("Verified package, SDK, Dream/provider, INTERNET-only permissions and local storage policy.")
