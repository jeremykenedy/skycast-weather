package com.jeremykenedy.skycastweather;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class WeatherText {
  public final String temperature, condition, summary, age;
  public final String[] dates, ranges, conditions, rain;

  public WeatherText(
      WeatherSnapshot snapshot, boolean imperial, long now, String status, Locale locale) {
    temperature = temperature(snapshot.temperatureC, imperial, locale) + (imperial ? "F" : "C");
    condition = WeatherSnapshot.condition(snapshot.code);
    summary =
        "Feels like "
            + temperature(snapshot.apparentC, imperial, locale)
            + "    Wind "
            + value(WeatherSnapshot.wind(snapshot.windKmh, imperial), locale)
            + (imperial ? " mph" : " km/h")
            + "    Humidity "
            + value(snapshot.humidity, locale)
            + "%";
    TimeZone zone = TimeZone.getTimeZone(snapshot.timezone);
    SimpleDateFormat valid = new SimpleDateFormat("HH:mm", locale);
    valid.setTimeZone(zone);
    String label =
        "Valid "
            + valid.format(new Date(snapshot.validSeconds * 1000))
            + " · "
            + (snapshot.ageMillis(now) / 60000)
            + " min old";
    if (snapshot.stale(now)) label += " · Stale";
    if (!status.isEmpty()) label += " · " + status;
    age = label;
    int count = snapshot.days.size();
    dates = new String[count];
    ranges = new String[count];
    conditions = new String[count];
    rain = new String[count];
    SimpleDateFormat date = new SimpleDateFormat("EEE d", locale);
    date.setTimeZone(zone);
    for (int i = 0; i < count; i++) {
      WeatherSnapshot.Day day = snapshot.days.get(i);
      dates[i] = date.format(new Date(day.epochSeconds * 1000));
      ranges[i] =
          temperature(day.highC, imperial, locale)
              + " / "
              + temperature(day.lowC, imperial, locale);
      String description = WeatherSnapshot.condition(day.code);
      conditions[i] =
          description.equals("Conditions unavailable")
              ? "Unavailable"
              : description.equals("Thunderstorms with hail") ? "Storms with hail" : description;
      rain[i] = "Rain " + value(day.rainPercent, locale) + "%";
    }
  }

  private static String value(double number, Locale locale) {
    return Double.isNaN(number) ? "--" : String.format(locale, "%.0f", number);
  }

  private static String temperature(double celsius, boolean imperial, Locale locale) {
    return value(WeatherSnapshot.temperature(celsius, imperial), locale) + "°";
  }
}
