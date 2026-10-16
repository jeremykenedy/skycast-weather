package com.jeremykenedy.skycastweather;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

public final class PreviewActivity extends Activity {
  private WeatherScene scene;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    scene = new WeatherScene(this);
    setContentView(scene);
  }

  @Override
  protected void onResume() {
    super.onResume();
    scene.start();
  }

  @Override
  protected void onPause() {
    scene.stop();
    super.onPause();
  }
}
