package com.jeremykenedy.skycastweather;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.Principal;
import java.security.cert.Certificate;
import javax.net.ssl.HttpsURLConnection;

public final class WeatherClientTest {
  private static int checks;
  private static FakeConnection next;
  private static WeatherClient cancelOnOpen;
  private static String opened;

  private static void check(boolean value, String message) {
    checks++;
    if (!value) throw new AssertionError(message);
  }

  private static final class FakeConnection extends HttpsURLConnection {
    int response = 200, declared = -1, disconnects;
    byte[] body = "{\"content\":true}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    WeatherClient cancelOnRead;
    boolean failRead, streamClosed;

    FakeConnection() throws Exception {
      super(new URL("https://api.open-meteo.com/v1/forecast"));
    }

    @Override
    public int getResponseCode() {
      return response;
    }

    @Override
    public int getContentLength() {
      return declared;
    }

    @Override
    public InputStream getInputStream() {
      return new ByteArrayInputStream(body) {
        @Override
        public synchronized int read(byte[] bytes, int offset, int length) {
          if (failRead) throw new IllegalStateException("read failure");
          if (cancelOnRead != null) cancelOnRead.close();
          return super.read(bytes, offset, length);
        }

        @Override
        public void close() throws IOException {
          streamClosed = true;
          super.close();
        }
      };
    }

    @Override
    public void disconnect() {
      disconnects++;
    }

    @Override
    public boolean usingProxy() {
      return false;
    }

    @Override
    public void connect() {}

    @Override
    public String getCipherSuite() {
      return "test";
    }

    @Override
    public Certificate[] getLocalCertificates() {
      return null;
    }

    @Override
    public Certificate[] getServerCertificates() {
      return null;
    }

    @Override
    public Principal getPeerPrincipal() {
      return null;
    }

    @Override
    public Principal getLocalPrincipal() {
      return null;
    }
  }

  private static void failed(WeatherClient client, String message) throws Exception {
    try {
      client.fetch(new WeatherLocation("Test", 0, 0), 3);
      throw new AssertionError(message);
    } catch (IOException expected) {
      checks++;
    }
  }

  public static void main(String[] args) throws Exception {
    URL.setURLStreamHandlerFactory(
        protocol ->
            !protocol.equals("https")
                ? null
                : new URLStreamHandler() {
                  @Override
                  protected URLConnection openConnection(URL url) {
                    opened = url.toString();
                    if (cancelOnOpen != null) cancelOnOpen.close();
                    return next;
                  }
                });
    next = new FakeConnection();
    WeatherClient client = new WeatherClient();
    check(
        client
            .fetch(new WeatherLocation("Public test point", 0, 0), 5)
            .equals("{\"content\":true}"),
        "actual content bytes returned unchanged");
    check(
        opened.startsWith("https://api.open-meteo.com/v1/forecast?")
            && opened.endsWith("forecast_days=5"),
        "only documented HTTPS content destination");
    check(
        next.getConnectTimeout() == 10000
            && next.getReadTimeout() == 10000
            && !next.getInstanceFollowRedirects(),
        "bounded timeouts and redirects disabled");
    check(
        next.getRequestProperty("Accept").equals("application/json")
            && next.getRequestProperty("User-Agent").startsWith("SkycastWeather/"),
        "content headers without device identifiers");
    check(next.disconnects == 1 && next.streamClosed, "resources released after success");
    next = new FakeConnection();
    check(
        client.findLocations("  São Paulo  ").equals("{\"content\":true}"),
        "geocoding content returned unchanged");
    check(
        opened.startsWith(
            "https://geocoding-api.open-meteo.com/v1/search?name=S%C3%A3o+Paulo&count=8"),
        "only encoded user city goes to documented geocoding endpoint");
    for (String query : new String[] {null, "x", new String(new char[101]).replace('\0', 'x')}) {
      try {
        client.findLocations(query);
        throw new AssertionError("invalid city query");
      } catch (IllegalArgumentException expected) {
        checks++;
      }
    }
    client.close();
    client.close();
    opened = null;
    failed(client, "closed client cannot restart network");
    check(opened == null, "no connection after close");
    for (int status : new int[] {302, 404, 500}) {
      next = new FakeConnection();
      next.response = status;
      failed(new WeatherClient(), "non200 content rejected");
      check(next.disconnects == 1, "failed response disconnected");
    }
    next = new FakeConnection();
    next.declared = 262145;
    failed(new WeatherClient(), "oversized declared response rejected");
    next = new FakeConnection();
    next.body = new byte[262145];
    failed(new WeatherClient(), "unbounded/chunked response still capped");
    check(next.streamClosed, "oversized stream released");
    next = new FakeConnection();
    client = new WeatherClient();
    next.cancelOnRead = client;
    failed(client, "cancelled response not published");
    check(next.disconnects >= 1 && next.streamClosed, "inflight cancellation releases resources");
    next = new FakeConnection();
    client = new WeatherClient();
    cancelOnOpen = client;
    failed(client, "cancellation while opening connection");
    check(next.disconnects == 1, "new connection immediately disconnected on race");
    cancelOnOpen = null;
    next = new FakeConnection();
    next.failRead = true;
    try {
      new WeatherClient().fetch(new WeatherLocation("Test", 0, 0), 1);
      throw new AssertionError("read failure");
    } catch (IllegalStateException expected) {
      checks++;
    }
    check(
        next.disconnects == 1 && next.streamClosed,
        "read exception still closes connection and stream");
    System.out.println("Weather transport checks passed: " + checks);
  }
}
