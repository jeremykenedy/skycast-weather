<p align="center">
  <a href="https://github.com/jeremykenedy/skycast-weather#gh-light-mode-only"><picture><source media="(max-width: 600px) and (prefers-reduced-motion: reduce)" srcset="art/banner-mobile-still-light.22e19367bf54.svg"><source media="(prefers-reduced-motion: reduce)" srcset="art/banner-still-light.3d702736cede.svg"><source media="(max-width: 600px)" srcset="art/banner-mobile-light.bc5c046bcba7.svg"><img src="art/banner-light.7fe2c2d03b59.svg#gh-light-mode-only" alt="Skycast Weather" width="800"></picture></a>
  <a href="https://github.com/jeremykenedy/skycast-weather#gh-dark-mode-only"><picture><source media="(max-width: 600px) and (prefers-reduced-motion: reduce)" srcset="art/banner-mobile-still-dark.647e41b79bce.svg"><source media="(prefers-reduced-motion: reduce)" srcset="art/banner-still-dark.47ff6ec2c17e.svg"><source media="(max-width: 600px)" srcset="art/banner-mobile-dark.1f31c60b9916.svg"><img src="art/banner-dark.cb03ba2c5546.svg#gh-dark-mode-only" alt="Skycast Weather" width="800"></picture></a>
</p>

<p align="center">A native weather screensaver with live forecasts over real filmed cloud flight.</p>

<p align="center">
  <a href="https://github.com/jeremykenedy/skycast-weather/actions/workflows/ci.yml"><img src="https://github.com/jeremykenedy/skycast-weather/actions/workflows/ci.yml/badge.svg" alt="Build and coverage"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache--2.0-blue.svg" alt="Apache-2.0 license"></a>
</p>
<p align="center">
  <a href="https://github.com/jeremykenedy"><img src="https://img.shields.io/github/followers/jeremykenedy?label=Follow&amp;style=social" alt="Follow on GitHub"></a>
  <a href="https://github.com/jeremykenedy/skycast-weather/stargazers"><img src="https://img.shields.io/github/stars/jeremykenedy/skycast-weather?style=social" alt="Star on GitHub"></a>
  <a href="https://github.com/sponsors/jeremykenedy"><img src="https://img.shields.io/badge/GitHub-Sponsor-ea4aaa?logo=github" alt="Sponsor on GitHub"></a>
</p>

Show some love by [starring this repository on GitHub](https://github.com/jeremykenedy/skycast-weather).

Skycast Weather combines an explicitly selected city's current weather and forecast with a quiet flight through real clouds. The film is an ambient backdrop; it does not depict the selected city or its current conditions.

This is an unreleased candidate. Native emulator previews have been reviewed; physical Fire TV checks, release and host integration remain open. No production APK is advertised yet.

<p align="center"><img src="art/screenshots/weather-london-cinematic-five-api34.png" alt="Actual native London five-day forecast over filmed sunset cloud flight" width="960"></p>

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Platform Support](#platform-support)
- [Documentation](#documentation)
- [License](#license)

## Features

| Feature | Description | Guide |
| --- | --- | --- |
| Current weather and forecast | Real Open-Meteo data for a city you choose. | [Content and privacy](docs/privacy.md) |
| Filmed cloud flight | Bundled, silent footage with two playback speeds. | [Architecture](docs/architecture.md) |
| Forecast layout | Metric or imperial units and 1, 3, 5 or 7 days. | [Configuration](docs/configuration.md) |
| Dated cache | Preserves forecast age and labels failed refreshes. | [Content and privacy](docs/privacy.md) |
| Remote controls | Native city search, preview, settings and offline source credits. | [Configuration](docs/configuration.md) |
| Local host settings | A bounded app-owned interface for display choices and explicit location. | [Integration](docs/integration.md) |

## Requirements

- Android API 23 or newer with H.264 video decoding. Verified environments are listed below.
- Internet access for weather and city searches; the cloud film is bundled locally.
- An explicitly chosen location. No GPS permission or IP geolocation is used.
- Noncommercial use of the configured free Open-Meteo endpoint, under its separate service terms.

The app has no ads, analytics, tracking identifiers or background weather service. Weather requests disclose the chosen coordinates and network address to the provider. See [Privacy](docs/privacy.md).

## Installation

Before the first release, build and install your own signed APK:

~~~sh
bash build.sh
python3 install.py --serial TV_IP:5555 --apk build/skycast-weather.apk
~~~

The installer asks before changing the selected device. An update with the same package and signer preserves app preferences. It does not change the selected screensaver, timers or security-update settings. Choose Skycast Weather through the supported screensaver picker.

Build requirements, signing and removal are documented in [Building](docs/building.md). [Fire TV Toolkit](https://github.com/jeremykenedy/fire-tv-toolkit) and [Fire TV UI](https://github.com/jeremykenedy/fire-tv-ui) registration will follow the verified public release; this source candidate is not a downloadable catalog entry.

## Quick Start

Open Skycast Weather, search for a city, choose the intended result, then select Preview weather. After typing, Back hides the keyboard so the remote can reach Find. Choose metric or imperial units, a forecast range and Gentle or Flowing film motion.

Forecasts show their valid time and age. Missing data stays unavailable; a cached forecast keeps its original timestamp. Clear saved location and forecast removes the private location/cache while retaining display preferences. The bundled cloud lighting is independent of the forecast.

## Platform Support

| Environment | Current evidence |
| --- | --- |
| Google TV API 34 emulator | Native 1920x1080 previews, all four forecast layouts, real HTTPS data, settings, source credits and Back. Moving film reviewed at normal speed. |
| Physical Fire TV | Final candidate and actual idle Dream gate pending. |
| Other Android TV / Google TV | Not yet verified. |

Emulator captures do not establish physical performance, native 4K output, thermal behavior or automatic idle activation. Exact evidence and limits are in [Verification](docs/verification.md).

## Documentation

- [Configuration and local settings](docs/configuration.md)
- [Architecture](docs/architecture.md)
- [Content requests and privacy](docs/privacy.md)
- [Building, signing and coverage](docs/building.md)
- [Native screenshot gallery](docs/gallery.md)
- [Verification and capture provenance](docs/verification.md)
- [Remaining native scenarios](docs/native-validation.md)
- [Host integration contract](docs/integration.md)
- [Delivery checkpoint](docs/checkpoint.md)

## License

This package is open-sourced software licensed under the [Apache License 2.0](LICENSE). Weather, city data and the Creative Commons cloud film have separate attribution and conditions in [NOTICE](NOTICE).
