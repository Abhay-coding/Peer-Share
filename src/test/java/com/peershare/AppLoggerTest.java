package com.peershare;

import com.peershare.util.AppLogger;

import java.util.logging.Logger;

public final class AppLoggerTest {
    private AppLoggerTest() {
    }

    public static void run() {
        Logger logger = AppLogger.create();

        TestSupport.assertEquals("com.peershare", logger.getName(), "The application logger needs a stable name.");
        TestSupport.assertTrue(!logger.getUseParentHandlers(), "The application logger must avoid duplicate console messages.");
    }
}
