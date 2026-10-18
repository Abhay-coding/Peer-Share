package com.peershare.db;

import com.peershare.config.AppConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {
    private final Path databasePath;

    public DatabaseManager(Path databasePath) {
        this.databasePath = databasePath;
    }

    public DatabaseManager(AppConfig config) {
        this(config.databasePath());
    }

    public Path databasePath() {
        return databasePath;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
    }

    public void initialize() throws SQLException {
        try {
            Path parent = databasePath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new SQLException("Failed to create parent directory for database: " + databasePath, e);
        }

        String createPeerTable = """
                CREATE TABLE IF NOT EXISTS peer (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    ip_address TEXT NOT NULL,
                    port INTEGER NOT NULL
                );
                """;

        String createSharedFileTable = """
                CREATE TABLE IF NOT EXISTS shared_file (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    file_name TEXT NOT NULL,
                    file_size INTEGER NOT NULL,
                    last_modified TEXT NOT NULL
                );
                """;

        String createDownloadHistoryTable = """
                CREATE TABLE IF NOT EXISTS download_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    file_name TEXT NOT NULL,
                    peer_name TEXT NOT NULL,
                    downloaded_at TEXT NOT NULL,
                    status TEXT NOT NULL
                );
                """;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createPeerTable);
            statement.execute(createSharedFileTable);
            statement.execute(createDownloadHistoryTable);
        }
    }
}
