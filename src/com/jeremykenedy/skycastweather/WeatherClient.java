package com.jeremykenedy.skycastweather;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

public final class WeatherClient {
  private volatile HttpsURLConnection active;
  private volatile boolean closed;

  public String fetch(WeatherLocation location, int days) throws IOException {
    return fetchUrl(location.forecastUrl(days));
  }

  public String findLocations(String query) throws IOException {
    if (query == null || query.trim().length() < 2 || query.length() > 100)
      throw new IllegalArgumentException("Enter a city name between 2 and 100 characters");
    return fetchUrl(
        "https://geocoding-api.open-meteo.com/v1/search?name="
            + java.net.URLEncoder.encode(query.trim(), "UTF-8")
            + "&count=8&language=en&format=json");
  }

  private String fetchUrl(String address) throws IOException {
    if (closed) throw new IOException("Weather request cancelled");
    HttpsURLConnection connection = (HttpsURLConnection) new URL(address).openConnection();
    active = connection;
    if (closed) {
      connection.disconnect();
      throw new IOException("Weather request cancelled");
    }
    try {
      connection.setConnectTimeout(10000);
      connection.setReadTimeout(10000);
      connection.setInstanceFollowRedirects(false);
      connection.setRequestProperty("Accept", "application/json");
      connection.setRequestProperty(
          "User-Agent", "SkycastWeather/1.0 (https://github.com/jeremykenedy/skycast-weather)");
      if (connection.getResponseCode() != 200) throw new IOException("Weather content unavailable");
      if (connection.getContentLength() > 262144)
        throw new IOException("Weather response too large");
      try (InputStream input = connection.getInputStream();
          ByteArrayOutputStream output = new ByteArrayOutputStream()) {
        byte[] buffer = new byte[4096];
        int length;
        while ((length = input.read(buffer)) != -1) {
          if (closed) throw new IOException("Weather request cancelled");
          if (output.size() + length > 262144) throw new IOException("Weather response too large");
          output.write(buffer, 0, length);
        }
        return output.toString("UTF-8");
      }
    } finally {
      connection.disconnect();
      active = null;
    }
  }

  public void close() {
    closed = true;
    HttpsURLConnection connection = active;
    if (connection != null) connection.disconnect();
  }
}
