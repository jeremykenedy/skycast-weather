package com.jeremykenedy.skycastweather;

import android.service.dreams.DreamService;

public final class WeatherDreamService extends DreamService {
  private WeatherScene scene;

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();
    setInteractive(false);
    setFullscreen(true);
    setScreenBright(false);
    scene = new WeatherScene(this);
    setContentView(scene);
  }

  @Override
  public void onDreamingStarted() {
    super.onDreamingStarted();
    scene.start();
  }

  @Override
  public void onDreamingStopped() {
    if (scene != null) scene.stop();
    super.onDreamingStopped();
  }

  @Override
  public void onDetachedFromWindow() {
    if (scene != null) scene.stop();
    super.onDetachedFromWindow();
  }
}
