package com.jeremykenedy.skycastweather;

import java.util.Random;

public final class SkyNoise {
  public static final int WIDTH = 272, HEIGHT = 136;

  private SkyNoise() {}

  public static byte[] atlas() {
    byte[] volume = new byte[32 * 32 * 32];
    new Random(0x57c45a71L).nextBytes(volume);
    byte[] result = new byte[WIDTH * HEIGHT * 2];
    for (int z = 0; z < 32; z++) {
      int tileX = (z % 8) * 34, tileY = (z / 8) * 34;
      for (int y = 0; y < 34; y++)
        for (int x = 0; x < 34; x++) {
          int vx = (x + 31) % 32, vy = (y + 31) % 32;
          int pixel = ((tileY + y) * WIDTH + tileX + x) * 2;
          result[pixel] = volume[(z * 32 + vy) * 32 + vx];
          result[pixel + 1] = volume[(((z + 1) % 32) * 32 + vy) * 32 + vx];
        }
    }
    return result;
  }
}
