package com.peershare.dao;

import com.peershare.db.DatabaseManager;
import com.peershare.model.SharedFile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class SharedFileDao {
    private final DatabaseManager databaseManager;

    public SharedFileDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public List<SharedFile> findAll() throws SQLException {
        String sql = "SELECT id, file_name, file_size, last_modified FROM shared_file ORDER BY id ASC";
        List<SharedFile> files = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                files.add(new SharedFile(
                        rs.getLong("id"),
                        rs.getString("file_name"),
                        rs.getLong("file_size"),
                        LocalDateTime.parse(rs.getString("last_modified"))
                ));
            }
        }
        return files;
    }

    public void replaceAll(List<SharedFile> files) throws SQLException {
        String deleteSql = "DELETE FROM shared_file";
        String insertSql = "INSERT INTO shared_file (file_name, file_size, last_modified) VALUES (?, ?, ?)";

        try (Connection conn = databaseManager.getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.executeUpdate();
                }
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    for (SharedFile file : files) {
                        insertStmt.setString(1, file.fileName());
                        insertStmt.setLong(2, file.fileSize());
                        insertStmt.setString(3, file.lastModified().toString());
                        insertStmt.addBatch();
                    }
                    insertStmt.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }
}
