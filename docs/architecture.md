# Architecture

WeatherScene combines a bounded GLES cloud surface with a separate native-resolution Canvas overlay. The sky is a continuously advected and evolving three-dimensional density field, not an image card or slideshow. A packed, padded density atlas supplies filtered volume samples. Two alternating cloud-history buffers integrate temporal samples without sampling the current render target. History resets when the surface context or day/cover/rain state changes. The broad cloud forms remain deliberately soft; they are an illustrative backdrop rather than a live camera, radar map or atmospheric reconstruction of the selected city.

The scene requests a cloud buffer no wider than 960px. The overlay uses the actual native View dimensions and keeps weather text sharp. Source-requested buffer dimensions, display modes and movie dimensions are distinct measurements. No native 4K or physical-device performance is claimed.

The UI schedules render requests near thirty times per second. That is a scheduling target, not an FPS measurement. Weather strings update only when content/status changes or a minute elapses; formatting and date objects are not rebuilt every animation frame. Slow bounded movement repositions the text. Stop removes callbacks, cancels the current HTTPS request and pauses the GL view; a bounded time step prevents large motion jumps after resume.

WeatherClient only constructs fixed HTTPS Open-Meteo weather/geocoding requests. It rejects redirects, limits responses and supports cancellation. WeatherLocation validates explicit user-selected coordinates. WeatherSnapshot validates units, times, timezone and physical value bounds; unknown data stays unavailable. WeatherText performs presentation-only rounding and unit conversion.

WeatherStore persists display preferences and explicit location in private Android storage. A validated forecast envelope is written with AtomicFile. Cache identity includes coordinates and requested forecast range, so changing cities or day count cannot display another city's cached response as current. Cached age preserves both original model-valid and fetch times. No background service polls. The active scene refreshes every fifteen minutes; failures retain dated content and label the failed refresh.

WeatherDreamService uses the real DreamService lifecycle. SettingsActivity supplies remote-friendly configuration, actual city search and preview. SettingsProvider exposes only the documented scene-settings contract; it is not a shell bridge or arbitrary URL loader.
