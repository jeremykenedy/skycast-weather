package com.jeremykenedy.skycastweather;

import java.math.BigDecimal;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class WeatherTest {
  private static int checks;

  private static void check(boolean value, String message) {
    checks++;
    if (!value) throw new AssertionError(message);
  }

  private static JSONObject sample() {
    return new JSONObject(
        "{\"timezone\":\"America/New_York\",\"current_units\":{\"temperature_2m\":\"°C\",\"wind_speed_10m\":\"km/h\"},\"daily_units\":{\"temperature_2m_min\":\"°C\",\"temperature_2m_max\":\"°C\"},\"current\":{\"time\":1800000000,\"temperature_2m\":20,\"apparent_temperature\":19,\"relative_humidity_2m\":55,\"wind_speed_10m\":16.09344,\"weather_code\":2,\"is_day\":1},\"daily\":{\"time\":[1800000000,1800086400],\"temperature_2m_min\":[10,11],\"temperature_2m_max\":[22,23],\"precipitation_probability_max\":[5,10],\"weather_code\":[2,61]}}");
  }

  private static WeatherSnapshot parse(JSONObject object, int days) throws JSONException {
    return WeatherSnapshot.parse(object.toString(), days, 1800000000000L);
  }

  private static void rejected(JSONObject object, String message) {
    try {
      parse(object, 3);
      throw new AssertionError(message);
    } catch (JSONException expected) {
      checks++;
    }
  }

  public static void main(String[] args) throws Exception {
    WeatherText metric = new WeatherText(parse(sample(), 3), false, 1800000000000L, "", Locale.US);
    check(
        metric.temperature.equals("20°C")
            && metric.summary.contains("16 km/h")
            && metric.age.endsWith("0 min old"),
        "metric labels and original valid age");
    check(
        metric.dates.length == 2
            && metric.ranges[0].equals("22° / 10°")
            && metric.conditions[1].equals("Rain"),
        "partial forecast labels match actual days");
    WeatherText imperial =
        new WeatherText(parse(sample(), 3), true, 1800003600000L, "Cached forecast", Locale.US);
    check(
        imperial.temperature.equals("68°F")
            && imperial.summary.contains("10 mph")
            && imperial.age.endsWith("Stale · Cached forecast"),
        "imperial content and honest stale cache label");
    JSONObject absent = sample();
    absent.getJSONObject("current").remove("temperature_2m");
    absent.getJSONObject("daily").put("weather_code", new JSONArray().put(96).put(-1));
    absent.getJSONObject("daily").remove("temperature_2m_max");
    WeatherText unavailable =
        new WeatherText(parse(absent, 3), false, 1800000000000L, "", Locale.US);
    check(
        unavailable.temperature.equals("--°C")
            && unavailable.ranges[0].startsWith("--°")
            && unavailable.conditions[0].equals("Storms with hail")
            && unavailable.conditions[1].equals("Unavailable"),
        "missing fields are unavailable rather than invented zero");
    JSONObject emptyDays = sample();
    emptyDays.getJSONObject("daily").put("time", new JSONArray());
    check(
        new WeatherText(parse(emptyDays, 3), false, 1800000000000L, "", Locale.US).dates.length
            == 0,
        "no fabricated days when provider has no forecast");
    WeatherLocation cachedCity = new WeatherLocation("New York", 40.7128, -74.006);
    String cached = WeatherCache.encode(cachedCity, sample().toString(), 3, 1800000000000L);
    WeatherSnapshot restored = WeatherCache.decode(cached, cachedCity, 3);
    check(
        restored.temperatureC == 20 && restored.fetchedMillis == 1800000000000L,
        "cache preserves actual content and original age");
    check(
        WeatherCache.decode(cached, new WeatherLocation("Elsewhere", 0, 0), 3) == null,
        "cache cannot cross selected locations");
    check(
        WeatherCache.decode(cached, cachedCity, 7) == null,
        "changed forecast range requires fresh content");
    check(
        WeatherCache.decode(cached, new WeatherLocation("Renamed", 40.7128, -74.006), 3) != null,
        "same coordinates can retain cache after label change");
    try {
      WeatherCache.encode(cachedCity, sample().toString(), 3, 0);
      throw new AssertionError("missing cache timestamp");
    } catch (IllegalArgumentException expected) {
      checks++;
    }
    try {
      WeatherCache.decode(new JSONObject(cached).put("fetched", 0).toString(), cachedCity, 3);
      throw new AssertionError("missing saved timestamp");
    } catch (JSONException expected) {
      checks++;
    }
    try {
      WeatherCache.decode("broken", cachedCity, 3);
      throw new AssertionError("corrupt cache");
    } catch (JSONException expected) {
      checks++;
    }
    try {
      WeatherCache.encode(cachedCity, "{}", 3, 1800000000000L);
      throw new AssertionError("invalid content cannot replace cache");
    } catch (JSONException expected) {
      checks++;
    }
    check(
        WeatherOptions.fallback("units").equals("metric")
            && WeatherOptions.fallback("days").equals("5")
            && WeatherOptions.fallback("motion").equals("gentle"),
        "stable defaults");
    for (String unit : new String[] {"metric", "imperial"})
      check(WeatherOptions.valid("units", unit), "supported units");
    for (String count : new String[] {"1", "3", "5", "7"})
      check(WeatherOptions.valid("days", count), "bounded forecast ranges");
    for (String motion : new String[] {"gentle", "flowing"})
      check(WeatherOptions.valid("motion", motion), "supported animation rates");
    check(
        !WeatherOptions.valid("units", "kelvin") && !WeatherOptions.valid("units", null),
        "reject unsupported units");
    check(
        !WeatherOptions.valid("days", "16") && !WeatherOptions.valid("days", null),
        "reject unsupported forecast count");
    check(
        !WeatherOptions.valid("motion", "frozen")
            && !WeatherOptions.valid("motion", null)
            && !WeatherOptions.valid("unknown", "gentle")
            && !WeatherOptions.valid(null, "gentle"),
        "reject unrelated setting keys");
    WeatherSnapshot normal = parse(sample(), 7);
    check(
        normal.temperatureC == 20
            && normal.apparentC == 19
            && normal.humidity == 55
            && normal.code == 2
            && normal.daylight,
        "actual fields preserved");
    check(
        normal.days.size() == 2
            && normal.days.get(1).lowC == 11
            && normal.days.get(1).highC == 23
            && normal.days.get(1).rainPercent == 10,
        "partial forecast stays partial");
    check(
        normal.days.get(0).epochSeconds == 1800000000,
        "epoch is not silently reinterpreted as local time");
    check(parse(sample(), 1).days.size() == 1, "configured day count limits visible data");
    check(
        !normal.stale(1800000000000L) && normal.stale(1800003600000L),
        "cached age distinguishes fresh and stale");
    check(normal.ageMillis(0) == 0, "backward clock does not produce negative age");
    WeatherSnapshot newer = WeatherSnapshot.parse(sample().toString(), 3, 1800001000000L);
    check(
        newer.ageMillis(1800001100000L) == 1100000L,
        "old provider time cannot be hidden by a fresh fetch");
    check(
        WeatherSnapshot.temperature(20, true) == 68 && WeatherSnapshot.temperature(20, false) == 20,
        "unit conversion only affects presentation");
    check(
        Math.abs(WeatherSnapshot.wind(16.09344, true) - 10) < .0001
            && WeatherSnapshot.wind(10, false) == 10,
        "wind conversion");
    try {
      normal.days.clear();
      throw new AssertionError("mutable forecast");
    } catch (UnsupportedOperationException expected) {
      checks++;
    }
    JSONObject missing = sample();
    missing.getJSONObject("current").remove("temperature_2m");
    missing.getJSONObject("current").put("is_day", 0);
    missing.getJSONObject("daily").remove("temperature_2m_min");
    missing.getJSONObject("daily").put("temperature_2m_max", new JSONArray().put(JSONObject.NULL));
    WeatherSnapshot partial = parse(missing, 7);
    check(
        Double.isNaN(partial.temperatureC)
            && Double.isNaN(partial.days.get(0).lowC)
            && Double.isNaN(partial.days.get(1).highC)
            && !partial.daylight,
        "missing/null/short arrays never become zero");
    for (Object code : new Object[] {JSONObject.NULL, -1, 100, 2.5, "2"}) {
      JSONObject unknown = sample();
      unknown.getJSONObject("current").put("weather_code", code);
      check(parse(unknown, 1).code == -1, "unknown code remains unavailable");
    }
    for (String number : new String[] {"1e999", "-1e999"}) {
      JSONObject overflow = sample();
      overflow.getJSONObject("current").put("temperature_2m", new BigDecimal(number));
      check(Double.isNaN(parse(overflow, 1).temperatureC), "numeric overflow remains unavailable");
    }
    for (double value : new double[] {-1000, 1000}) {
      JSONObject impossible = sample();
      for (String key :
          new String[] {
            "temperature_2m", "apparent_temperature", "relative_humidity_2m", "wind_speed_10m"
          }) impossible.getJSONObject("current").put(key, value);
      WeatherSnapshot invalid = parse(impossible, 1);
      check(
          Double.isNaN(invalid.temperatureC)
              && Double.isNaN(invalid.apparentC)
              && Double.isNaN(invalid.humidity)
              && Double.isNaN(invalid.windKmh),
          "physically impossible values stay unavailable");
    }
    for (String field : new String[] {"temperature_2m", "wind_speed_10m"}) {
      JSONObject bad = sample();
      bad.getJSONObject("current_units").put(field, "wrong");
      rejected(bad, "current units mismatch");
    }
    for (String field : new String[] {"temperature_2m_min", "temperature_2m_max"}) {
      JSONObject bad = sample();
      bad.getJSONObject("daily_units").put(field, "wrong");
      rejected(bad, "daily units mismatch");
    }
    for (long time : new long[] {0, Long.MAX_VALUE}) {
      JSONObject bad = sample();
      bad.getJSONObject("current").put("time", time);
      rejected(bad, "bad current timestamp");
      bad = sample();
      bad.getJSONObject("daily").getJSONArray("time").put(0, time);
      rejected(bad, "bad forecast timestamp");
    }
    JSONObject bad = sample().put("timezone", "Not/A_Real_Zone");
    rejected(bad, "timezone cannot silently become GMT");
    rejected(sample().put("error", true), "provider error");
    try {
      WeatherSnapshot.parse("not json", 3, 0);
      throw new AssertionError("invalid JSON");
    } catch (JSONException expected) {
      checks++;
    }
    for (int days : new int[] {0, 8}) {
      try {
        parse(sample(), days);
        throw new AssertionError("day count range");
      } catch (IllegalArgumentException expected) {
        checks++;
      }
    }
    int[] codes = {
      0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85,
      86, 95, 96, 99, -1, 4
    };
    for (int code : codes)
      check(!WeatherSnapshot.condition(code).isEmpty(), "WMO condition descriptions");
    check(
        WeatherSnapshot.condition(95).equals("Thunderstorms")
            && WeatherSnapshot.condition(-1).equals("Conditions unavailable"),
        "condition meanings");
    Locale old = Locale.getDefault();
    Locale.setDefault(Locale.GERMANY);
    try {
      WeatherLocation location = new WeatherLocation("  Test City  ", 40.7128, -74.006);
      String url = location.forecastUrl(7);
      check(
          location.name.equals("Test City") && url.contains("latitude=40.7128&longitude=-74.0060"),
          "locale-independent coordinates");
      check(
          url.endsWith("forecast_days=7") && url.contains("timeformat=unixtime"),
          "bounded explicit horizon and timestamp contract");
      check(
          location.cacheKey().equals("40.7128,-74.0060"),
          "cache identity independent of location label");
      for (int days : new int[] {0, 8})
        try {
          location.forecastUrl(days);
          throw new AssertionError("bad request horizon");
        } catch (IllegalArgumentException expected) {
          checks++;
        }
    } finally {
      Locale.setDefault(old);
    }
    for (String name : new String[] {null, " ", new String(new char[101]).replace('\0', 'x')})
      try {
        new WeatherLocation(name, 0, 0);
        throw new AssertionError("bad name");
      } catch (IllegalArgumentException expected) {
        checks++;
      }
    for (double[] point :
        new double[][] {
          {Double.NaN, 0},
          {0, Double.POSITIVE_INFINITY},
          {-91, 0},
          {91, 0},
          {0, -181},
          {0, 181},
          {Double.NEGATIVE_INFINITY, 0}
        })
      try {
        new WeatherLocation("City", point[0], point[1]);
        throw new AssertionError("bad coordinates");
      } catch (IllegalArgumentException expected) {
        checks++;
      }
    new WeatherLocation("North", 90, 180);
    new WeatherLocation("South", -90, -180);
    check(WeatherLocation.searchResults("{}").isEmpty(), "no matches is an empty search result");
    check(
        WeatherLocation.searchResults(
                "{\"results\":[null,{},"
                    + " {\"name\":\"Paris\",\"admin1\":\"Ile-de-France\",\"country\":\"France\",\"latitude\":48.85,\"longitude\":2.35}]}")
            .get(0)
            .name
            .equals("Paris, Ile-de-France, France"),
        "malformed entries do not hide a valid disambiguated city");
    check(
        WeatherLocation.searchResults(
                "{\"results\":[{\"name\":\"City\",\"latitude\":0,\"longitude\":0}]}")
            .get(0)
            .name
            .equals("City"),
        "optional region/country absent");
    JSONArray many = new JSONArray();
    for (int i = 0; i < 10; i++)
      many.put(
          new JSONObject()
              .put("name", new String(new char[120]).replace('\0', 'A'))
              .put("latitude", 0)
              .put("longitude", 0));
    java.util.List<WeatherLocation> matches =
        WeatherLocation.searchResults(new JSONObject().put("results", many).toString());
    check(
        matches.size() == 8 && matches.get(0).name.length() == 100,
        "search result count and label length bounded");
    System.out.println("Weather content checks passed: " + checks);
  }
}
