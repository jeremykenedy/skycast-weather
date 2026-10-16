package com.jeremykenedy.skycastweather;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AtomicFile;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

final class WeatherStore {
  private final SharedPreferences preferences;
  private final AtomicFile cache;

  WeatherStore(Context context) {
    preferences = context.getSharedPreferences("weather", Context.MODE_PRIVATE);
    cache = new AtomicFile(new File(context.getFilesDir(), "forecast.json"));
  }

  WeatherLocation location() {
    try {
      return new WeatherLocation(
          preferences.getString("location", ""),
          Double.parseDouble(preferences.getString("latitude", "NaN")),
          Double.parseDouble(preferences.getString("longitude", "NaN")));
    } catch (IllegalArgumentException invalid) {
      return null;
    }
  }

  void location(WeatherLocation location) {
    preferences
        .edit()
        .putString("location", location.name)
        .putString("latitude", Double.toString(location.latitude))
        .putString("longitude", Double.toString(location.longitude))
        .apply();
  }

  String choice(String key) {
    return preferences.getString(key, WeatherOptions.fallback(key));
  }

  void choice(String key, String value) {
    if (!WeatherOptions.valid(key, value))
      throw new IllegalArgumentException("Unsupported weather setting");
    preferences.edit().putString(key, value).apply();
  }

  int days() {
    return Integer.parseInt(choice("days"));
  }

  WeatherSnapshot cached(WeatherLocation location) {
    try {
      return WeatherCache.decode(
          new String(cache.readFully(), StandardCharsets.UTF_8), location, days());
    } catch (Exception unavailable) {
      return null;
    }
  }

  void cache(WeatherLocation location, String body, int requestedDays, long fetched)
      throws Exception {
    byte[] bytes =
        WeatherCache.encode(location, body, requestedDays, fetched)
            .getBytes(StandardCharsets.UTF_8);
    FileOutputStream output = cache.startWrite();
    try {
      output.write(bytes);
      cache.finishWrite(output);
    } catch (Exception failure) {
      cache.failWrite(output);
      throw failure;
    }
  }
}
