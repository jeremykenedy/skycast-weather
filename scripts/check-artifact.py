"""Check the built package, required components and content-only permission boundary."""

import hashlib
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
    r"package: name='com\.jeremykenedy\.skycastweather' versionCode='4' versionName='1\.0\.0'",
    badging,
):
    raise SystemExit("Unexpected package or version")
for value in ("sdkVersion:'23'", "targetSdkVersion:'36'"):
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
    root = Path(__file__).resolve().parent.parent
    for resource, source in (
        ("res/raw/apache_license.txt", root / "LICENSE"),
        ("assets/NOTICE.txt", root / "assets/NOTICE.txt"),
        ("assets/CC-BY-3.0.txt", root / "assets/CC-BY-3.0.txt"),
        ("res/drawable/screensaver_preview.jpg", root / "res/drawable/screensaver_preview.jpg"),
        ("res/drawable/banner.jpg", root / "res/drawable/banner.jpg"),
    ):
        if archive.read(resource) != source.read_bytes():
            raise SystemExit("Bundled license/preview differs from verified source: " + resource)
    for name in ("clouds-1080.mp4", "clouds-720.mp4"):
        item = archive.getinfo("assets/" + name)
        if item.compress_type != zipfile.ZIP_STORED:
            raise SystemExit("Bundled film must be directly readable by the hardware decoder")
        if (
            hashlib.sha256(archive.read(item)).digest()
            != hashlib.sha256((root / "assets" / name).read_bytes()).digest()
        ):
            raise SystemExit("Bundled film differs from licensed source: " + name)
dream = subprocess.check_output(
    [str(aapt), "dump", "xmltree", str(apk), "res/xml/dream.xml"], text=True
)
if "android:previewImage" not in dream or "android:settingsActivity" not in dream:
    raise SystemExit("Dream metadata must include the real preview and settings activity")
print("Verified package, SDK, Dream/provider, INTERNET-only permissions and local storage policy.")
