package com.peershare.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class AppLogger {
    private AppLogger() {
    }

    public static Logger create() {
        Logger logger = Logger.getLogger("com.peershare");
        logger.setUseParentHandlers(false);

        if (logger.getHandlers().length == 0) {
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.ALL);
            consoleHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(consoleHandler);
        }

        logger.setLevel(Level.ALL);
        return logger;
    }
}
