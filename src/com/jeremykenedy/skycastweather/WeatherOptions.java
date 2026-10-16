package com.jeremykenedy.skycastweather;

public final class WeatherOptions {
  private WeatherOptions() {}

  public static String fallback(String key) {
    return "units".equals(key) ? "metric" : "days".equals(key) ? "5" : "gentle";
  }

  public static boolean valid(String key, String value) {
    if ("units".equals(key)) return "metric".equals(value) || "imperial".equals(value);
    if ("days".equals(key))
      return "1".equals(value) || "3".equals(value) || "5".equals(value) || "7".equals(value);
    return "motion".equals(key) && ("gentle".equals(value) || "flowing".equals(value));
  }
}
