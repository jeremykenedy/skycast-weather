package com.jeremykenedy.skycastweather;

import org.json.JSONException;
import org.json.JSONObject;

public final class WeatherCache {
  private WeatherCache() {}

  public static String encode(WeatherLocation location, String body, int days, long fetched)
      throws JSONException {
    if (fetched <= 0) throw new IllegalArgumentException("Missing fetch time");
    WeatherSnapshot.parse(body, days, fetched);
    return new JSONObject()
        .put("location", location.cacheKey())
        .put("body", body)
        .put("days", days)
        .put("fetched", fetched)
        .toString();
  }

  public static WeatherSnapshot decode(String content, WeatherLocation location, int days)
      throws JSONException {
    JSONObject entry = new JSONObject(content);
    if (!location.cacheKey().equals(entry.getString("location")) || entry.getInt("days") != days)
      return null;
    long fetched = entry.getLong("fetched");
    if (fetched <= 0) throw new JSONException("Missing cached fetch time");
    return WeatherSnapshot.parse(entry.getString("body"), days, fetched);
  }
}
