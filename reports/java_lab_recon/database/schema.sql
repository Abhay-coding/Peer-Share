-- Extracted verbatim from src/main/java/com/peershare/db/DatabaseManager.java (lines 42-68)
-- Audited Commit: 230bb5fb750812f7d1f19a3fc5586932f99e58ed

CREATE TABLE IF NOT EXISTS peer (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    ip_address TEXT NOT NULL,
    port INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS shared_file (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_name TEXT NOT NULL,
    file_size INTEGER NOT NULL,
    last_modified TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS download_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_name TEXT NOT NULL,
    peer_name TEXT NOT NULL,
    downloaded_at TEXT NOT NULL,
    status TEXT NOT NULL
);
