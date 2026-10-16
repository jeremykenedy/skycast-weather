# Pending native gates

Two initial Google TV API34 native gates rendered genuine New York and London forecasts at native 1920x1080 view resolution. Current scene captures and three frames from its six-second motion recording were opened and inspected; parent visual acceptance remains pending. The cloud render surface is bounded to 960px width; text remains at the native view resolution. Neither a 4K display mode nor a captured movie frame rate proves the render surface or application FPS. See checkpoint.md for exact candidate/capture provenance and remaining gates below.

Use only a fresh root-coordinated emulator lease. Read /tmp/screensaver-emulator-5580-owner.json before mutations. No physical TV installation is authorized while its external workflow is held.

1. Preserve five secure Dream/timer keys and the app's location/units/day/motion values. Install only the correctly signed package. Launch real settings and verify initial Preview focus and D-pad city search/results selection.
2. Inspect the unconfigured scene: no guessed city, simulated current values or blank screen. Set an explicit public test location through city search or the documented provider, never through IP geolocation.
3. Open native Preview using the remote, wait for the real bounded response and inspect location, units, valid time, current condition and forecast dates. Capture a native timed clip and inspect distinct frames for actual cloud movement, raymarch artifacts, text clipping and readability. Review pacing independently from screen recording FPS.
4. Exercise metric/imperial, 1/3/5/7 days and both cloud rates. Verify changing day count invalidates the old range cache. Check long city labels and missing data. Reject invalid host writes without changing values.
5. Pause/resume and Back/Home. Verify content workers cancel and renderer stops after leaving. Restart from cache; original valid/fetch times remain intact. After a real failed request, show cached age and refresh unavailable. A source mock alone does not establish native network behavior.
6. Bind the actual Dream on the verified emulator image without persisting selection. Inspect its component, pixels and Back exit. Do not assume hidden Binder mappings work on another OS.
7. Restore app preferences, including clear_location=true if the initial app had no saved location. Read back all five secure keys and confirm unchanged; report any external change rather than overwriting it. Save source screenshots and derive the small in-app preview only from accepted actual native content.

Settings host interface: content://com.jeremykenedy.skycastweather.settings/schema and /settings, key/value query rows. Updates accept units=metric|imperial, days=1|3|5|7 and motion=gentle|flowing. A location update supplies location_name, latitude and longitude together. To remove the saved city and forecast, send clear_location=true alone. Other paths/keys/values are rejected.

Physical automatic idle activation, Fire OS Back/Menu/Home behavior, sustained thermal/power/performance, actual target buffer sizes, installer device flow and ecosystem integration remain separate required gates.
