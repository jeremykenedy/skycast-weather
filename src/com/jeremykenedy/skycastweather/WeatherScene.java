package com.jeremykenedy.skycastweather;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.FrameLayout;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class WeatherScene extends FrameLayout {
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final WeatherStore store;
  private final GLSurfaceView surface;
  private final SkyRenderer sky = new SkyRenderer();
  private final Overlay overlay;
  private WeatherLocation location;
  private WeatherSnapshot snapshot;
  private String status = "";
  private volatile int generation;
  private volatile WeatherClient client;
  private boolean running, imperial;
  private long previous;
  private float elapsed, speed;

  WeatherScene(Context context) {
    super(context);
    store = new WeatherStore(context);
    surface = new GLSurfaceView(context);
    surface.setEGLContextClientVersion(2);
    surface.setRenderer(sky);
    surface.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    addView(surface, new LayoutParams(-1, -1));
    overlay = new Overlay(context);
    addView(overlay, new LayoutParams(-1, -1));
    setBackgroundColor(Color.rgb(9, 20, 36));
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    // The weather overlay stays at native resolution; clouds use a bounded render surface.
    if (w > 0 && h > 0)
      surface
          .getHolder()
          .setFixedSize(
              Math.min(w, 960), Math.max(1, Math.round(h * Math.min(w, 960) / (float) w)));
  }

  void start() {
    if (running) return;
    running = true;
    location = store.location();
    imperial = "imperial".equals(store.choice("units"));
    speed = "flowing".equals(store.choice("motion")) ? 1.8f : 1;
    snapshot = null;
    status = location == null ? "Location not selected" : "Loading forecast";
    sky.daylight = 1;
    sky.cloudCover = .5f;
    sky.rain = 0;
    surface.onResume();
    previous = SystemClock.uptimeMillis();
    handler.post(tick);
    if (location != null) load(true);
  }

  void stop() {
    if (!running) return;
    running = false;
    generation++;
    handler.removeCallbacksAndMessages(null);
    WeatherClient request = client;
    if (request != null) request.close();
    surface.onPause();
  }

  @Override
  protected void onDetachedFromWindow() {
    stop();
    super.onDetachedFromWindow();
  }

  private final Runnable tick =
      new Runnable() {
        @Override
        public void run() {
          if (!running) return;
          long now = SystemClock.uptimeMillis();
          elapsed += Math.min(.1f, (now - previous) / 1000f) * speed;
          previous = now;
          sky.elapsed = elapsed;
          surface.requestRender();
          overlay.invalidate();
          handler.postDelayed(this, 33);
        }
      };

  private void load(boolean fromCache) {
    final int token = ++generation;
    final WeatherLocation requested = location;
    final int days = store.days();
    new Thread(
            () -> {
              WeatherSnapshot saved = fromCache ? store.cached(requested) : null;
              if (saved != null)
                handler.post(
                    () -> {
                      if (running && token == generation) apply(saved, "Cached forecast");
                    });
              long age =
                  saved == null
                      ? Long.MAX_VALUE
                      : Math.max(0, System.currentTimeMillis() - saved.fetchedMillis);
              if (age < 15 * 60 * 1000) {
                long delay = 15 * 60 * 1000 - age;
                handler.post(
                    () -> {
                      if (running && token == generation)
                        handler.postDelayed(() -> load(false), delay);
                    });
                return;
              }
              WeatherClient request = new WeatherClient();
              client = request;
              try {
                if (token != generation) {
                  request.close();
                  return;
                }
                String body = request.fetch(requested, days);
                long fetched = System.currentTimeMillis();
                WeatherSnapshot fresh = WeatherSnapshot.parse(body, days, fetched);
                if (token != generation) return;
                try {
                  store.cache(requested, body, days, fetched);
                } catch (Exception unavailable) {
                  /* Display usable content even if local storage is full. */
                }
                handler.post(
                    () -> {
                      if (running && token == generation) apply(fresh, "");
                    });
              } catch (Exception unavailable) {
                handler.post(
                    () -> {
                      if (running && token == generation)
                        status =
                            snapshot == null
                                ? "Weather unavailable"
                                : "Refresh unavailable; cached forecast";
                    });
              } finally {
                request.close();
                if (client == request) client = null;
                handler.post(
                    () -> {
                      if (running && token == generation)
                        handler.postDelayed(() -> load(false), 15 * 60 * 1000);
                    });
              }
            },
            "weather-content")
        .start();
  }

  private void apply(WeatherSnapshot fresh, String message) {
    snapshot = fresh;
    status = message;
    sky.daylight = fresh.daylight ? 1 : 0;
    sky.cloudCover = fresh.code == 0 ? .16f : fresh.code == 1 ? .3f : fresh.code == 2 ? .56f : .92f;
    sky.rain = fresh.code >= 51 ? .8f : 0;
  }

  private final class Overlay extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    Overlay(Context context) {
      super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
      float width = getWidth(),
          height = getHeight(),
          scale = Math.min(width / 1920f, height / 1080f);
      canvas.save();
      canvas.translate(
          width * .5f + (float) Math.sin(elapsed * .012f) * width * .012f,
          height * .5f + (float) Math.sin(elapsed * .009f) * height * .015f);
      canvas.scale(scale, scale);
      paint.setShader(
          new LinearGradient(
              0,
              -440,
              0,
              450,
              new int[] {0x20050912, 0x90050912, 0xc0050912},
              null,
              Shader.TileMode.CLAMP));
      canvas.drawRoundRect(-835, -410, 835, 420, 36, 36, paint);
      paint.setShader(null);
      paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
      text(
          canvas, location == null ? "Skycast Weather" : location.name, -760, -315, 36, 0xe8ffffff);
      if (snapshot == null) {
        text(canvas, status, -760, -125, 76, Color.WHITE);
        if (location == null)
          text(canvas, "Choose a city in weather settings", -760, -40, 30, 0xcaffffff);
      } else {
        text(canvas, temperature(snapshot.temperatureC), -760, -110, 148, Color.WHITE);
        text(canvas, WeatherSnapshot.condition(snapshot.code), -750, -25, 44, Color.WHITE);
        String wind =
            value(WeatherSnapshot.wind(snapshot.windKmh, imperial)) + (imperial ? " mph" : " km/h");
        text(
            canvas,
            "Feels like "
                + temperature(snapshot.apparentC)
                + "    Wind "
                + wind
                + "    Humidity "
                + value(snapshot.humidity)
                + "%",
            -748,
            35,
            27,
            0xd8ffffff);
        TimeZone zone = TimeZone.getTimeZone(snapshot.timezone);
        SimpleDateFormat valid = new SimpleDateFormat("HH:mm", Locale.getDefault());
        valid.setTimeZone(zone);
        String age =
            "Valid "
                + valid.format(new Date(snapshot.validSeconds * 1000))
                + " · "
                + (snapshot.ageMillis(System.currentTimeMillis()) / 60000)
                + " min old";
        if (snapshot.stale(System.currentTimeMillis())) age += " · Stale";
        if (!status.isEmpty()) age += " · " + status;
        text(canvas, age, -748, 90, 24, 0xbfffffff);
        int count = snapshot.days.size();
        float step = 1510f / Math.max(1, count);
        SimpleDateFormat date = new SimpleDateFormat("EEE d", Locale.getDefault());
        date.setTimeZone(zone);
        for (int i = 0; i < count; i++) {
          WeatherSnapshot.Day day = snapshot.days.get(i);
          float x = -750 + i * step;
          text(canvas, date.format(new Date(day.epochSeconds * 1000)), x, 205, 28, Color.WHITE);
          text(
              canvas,
              temperature(day.highC) + " / " + temperature(day.lowC),
              x,
              260,
              count > 5 ? 27 : 34,
              Color.WHITE);
          text(canvas, shortCondition(day.code), x, 304, count > 5 ? 19 : 24, 0xd8ffffff);
          text(canvas, "Rain " + value(day.rainPercent) + "%", x, 345, 22, 0xbfffffff);
        }
      }
      text(canvas, "Open-Meteo · Model forecast", -748, 385, 18, 0x9fffffff);
      canvas.restore();
    }

    private String shortCondition(int code) {
      String condition = WeatherSnapshot.condition(code);
      return condition.equals("Conditions unavailable")
          ? "Unavailable"
          : condition.equals("Thunderstorms with hail") ? "Storms with hail" : condition;
    }

    private String value(double number) {
      return Double.isNaN(number) ? "--" : String.format(Locale.getDefault(), "%.0f", number);
    }

    private String temperature(double celsius) {
      return value(WeatherSnapshot.temperature(celsius, imperial)) + "°";
    }

    private void text(Canvas canvas, String value, float x, float y, float size, int color) {
      paint.setTextSize(size);
      paint.setColor(color);
      if (paint.measureText(value) > 1510) {
        int count = paint.breakText(value, true, 1470, null);
        value = value.substring(0, count) + "...";
      }
      canvas.drawText(value, x, y, paint);
    }
  }
}
