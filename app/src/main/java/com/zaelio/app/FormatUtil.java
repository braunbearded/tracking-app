package com.zaelio.app;

import java.util.Locale;

final class FormatUtil {
    private FormatUtil() {
    }

    static String formatMs(long millis) {
        long seconds = millis / 1000;
        return String.format(Locale.US, "%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60);
    }
}
