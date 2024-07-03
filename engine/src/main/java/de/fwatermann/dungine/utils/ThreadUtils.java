package de.fwatermann.dungine.utils;

import de.fwatermann.dungine.window.GameWindow;

public class ThreadUtils {

  public static void checkMainThread() {
    if (Thread.currentThread() != GameWindow.MAIN_THREAD) {
      throw new IllegalStateException(
          "This method can only be called from the main thread (aka. Render-/Input-Thread)");
    }
  }
}
