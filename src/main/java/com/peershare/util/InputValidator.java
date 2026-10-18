package com.peershare.util;

public final class InputValidator {
    private InputValidator() {
    }

    public static void validatePeer(String name, String host, String portText) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Peer name is required.");
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Peer IP address is required.");
        }
        parsePort(portText);
    }

    public static int parsePort(String portText) {
        int port;
        try {
            port = Integer.parseInt(portText == null ? "" : portText.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Port must be a whole number.");
        }

        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535.");
        }
        return port;
    }

    public static boolean isSafeFileName(String fileName) {
        return fileName != null
                && !fileName.isBlank()
                && !fileName.contains("..")
                && !fileName.contains("/")
                && !fileName.contains("\\");
    }
}
