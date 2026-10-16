package com.jeremykenedy.skycastweather;

import java.util.Locale;

public final class WeatherLocation {
  public final String name;
  public final double latitude, longitude;

  public WeatherLocation(String name, double latitude, double longitude) {
    if (name == null || name.trim().isEmpty() || name.length() > 100)
      throw new IllegalArgumentException("Choose a location name");
    if (!finite(latitude)
        || !finite(longitude)
        || latitude < -90
        || latitude > 90
        || longitude < -180
        || longitude > 180) throw new IllegalArgumentException("Coordinates out of range");
    this.name = name.trim();
    this.latitude = latitude;
    this.longitude = longitude;
  }

  private static boolean finite(double value) {
    return !Double.isNaN(value) && !Double.isInfinite(value);
  }

  public String forecastUrl(int days) {
    if (days < 1 || days > 7) throw new IllegalArgumentException("Forecast day count out of range");
    return String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f",
            latitude,
            longitude)
        + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,is_day,weather_code,wind_speed_10m"
        + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
        + "&temperature_unit=celsius&wind_speed_unit=kmh&timezone=auto&timeformat=unixtime&forecast_days="
        + days;
  }

  public static java.util.List<WeatherLocation> searchResults(String body)
      throws org.json.JSONException {
    org.json.JSONArray results = new org.json.JSONObject(body).optJSONArray("results");
    java.util.List<WeatherLocation> locations = new java.util.ArrayList<>();
    if (results == null) return locations;
    for (int i = 0; i < Math.min(8, results.length()); i++) {
      org.json.JSONObject result = results.optJSONObject(i);
      if (result == null) continue;
      String label = result.optString("name", "");
      String region = result.optString("admin1", "");
      String country = result.optString("country", "");
      if (!region.isEmpty()) label += ", " + region;
      if (!country.isEmpty()) label += ", " + country;
      if (label.length() > 100) label = label.substring(0, 100);
      try {
        locations.add(
            new WeatherLocation(
                label,
                result.optDouble("latitude", Double.NaN),
                result.optDouble("longitude", Double.NaN)));
      } catch (IllegalArgumentException invalid) {
        // A malformed result must not hide other valid matches.
      }
    }
    return locations;
  }

  public String cacheKey() {
    return String.format(Locale.US, "%.4f,%.4f", latitude, longitude);
  }
}
