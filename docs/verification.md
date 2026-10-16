# Verification and capture provenance

The current scene source was exercised on Google TV API 34, emulator-5580, host GPU, native view/capture 1920x1080, 2026-10-16. This is emulator evidence, not physical Fire TV or native 4K support.

The current temporal-volume scene capture APK is SHA-256 82315a3bc69cf2af466b3b66c37b7ecf4d7eca3ac8a1a09469a14bcd19e0be88. The signed resource candidate e5f7b0af51805b7320604b8bc7416f3765a2d3ecfbf98d7f56f3686367b2f74c adds embedded-preview and offline-license resources; they do not change that scene's geometry, shader, text layout or content transport.

| Image | Exact native provenance |
| --- | --- |
| weather-london-day-api34.png | Actual bound WeatherDreamService, temporal-volume candidate 82315a3b, real London 51.5074/-0.1278 request, metric/five days/Gentle; daytime returned by provider. |
| weather-new-york-night-api34.png | Actual bound WeatherDreamService, same scene candidate, real New York 40.7128/-74.006 request, imperial/seven days/Gentle; nighttime returned by provider. |
| location-search-api34.png | Actual native city results, candidate 1e579461, typed London, eight real matches returned; current Settings code unchanged by the later scene-only temporal pass. |
| weather-settings-api34.png | Native unconfigured Settings, current resource candidate e5f7b0af, metric/five days/Gentle, captured after embedded preview and full offline license resources were added. |
| weather-credits-api34.png | Current resource candidate e5f7b0af, D-pad seven steps from Preview to the exact credits button, Select opens native provider attribution and bundled Apache License; Back closes. |
| weather-settings-updated-api34.png | Native existing Settings resumed after local-host updates, candidate 1e579461; real selected London city, imperial/seven days reflected. |

Every source image was opened and viewed. The embedded 640x360 preview and 320x180 launcher banner are resized derivatives of the real current London Dream capture. No scene content was retouched. These are illustrative public test locations, not a claim about the user's location.

Native checks verified the initial Preview focus/Select path, continuous scene rendering, actual Android HTTPS forecast response, metric/five and imperial/seven layouts, invalid Kelvin rejection, D-pad city entry, keyboard dismissal, Find, real same-name city results, selection persistence, Settings refresh on resume, Back and Home. Explicit focus links fixed a city field skipped by Android TV navigation.

The actual Dream component was confirmed in dumpsys. A nonpersistent test used the verified exact-image Android 14 testDream Binder mapping, previously checked against AOSP's IDreamManager.aidl. This is not a portable installer command. Back first began waking, then a separate dumpsys read returned mCurrentDream=null. Selected component, enabled, activate-on-sleep, activate-on-dock and sleep-timeout values remained unchanged. App defaults and the unconfigured location/cache state were restored after each gate.

Actual ten-second day and five-second night native recordings were played in QuickTime at normal speed, with settled portions inspected while the timeline advanced. Distinct extracted frames were also viewed. Continuous evolving volume and slow text travel were visible. The source movies include short startup/loading transitions; only settled portions are scene evidence. Broad cloud shapes remain soft, and no claim of photographic weather reconstruction is made.

The own current GL SurfaceView BLAST layer's SurfaceFlinger latency snapshot contained 127 distinct valid actual-present timestamps over 4.254 seconds, about 29.619 presented buffers/second, median 33.489ms and maximum 50.183ms interval. Column order was verified against official Android 14 FrameTracker.cpp: desiredPresentTime, actualPresentTime, frameReadyTime. This short host-GPU sample was collected around recording; it is not screenshot FPS, display refresh rate, sustained performance or physical-device evidence.

Remaining: actual offline recovery and cancellation/resource observations, one/three-day and Flowing native extremes, remaining offline-license scrolling extremes, installer device path, final merged-artifact/runtime privacy review, public browser checks/CI, physical automatic idle/vendor navigation/thermal/buffer tests, release, both host integrations and replacement TV delivery. See native-validation.md and checkpoint.md.

Public branch browser review on October16,2026 verified all eight current banner variants: desktop1280 and mobile320, light/dark, animated/reduced-motion. Each visible image completed loading and selected its expected current hashed source. Animated variants were inspected over complete loops with multiple real-time frames; still variants stayed unchanged. Native gallery contains six actual1920x1080 images, all loaded at both widths with no page overflow. Media overrides were reset to No emulation, device toolbar stayed off, viewport was reset and owned tabs closed. No account theme preference was changed.
