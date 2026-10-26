# Architecture

WeatherScene combines a bundled film in a TextureView with a separate native-resolution Canvas weather overlay. The background is an edited 34-second loop of real cloud flight above Lake Lucerne. It is ambient footage, independent of the selected city, day/night state or weather. Source lighting and camera banking are retained. The editorial dissolve is documented in NOTICE; this is not a naturally closed flight path.

SkyFilmView uses Android MediaPlayer and hardware video decoding. The primary film is 1920x1080 at25 source frames per second; a bundled1280x720 fallback is tried if primary playback fails. Gentle plays at0.75x and Flowing at1x. Slower playback holds source frames longer; it does not synthesize additional frames. Both files are muted. Center-crop preserves the source aspect ratio.

The overlay uses actual View dimensions, with slow bounded travel. A33ms callback schedules text redraws; that is not a measured FPS claim. Weather formatting updates when content/status changes or a minute elapses. Stop cancels callbacks and the active content request, releases the decoder and records playback position. The TextureView surface is released when destroyed. Resume prepares a new player and restores its position. If both files fail, the forecast remains visible over a dark background.

WeatherClient constructs only fixed HTTPS Open-Meteo weather/geocoding requests. It rejects redirects, limits responses and supports cancellation. WeatherLocation validates explicitly selected coordinates. WeatherSnapshot validates units, times, timezone and physical bounds; unknown data remains unavailable. WeatherText rounds and converts display units.

WeatherStore keeps display preferences and explicit location in private Android storage. AtomicFile protects the validated forecast cache. Cache identity includes location and requested range. Cached content preserves original model-valid and fetch times. The active scene refreshes every15minutes; failures retain dated content and label the unavailable refresh. No background service polls.

WeatherDreamService uses the Android DreamService lifecycle. SettingsActivity supplies remote-friendly city search, configuration, preview and offline credits. The exported PreviewActivity supports explicit host previews without changing the system selection. It accepts no arbitrary content URL or command. SettingsProvider exposes only the documented app settings contract.
