package com.peershare.dao;

import com.peershare.db.DatabaseManager;
import com.peershare.model.Peer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class PeerDao {
    private final DatabaseManager databaseManager;

    public PeerDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Peer create(Peer peer) throws SQLException {
        String sql = "INSERT INTO peer (name, ip_address, port) VALUES (?, ?, ?)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, peer.name());
            stmt.setString(2, peer.ipAddress());
            stmt.setInt(3, peer.port());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    return new Peer(id, peer.name(), peer.ipAddress(), peer.port());
                } else {
                    throw new SQLException("Creating peer failed, no ID obtained.");
                }
            }
        }
    }

    public List<Peer> findAll() throws SQLException {
        String sql = "SELECT id, name, ip_address, port FROM peer ORDER BY id ASC";
        List<Peer> peers = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                peers.add(new Peer(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("ip_address"),
                        rs.getInt("port")
                ));
            }
        }
        return peers;
    }

    public void update(Peer peer) throws SQLException {
        String sql = "UPDATE peer SET name = ?, ip_address = ?, port = ? WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, peer.name());
            stmt.setString(2, peer.ipAddress());
            stmt.setInt(3, peer.port());
            stmt.setLong(4, peer.id());
            stmt.executeUpdate();
        }
    }

    public void deleteById(long id) throws SQLException {
        String sql = "DELETE FROM peer WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }
}
