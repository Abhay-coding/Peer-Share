# PeerShare SQLite Data Dictionary

**Source:** [DatabaseManager.java](file:///e:/Peer-Share/src/main/java/com/peershare/db/DatabaseManager.java#L42-L68)  
**Commit SHA:** `230bb5fb750812f7d1f19a3fc5586932f99e58ed`

## Table 1: `peer`
Stores manually registered remote peers for browsing and downloading files.

| Column | SQL Type | Constraints | Nullable | Default | Java Field Mapping |
|---|---|---|---|---|---|
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | NO (PK) | Auto-increment | `Peer.id()` (`long`) |
| `name` | `TEXT` | `NOT NULL` | NO | None | `Peer.name()` (`String`) |
| `ip_address` | `TEXT` | `NOT NULL` | NO | None | `Peer.ipAddress()` (`String`) |
| `port` | `INTEGER` | `NOT NULL` | NO | None | `Peer.port()` (`int`) |

## Table 2: `shared_file`
Stores metadata cache of local files scanned from `shared_files/`.

| Column | SQL Type | Constraints | Nullable | Default | Java Field Mapping |
|---|---|---|---|---|---|
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | NO (PK) | Auto-increment | `SharedFile.id()` (`long`) |
| `file_name` | `TEXT` | `NOT NULL` | NO | None | `SharedFile.fileName()` (`String`) |
| `file_size` | `INTEGER` | `NOT NULL` | NO | None | `SharedFile.fileSize()` (`long`) |
| `last_modified` | `TEXT` | `NOT NULL` | NO | None | `SharedFile.lastModified()` (`LocalDateTime` via ISO-8601 string) |

## Table 3: `download_history`
Stores audit records of completed (`SUCCESS`) and failed/cancelled (`FAILED`) download attempts.

| Column | SQL Type | Constraints | Nullable | Default | Java Field Mapping |
|---|---|---|---|---|---|
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | NO (PK) | Auto-increment | `DownloadHistory.id()` (`long`) |
| `file_name` | `TEXT` | `NOT NULL` | NO | None | `DownloadHistory.fileName()` (`String`) |
| `peer_name` | `TEXT` | `NOT NULL` | NO | None | `DownloadHistory.peerName()` (`String`) |
| `downloaded_at` | `TEXT` | `NOT NULL` | NO | None | `DownloadHistory.downloadedAt()` (`LocalDateTime` via ISO-8601 string) |
| `status` | `TEXT` | `NOT NULL` | NO | None | `DownloadHistory.status()` (`String`: `"SUCCESS"` or `"FAILED"`) |
