package com.jeremykenedy.skycastweather;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.FrameLayout;
import java.util.Locale;

final class WeatherScene extends FrameLayout {
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final WeatherStore store;
  private final SkyFilmView sky;
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
    sky = new SkyFilmView(context);
    addView(sky, new LayoutParams(-1, -1));
    overlay = new Overlay(context);
    addView(overlay, new LayoutParams(-1, -1));
    setBackgroundColor(Color.rgb(9, 20, 36));
  }

  void start() {
    if (running) return;
    running = true;
    location = store.location();
    imperial = "imperial".equals(store.choice("units"));
    speed = "flowing".equals(store.choice("motion")) ? 1 : .75f;
    snapshot = null;
    status = location == null ? "Location not selected" : "Loading forecast";
    sky.start(speed);
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
    sky.stop();
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
  }

  private final class Overlay extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Shader vertical, horizontal;
    private WeatherSnapshot formattedSnapshot;
    private WeatherText formatted;
    private long formattedMinute;
    private String formattedStatus = "";

    Overlay(Context context) {
      super(context);
      paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
      vertical =
          new LinearGradient(
              0,
              0,
              0,
              height,
              new int[] {0x10050912, 0x20050912, 0xb0050912},
              null,
              Shader.TileMode.CLAMP);
      horizontal =
          new LinearGradient(0, 0, width, 0, 0x65050912, 0x00050912, Shader.TileMode.CLAMP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
      float width = getWidth(),
          height = getHeight(),
          scale = Math.min(width / 1920f, height / 1080f);
      paint.clearShadowLayer();
      paint.setShader(vertical);
      canvas.drawRect(0, 0, width, height, paint);
      paint.setShader(horizontal);
      canvas.drawRect(0, 0, width, height, paint);
      paint.setShader(null);
      canvas.save();
      canvas.translate(
          width * .5f + (float) Math.sin(elapsed * .012f) * width * .012f,
          height * .5f + (float) Math.sin(elapsed * .009f) * height * .015f);
      canvas.scale(scale, scale);
      paint.setShadowLayer(2, 0, 1, 0x80000000);
      text(
          canvas, location == null ? "Skycast Weather" : location.name, -760, -315, 36, 0xe8ffffff);
      if (snapshot == null) {
        text(canvas, status, -760, -125, 76, Color.WHITE);
        if (location == null)
          text(canvas, "Choose a city in weather settings", -760, -40, 30, 0xcaffffff);
      } else {
        long now = System.currentTimeMillis();
        long minute = now / 60000;
        if (formattedSnapshot != snapshot
            || minute != formattedMinute
            || !formattedStatus.equals(status)) {
          formatted = new WeatherText(snapshot, imperial, now, status, Locale.getDefault());
          formattedSnapshot = snapshot;
          formattedMinute = minute;
          formattedStatus = status;
        }
        text(canvas, formatted.temperature, -760, -110, 148, Color.WHITE);
        text(canvas, formatted.condition, -750, -25, 44, Color.WHITE);
        text(canvas, formatted.summary, -748, 35, 27, 0xd8ffffff);
        text(canvas, formatted.age, -748, 90, 24, 0xbfffffff);
        int count = formatted.dates.length;
        float step = 1510f / Math.max(1, count);
        for (int i = 0; i < count; i++) {
          float x = -750 + i * step;
          text(canvas, formatted.dates[i], x, 205, 28, Color.WHITE);
          text(canvas, formatted.ranges[i], x, 260, count > 5 ? 27 : 34, Color.WHITE);
          text(canvas, formatted.conditions[i], x, 304, count > 5 ? 19 : 24, 0xd8ffffff);
          text(canvas, formatted.rain[i], x, 345, 22, 0xbfffffff);
        }
      }
      text(canvas, "Open-Meteo · Model forecast", -748, 385, 18, 0x9fffffff);
      canvas.restore();
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
