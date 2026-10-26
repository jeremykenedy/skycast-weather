# Settings and local host interface

| Setting | Choices | Default |
| --- | --- | --- |
| Location | Explicit city-search result, or explicit host-supplied name/coordinates | None |
| Temperature and wind | Celsius / km/h; Fahrenheit / mph | Metric |
| Forecast range | 1, 3, 5 or 7 days | 5 |
| Cloud motion | Gentle; Flowing | Gentle |
| Clear saved location and forecast | Removes private coordinates and cached forecast; preserves display choices | User action |

Weather content, units and location are never randomized. Gentle plays the bundled film at0.75x; Flowing uses1x. Both also change the slow text-motion rate, never timestamps or weather values. The bundled background also plays before a city is selected. Its recorded cloud cover and lighting do not change to match the forecast.

Choose a city in Settings. D-pad reaches Preview, Clear, city text entry and Find. The keyboard can use its Search action; after typing, Back hides it so D-pad can reach Find. The result dialog distinguishes same-name places by region and country. Selection persists locally. Back leaves Preview, Home returns to the launcher, and the supported platform screensaver picker controls actual selection.

Local provider:

- content://com.jeremykenedy.skycastweather.settings/schema
- content://com.jeremykenedy.skycastweather.settings/settings

Queries return key/value cursor rows. The schema describes units, days, motion and explicit-location updates. Settings returns current display choices and city label, not stored coordinates or the forecast file.

Updates accept units=metric|imperial, days=1|3|5|7, and motion=gentle|flowing. A location update supplies location_name, latitude and longitude together. Latitude must be finite and within ±90; longitude within ±180; a nonempty label is required. Unsupported paths, keys or values are rejected before changing settings. Send clear_location=true by itself to remove the saved location/cache.

For an explicit public test location:

~~~sh
adb -s emulator-5580 shell content query --uri content://com.jeremykenedy.skycastweather.settings/schema
adb -s emulator-5580 shell content update --uri content://com.jeremykenedy.skycastweather.settings/settings --bind location_name:s:London --bind latitude:d:51.5074 --bind longitude:d:-0.1278
adb -s emulator-5580 shell content update --uri content://com.jeremykenedy.skycastweather.settings/settings --bind units:s:imperial --bind days:s:7
~~~

Use the device's real ADB serial; emulator-5580 is only the documented test environment. Set a location you intend to request. Do not infer user coordinates or change system Dream/timer settings through this interface. Fire TV UI's current installed-service picker does not automatically expose this app-specific editor.
