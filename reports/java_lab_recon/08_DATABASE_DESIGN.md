# 08 — Database Design and Persistence

**Audited Commit:** `230bb5fb750812f7d1f19a3fc5586932f99e58ed`  
**Engine:** SQLite (embedded file `peershare.db`) via `org.xerial:sqlite-jdbc` `3.54.1.0`

## 1. Extracted SQL DDL (`DatabaseManager.java` L42-L68)
```sql
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
```

## 2. Schema Observations & Normalization Assessment
- **Relationships:** There are **no SQL `FOREIGN KEY` constraints**, **no `UNIQUE` constraints**, **no `CHECK` constraints**, and **no secondary `INDEX`es** in the schema.
- **Normalization Analysis:**
  - All three tables are in **1NF** (atomic scalar columns, `id` primary key).
  - All non-key attributes depend solely on the single-column surrogate key `id`, so there are **no partial dependencies (2NF satisfied)**.
  - In `peer` and `shared_file`, non-key attributes have no transitive dependencies among themselves (**3NF satisfied**).
  - In `download_history`, `peer_name` is stored as a denormalized `TEXT` snapshot rather than a `peer_id` foreign key. Because a user can delete or rename a `peer` row later without invalidating historical logs, `peer_name` is an intentional historical snapshot; within `download_history` itself, `id -> {file_name, peer_name, downloaded_at, status}` has no transitive dependency (**3NF satisfied**), though referential integrity to `peer` is not enforced at the DB level.
- **Timestamp Representation:** SQLite stores `LocalDateTime` as ISO-8601 `TEXT` (`toString()` / `LocalDateTime.parse()`).

## 3. DAO-to-Table Mapping & CRUD Operations

| DAO Class | Table | Methods | SQL Operations | Transaction Handling |
|---|---|---|---|---|
| `PeerDao` | `peer` | `create`, `findAll`, `update`, `deleteById` | `INSERT` (`RETURN_GENERATED_KEYS`), `SELECT ... ORDER BY id ASC`, `UPDATE ... WHERE id = ?`, `DELETE ... WHERE id = ?` | Auto-commit per statement |
| `SharedFileDao` | `shared_file` | `findAll`, `replaceAll` | `SELECT ... ORDER BY id ASC`, `DELETE FROM shared_file` + `INSERT` batch | **Explicit transaction** (`setAutoCommit(false)`, `addBatch`, `executeBatch`, `commit`, `rollback` on `SQLException`) |
| `DownloadHistoryDao` | `download_history` | `create`, `findAll`, `deleteById` | `INSERT` (`RETURN_GENERATED_KEYS`), `SELECT ... ORDER BY id ASC`, `DELETE ... WHERE id = ?` | Auto-commit per statement |

## 4. Persistent vs. In-Memory State
- **Persisted in SQLite (`peershare.db`):** Saved peers (`peer`), local shared-file metadata snapshot (`shared_file`), and transfer audit log (`download_history`).
- **In-Memory Only:** Remote peer file lists (`DownloadTab.files`), active `ObservableList` / `FilteredList` / `SortedList` UI views, `PeerServer.clients` active socket set, and `AppConfig` runtime configuration (loaded from `config.properties`).

## 5. Sample Records (From Isolated Demonstration DB)
Observed during `VerifyRecon` isolated execution (`reports/java_lab_recon/logs/recon_audit.log`):
- `peer`: `(id=1, name="DemoPeer2", ip_address="127.0.0.1", port=15002)`
- `download_history`:
  - `(id=1, file_name="alpha.txt", peer_name="DemoPeer2", downloaded_at="2026-11-25T14:37:59.044052800", status="SUCCESS")`
  - `(id=2, file_name="beta.bin", peer_name="DemoPeer2", downloaded_at="2026-11-25T14:37:59.076440600", status="SUCCESS")`
  - `(id=3, file_name="alpha.txt", peer_name="DemoPeer2", downloaded_at="2026-11-25T14:37:59.077279100", status="FAILED")`
