# Project standards

- This is a standalone native weather DreamService with actual content and continuous sky animation.
- No ads, analytics, telemetry, identifiers, diagnostic reporting, remote configuration or update checks.
- Network requests are restricted to documented weather/geocoding content. No location permission or IP geolocation.
- Never present sample/random weather as current. Missing values are unavailable; cached values have honest age/offline status.
- Stop animation and cancel requests when the scene detaches. No background service polling.
- Preserve com.jeremykenedy.skycastweather and its unique release signer.
- Render within Android surface dimensions. Do not force 4K or infer performance from refresh rate.
- Require host coverage, native moving visual review, meaningful settings/lifecycle checks and artifact privacy review before release.
- Keep keys and device-state backups outside Git. Use Apache-2.0 and preserve notices.
