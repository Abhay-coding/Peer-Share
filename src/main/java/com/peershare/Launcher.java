package com.peershare;

/**
 * Launcher serves as an un-extended main class entry point to bootstrap JavaFX
 * cleanly from the classpath across all JDK distributions.
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        MainApp.main(args);
    }
}
