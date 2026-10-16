package com.jeremykenedy.skycastweather;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class WeatherSnapshot {
  public final long validSeconds, fetchedMillis;
  public final double temperatureC, apparentC, windKmh, humidity;
  public final int code;
  public final boolean daylight;
  public final String timezone;
  public final List<Day> days;

  public static final class Day {
    public final long epochSeconds;
    public final double lowC, highC, rainPercent;
    public final int code;

    private Day(long epochSeconds, double lowC, double highC, double rainPercent, int code) {
      this.epochSeconds = epochSeconds;
      this.lowC = lowC;
      this.highC = highC;
      this.rainPercent = rainPercent;
      this.code = code;
    }
  }

  private WeatherSnapshot(JSONObject current, String timezone, List<Day> days, long fetchedMillis)
      throws JSONException {
    validSeconds = current.getLong("time");
    if (validSeconds <= 0 || validSeconds > Long.MAX_VALUE / 1000)
      throw new JSONException("Missing current valid time");
    this.fetchedMillis = fetchedMillis;
    temperatureC = bounded(current.opt("temperature_2m"), -100, 70);
    apparentC = bounded(current.opt("apparent_temperature"), -150, 100);
    windKmh = bounded(current.opt("wind_speed_10m"), 0, 500);
    humidity = bounded(current.opt("relative_humidity_2m"), 0, 100);
    code = weatherCode(current.opt("weather_code"));
    daylight = current.optInt("is_day", 1) == 1;
    this.timezone = timezone;
    this.days = Collections.unmodifiableList(days);
  }

  public static WeatherSnapshot parse(String body, int requestedDays, long fetchedMillis)
      throws JSONException {
    if (requestedDays < 1 || requestedDays > 7)
      throw new IllegalArgumentException("Forecast day count out of range");
    JSONObject root = new JSONObject(body);
    if (root.optBoolean("error", false))
      throw new JSONException("Weather provider returned an error");
    JSONObject units = root.getJSONObject("current_units");
    if (!"°C".equals(units.optString("temperature_2m"))
        || !"km/h".equals(units.optString("wind_speed_10m")))
      throw new JSONException("Unexpected current units");
    JSONObject dailyUnits = root.getJSONObject("daily_units");
    if (!"°C".equals(dailyUnits.optString("temperature_2m_max"))
        || !"°C".equals(dailyUnits.optString("temperature_2m_min")))
      throw new JSONException("Unexpected forecast units");
    String timezone = root.getString("timezone");
    if (!validTimezone(timezone)) throw new JSONException("Unknown forecast timezone");
    JSONObject daily = root.getJSONObject("daily");
    JSONArray times = daily.getJSONArray("time");
    List<Day> days = new ArrayList<>();
    for (int i = 0; i < Math.min(requestedDays, times.length()); i++) {
      long time = times.getLong(i);
      if (time <= 0 || time > Long.MAX_VALUE / 1000)
        throw new JSONException("Missing forecast valid time");
      days.add(
          new Day(
              time,
              bounded(arrayValue(daily, "temperature_2m_min", i), -100, 70),
              bounded(arrayValue(daily, "temperature_2m_max", i), -100, 70),
              bounded(arrayValue(daily, "precipitation_probability_max", i), 0, 100),
              weatherCode(arrayValue(daily, "weather_code", i))));
    }
    return new WeatherSnapshot(root.getJSONObject("current"), timezone, days, fetchedMillis);
  }

  private static boolean validTimezone(String value) {
    for (String id : TimeZone.getAvailableIDs()) if (id.equals(value)) return true;
    return false;
  }

  private static Object arrayValue(JSONObject object, String key, int index) {
    JSONArray values = object.optJSONArray(key);
    return values == null ? null : values.opt(index);
  }

  private static double bounded(Object value, double minimum, double maximum) {
    double number = number(value);
    return number >= minimum && number <= maximum ? number : Double.NaN;
  }

  private static double number(Object value) {
    if (!(value instanceof Number)) return Double.NaN;
    double number = ((Number) value).doubleValue();
    return number <= Double.MAX_VALUE && number >= -Double.MAX_VALUE ? number : Double.NaN;
  }

  private static int weatherCode(Object value) {
    double number = number(value);
    if (Double.isNaN(number) || number != Math.rint(number) || number < 0 || number > 99) return -1;
    return (int) number;
  }

  public long ageMillis(long nowMillis) {
    return Math.max(0, Math.max(nowMillis - fetchedMillis, nowMillis - validSeconds * 1000));
  }

  public boolean stale(long nowMillis) {
    return ageMillis(nowMillis) > 45 * 60 * 1000;
  }

  public static double temperature(double celsius, boolean fahrenheit) {
    return fahrenheit ? celsius * 1.8 + 32 : celsius;
  }

  public static double wind(double kmh, boolean imperial) {
    return imperial ? kmh / 1.609344 : kmh;
  }

  public static String condition(int code) {
    switch (code) {
      case 0:
        return "Clear";
      case 1:
        return "Mostly clear";
      case 2:
        return "Partly cloudy";
      case 3:
        return "Overcast";
      case 45:
      case 48:
        return "Fog";
      case 51:
      case 53:
      case 55:
        return "Drizzle";
      case 56:
      case 57:
        return "Freezing drizzle";
      case 61:
      case 63:
      case 65:
        return "Rain";
      case 66:
      case 67:
        return "Freezing rain";
      case 71:
      case 73:
      case 75:
      case 77:
        return "Snow";
      case 80:
      case 81:
      case 82:
        return "Rain showers";
      case 85:
      case 86:
        return "Snow showers";
      case 95:
        return "Thunderstorms";
      case 96:
      case 99:
        return "Thunderstorms with hail";
      default:
        return "Conditions unavailable";
    }
  }
}
