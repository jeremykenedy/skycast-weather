package com.jeremykenedy.skycastweather;

public final class SkyHistory {
  private int write, phase, samples;
  private float daylight = Float.NaN, cover = Float.NaN, rain = Float.NaN;

  public void reset() {
    write = 0;
    phase = 0;
    samples = 0;
    daylight = Float.NaN;
    cover = Float.NaN;
    rain = Float.NaN;
  }

  public float blend(float nextDaylight, float nextCover, float nextRain) {
    if (daylight != nextDaylight || cover != nextCover || rain != nextRain) {
      samples = 0;
      phase = 0;
      daylight = nextDaylight;
      cover = nextCover;
      rain = nextRain;
    }
    return samples / (samples + 1f);
  }

  public int writeIndex() {
    return write;
  }

  public int readIndex() {
    return 1 - write;
  }

  public int phase() {
    return phase;
  }

  public void advance() {
    write = 1 - write;
    phase = (phase + 1) & 15;
    samples = Math.min(7, samples + 1);
  }
}
