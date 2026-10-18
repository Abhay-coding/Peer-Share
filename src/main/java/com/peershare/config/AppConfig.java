package com.peershare.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class AppConfig {
    private final int serverPort;
    private final Path sharedDirectory;
    private final Path downloadDirectory;
    private final Path databasePath;

    private AppConfig(int serverPort, Path sharedDirectory, Path downloadDirectory, Path databasePath) {
        this.serverPort = serverPort;
        this.sharedDirectory = sharedDirectory;
        this.downloadDirectory = downloadDirectory;
        this.databasePath = databasePath;
    }

    public static AppConfig load(Path projectRoot) throws IOException {
        Path root = projectRoot.toAbsolutePath().normalize();
        Properties properties = new Properties();

        try (InputStream input = Files.newInputStream(root.resolve("config.properties"))) {
            properties.load(input);
        }

        int port = Integer.parseInt(properties.getProperty("server.port"));
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("server.port must be between 1 and 65535.");
        }

        Path sharedDirectory = resolveInsideRoot(root, properties.getProperty("shared.directory"));
        Path downloadDirectory = resolveInsideRoot(root, properties.getProperty("downloads.directory"));
        Files.createDirectories(sharedDirectory);
        Files.createDirectories(downloadDirectory);

        return new AppConfig(port, sharedDirectory, downloadDirectory, root.resolve("peershare.db"));
    }

    private static Path resolveInsideRoot(Path root, String configuredDirectory) {
        Path directory = root.resolve(configuredDirectory).normalize();
        if (!directory.startsWith(root)) {
            throw new IllegalArgumentException("Configured folders must stay inside the project folder.");
        }
        return directory;
    }

    public int serverPort() {
        return serverPort;
    }

    public Path sharedDirectory() {
        return sharedDirectory;
    }

    public Path downloadDirectory() {
        return downloadDirectory;
    }

    public Path databasePath() {
        return databasePath;
    }
}
