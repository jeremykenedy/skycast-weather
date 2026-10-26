# Content requests and local data

The app has no ads, analytics, telemetry, crash reporting, tracking identifiers, remote configuration or update checks. Its only permission is INTERNET, used for the requested weather and city-search content. The bundled cloud film plays locally without streaming or additional content requests. No location permission is requested, and the app does not infer a location from an IP address. The user chooses a city or a local host explicitly supplies coordinates.

City searches send the entered search text to the fixed HTTPS Open-Meteo geocoding endpoint. Forecast requests send selected latitude and longitude rounded to four decimals, requested day count and fixed metric forecast fields to the fixed HTTPS weather endpoint. Requests include a generic software User-Agent, with no device, account or user identifier. Redirects are disabled, requests time out after ten seconds, and responses are bounded to 256KiB. The app cancels content requests when the scene stops or the settings search leaves the foreground. No background service polls for weather.

Open-Meteo necessarily receives the request's network address and content parameters. Its own service and privacy terms apply, including its documented logging policy. No claim is made that the content provider observes or stores nothing. The free endpoint is for noncommercial use. The Apache-2.0 source license does not remove those content-service conditions.

Selected city/coordinates, display choices and the most recent dated forecast are stored in the app's private Android storage. Android backup is disabled. Clear saved location and forecast removes the coordinates and private forecast cache. It preserves unrelated display choices. Forecasts refresh every fifteen minutes only while the scene is active; cached content remains dated and failed refreshes are labelled. Forecasts are model estimates, not emergency warnings.

The exported app-owned settings provider lets local apps query the schema, display choices and selected city label. It does not return the stored coordinates, forecast file, device identifiers, system settings or diagnostics. A local host can update documented choices, choose explicit coordinates or clear the saved location. It cannot run arbitrary commands or supply arbitrary content URLs.

The Python installer is separate from runtime. Explicit latest-release installation uses GitHub metadata and checksum-verified release downloads. A local APK needs no download. The installer does not alter Dream selection, timers, permissions, or security-update settings.

Static package checks pass for the local candidate, including the bundled film and offline Creative Commons license. Native request/cancellation observations, final merged-artifact review and physical gates remain pending. This source candidate is not a verified release.

Provider sources: [Open-Meteo weather documentation](https://open-meteo.com/en/docs), [city search](https://open-meteo.com/en/docs/geocoding-api), [terms and privacy](https://open-meteo.com/en/terms).
