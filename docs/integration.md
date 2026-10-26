# Integration contract

Source candidate only. No release asset or verified host registration exists yet.

| Field | Value |
| --- | --- |
| Title | Skycast Weather |
| Saver/repository ID | skycast-weather |
| Repository | jeremykenedy/skycast-weather |
| Package | com.jeremykenedy.skycastweather |
| Dream | com.jeremykenedy.skycastweather/.WeatherDreamService |
| Fully qualified Dream | com.jeremykenedy.skycastweather/com.jeremykenedy.skycastweather.WeatherDreamService |
| Settings activity | com.jeremykenedy.skycastweather/.SettingsActivity |
| Explicit preview activity | com.jeremykenedy.skycastweather/.PreviewActivity |
| Settings provider | com.jeremykenedy.skycastweather.settings |
| Minimum API | 23 |
| Candidate version | 1.0.0 / code 3, final build pending |
| Planned release APK | skycast-weather.apk |
| Checksum asset | skycast-weather.apk.sha256, generated from the final signed APK |
| License | Apache-2.0 code; CC BY3.0 film; attributed provider content under its own terms |
| Scene preview source | art/screenshots/weather-london-cinematic-five-api34.png |

Preserve the unique production signer and package for updates. Publish only from verified remote main after the required release gates. Download and hash the actual published artifact before pinning Toolkit/UI metadata.

Toolkit registration and its exact install/select/update/remove commands will be verified as part of release integration. Never add a nonexistent release as downloadable. Fire TV UI dynamically discovers the installed Dream, provided its real service/metadata are correct. Its preview uses the normalized fully qualified component hash through Screensavers.previewFile(), not a competing naming convention.

The local settings interface and explicit public test coordinates are documented in configuration.md. There is no GPS/IP geolocation, arbitrary URL or shell-action interface. A missing location is a real unconfigured state; do not silently assign the user's location from a test preset.

Physical validation presets: London 51.5074/-0.1278, metric/five days/Gentle; New York 40.7128/-74.0060, imperial/seven days/Gentle. These make actual provider requests, so day/night and conditions depend on current provider content. Restore no-city/metric/five/Gentle after the test. Do not change a user's actual chosen location or system Dream state without preserving it.

Current cinematic capture artifact:1.0.0/code2, SHA2563b093626635e6ce2db23defeb2025355820cf072910368a1f90d29af975783c3. The next code3 build exports the explicit preview and embeds current native images. Neither candidate is a public release pin. Preserve the production signer listed in checkpoint.md.
