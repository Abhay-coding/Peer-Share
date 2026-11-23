package com.peershare.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Properties;

public final class AppConfig {
    private final int serverPort;
    private final String bindAddress;
    private final Path sharedDirectory;
    private final Path downloadDirectory;
    private final Path databasePath;

    private AppConfig(int serverPort, String bindAddress, Path sharedDirectory, Path downloadDirectory, Path databasePath) {
        this.serverPort = serverPort;
        this.bindAddress = bindAddress;
        this.sharedDirectory = sharedDirectory;
        this.downloadDirectory = downloadDirectory;
        this.databasePath = databasePath;
    }

    public static AppConfig load(Path projectRoot) throws IOException {
        Path root = projectRoot.toRealPath();
        Properties properties = new Properties();

        try (InputStream input = Files.newInputStream(root.resolve("config.properties"))) {
            properties.load(input);
        }

        int port = Integer.parseInt(properties.getProperty("server.port"));
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("server.port must be between 1 and 65535.");
        }
        String bindAddress = properties.getProperty("server.bindAddress", "127.0.0.1").trim();
        if (!bindAddress.equals("127.0.0.1") && !bindAddress.equals("0.0.0.0")) {
            throw new IllegalArgumentException("server.bindAddress must be 127.0.0.1 or 0.0.0.0.");
        }

        Path sharedDirectory = resolveInsideRoot(root, properties.getProperty("shared.directory"));
        Path downloadDirectory = resolveInsideRoot(root, properties.getProperty("downloads.directory"));
        if (sharedDirectory.startsWith(downloadDirectory) || downloadDirectory.startsWith(sharedDirectory)) {
            throw new IllegalArgumentException("Shared files and downloads must use separate, non-overlapping folders.");
        }
        Files.createDirectories(sharedDirectory);
        Files.createDirectories(downloadDirectory);

        return new AppConfig(port, bindAddress, sharedDirectory, downloadDirectory, root.resolve("peershare.db"));
    }

    private static Path resolveInsideRoot(Path root, String configuredDirectory) throws IOException {
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            throw new IllegalArgumentException("Set both shared.directory and downloads.directory.");
        }
        Path directory = root.resolve(configuredDirectory).normalize();
        if (!directory.startsWith(root) || directory.equals(root)) {
            throw new IllegalArgumentException("Configured folders must be subfolders of the project folder.");
        }

        // Resolve existing links before creating any missing folders.
        Path existing = directory;
        while (!Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            existing = existing.getParent();
        }
        Path resolved = existing.toRealPath().resolve(existing.relativize(directory));
        if (!resolved.startsWith(root) || resolved.equals(root)) {
            throw new IllegalArgumentException("Configured folders must resolve to subfolders of the project folder.");
        }
        return resolved;
    }

    public int serverPort() {
        return serverPort;
    }

    public String bindAddress() {
        return bindAddress;
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
