// Source: src\main\java\com\peershare\dao\SharedFileDao.java (lines 39-67)
// Commit: 230bb5fb750812f7d1f19a3fc5586932f99e58ed
// Concept: JDBC Manual Transaction & Batch Insert

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
