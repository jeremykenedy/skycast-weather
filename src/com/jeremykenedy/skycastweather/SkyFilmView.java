package com.jeremykenedy.skycastweather;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import java.io.IOException;

final class SkyFilmView extends TextureView implements TextureView.SurfaceTextureListener {
  private Surface videoSurface;
  private MediaPlayer player;
  private boolean running, fallback;
  private float speed = .75f;
  private int position;

  SkyFilmView(Context context) {
    super(context);
    setSurfaceTextureListener(this);
  }

  void start(float playbackSpeed) {
    speed = playbackSpeed;
    running = true;
    prepare();
  }

  void stop() {
    running = false;
    releasePlayer();
  }

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
    videoSurface = new Surface(texture);
    fit(width, height);
    prepare();
  }

  @Override
  public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
    fit(width, height);
  }

  private void fit(int width, int height) {
    if (width <= 0 || height <= 0) return;
    float aspect = 16f / 9;
    float x = Math.max(1, height * aspect / width);
    float y = Math.max(1, width / (height * aspect));
    Matrix transform = new Matrix();
    transform.setScale(x, y, width / 2f, height / 2f);
    setTransform(transform);
  }

  @Override
  public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
    releasePlayer();
    if (videoSurface != null) videoSurface.release();
    videoSurface = null;
    return true;
  }

  @Override
  public void onSurfaceTextureUpdated(SurfaceTexture texture) {}

  private void prepare() {
    if (!running || player != null || videoSurface == null || !videoSurface.isValid()) return;
    MediaPlayer next = new MediaPlayer();
    player = next;
    next.setSurface(videoSurface);
    next.setVolume(0, 0);
    next.setLooping(true);
    next.setOnPreparedListener(
        ready -> {
          if (player != ready || !running) return;
          ready.setPlaybackParams(new PlaybackParams().setSpeed(speed));
          if (position > 0) {
            ready.setOnSeekCompleteListener(
                seeked -> {
                  if (player == seeked && running) seeked.start();
                });
            ready.seekTo(position);
          } else ready.start();
          Log.i("SkycastPlayback", "Started bundled " + (fallback ? "720p" : "1080p") + " film");
        });
    next.setOnErrorListener(
        (failed, what, extra) -> {
          if (player == failed) unavailable();
          return true;
        });
    try (AssetFileDescriptor asset =
        getContext().getAssets().openFd(fallback ? "clouds-720.mp4" : "clouds-1080.mp4")) {
      next.setDataSource(asset.getFileDescriptor(), asset.getStartOffset(), asset.getLength());
      next.prepareAsync();
    } catch (IOException unavailable) {
      unavailable();
    }
  }

  private void unavailable() {
    releasePlayer();
    if (!fallback && running) {
      fallback = true;
      position = 0;
      prepare();
    } else Log.w("SkycastPlayback", "Bundled film unavailable; forecast remains visible");
  }

  private void releasePlayer() {
    MediaPlayer current = player;
    player = null;
    if (current == null) return;
    try {
      position = current.getCurrentPosition();
    } catch (IllegalStateException unprepared) {
      // A surface can disappear while the decoder is still preparing.
    }
    current.setOnPreparedListener(null);
    current.setOnErrorListener(null);
    current.setOnSeekCompleteListener(null);
    current.release();
  }
}
