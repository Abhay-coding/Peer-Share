package com.peershare.dao;

import com.peershare.db.DatabaseManager;
import com.peershare.model.DownloadHistory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class DownloadHistoryDao {
    private final DatabaseManager databaseManager;

    public DownloadHistoryDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public DownloadHistory create(DownloadHistory entry) throws SQLException {
        String sql = "INSERT INTO download_history (file_name, peer_name, downloaded_at, status) VALUES (?, ?, ?, ?)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, entry.fileName());
            stmt.setString(2, entry.peerName());
            stmt.setString(3, entry.downloadedAt().toString());
            stmt.setString(4, entry.status());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    return new DownloadHistory(id, entry.fileName(), entry.peerName(),
                            entry.downloadedAt(), entry.status());
                } else {
                    throw new SQLException("Creating download history failed, no ID obtained.");
                }
            }
        }
    }

    public List<DownloadHistory> findAll() throws SQLException {
        String sql = "SELECT id, file_name, peer_name, downloaded_at, status FROM download_history ORDER BY id ASC";
        List<DownloadHistory> entries = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                entries.add(new DownloadHistory(
                        rs.getLong("id"),
                        rs.getString("file_name"),
                        rs.getString("peer_name"),
                        LocalDateTime.parse(rs.getString("downloaded_at")),
                        rs.getString("status")
                ));
            }
        }
        return entries;
    }

    public void deleteById(long id) throws SQLException {
        String sql = "DELETE FROM download_history WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }
}
