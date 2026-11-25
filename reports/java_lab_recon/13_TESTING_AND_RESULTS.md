# 13 — Automated Testing and Functional Verification

**Audited Commit:** `230bb5fb750812f7d1f19a3fc5586932f99e58ed`

## 1. Test Framework & Inventory
- **Framework:** Custom core-Java assertion harness (`TestSupport.java` + `AllTests.java`); no JUnit/TestNG.
- **Coverage Tooling:** None configured; numerical line/branch coverage is **unavailable**.
- **Test Files (9):** `AllTests`, `TestSupport`, `AppConfigTest`, `AppLoggerTest`, `ModelAndValidationTest`, `DatabaseDaoTest`, `FileScannerTest`, `NetworkTransferTest`, `TransferServiceTest`.

## 2. Automated Test Suite Summary (37 Audited Test Methods)

| Test Suite | Audited Units | Passed | Failed | Notes |
|---|---|---|---|---|
| `AppConfigTest` | 1 (`run()`, covering port, bindAddress, path containment, symlink test) | 1 | 0 | Symlink sub-check skips gracefully if Windows lacks symlink privilege (`catch IOException`). |
| `AppLoggerTest` | 1 (`run()`, logger name & `useParentHandlers=false`) | 1 | 0 | — |
| `ModelAndValidationTest` | 9 test methods | 9 | 0 | Covers records + `InputValidator` positive/negative cases. |
| `DatabaseDaoTest` | 1 (`run()`, covering Peer CRUD, SharedFile `replaceAll`, History CRUD) | 1 | 0 | Uses isolated temp SQLite DB. |
| `FileScannerTest` | 2 test methods | 2 | 0 | Covers direct regular files, `.gitkeep` exclusion, subdirectory exclusion, DB sync. |
| `NetworkTransferTest` | 15 test methods | 14 | **1** | `testStopClosesConnectedClient` fails on Windows (`SocketException: Connection reset` vs `-1`). |
| `TransferServiceTest` | 8 test methods | 8 | 0 | Verified via `VerifyRecon` isolated invocation (skipped by `AllTests` due to earlier failure). |
| **Total** | **37** | **36** | **1** | — |

## 3. Functional Demonstration Matrix

| Test ID | Feature | Preconditions | Test Steps | Expected Result | Actual Result | Status | Evidence |
|---|---|---|---|---|---|---|---|
| FD-01 | App Config & DB Init | Isolated temp dir + `config.properties` | `AppConfig.load` + `DatabaseManager.initialize` | Folders & 3 SQLite tables created | Created `shared_files`, `downloads`, `peershare.db` | **PASS** | `recon_audit.log` |
| FD-02 | Shared Folder Scan | `alpha.txt`, `beta.bin` in `shared_files` | `FileScanner.scanAndStore` | 2 files indexed in `shared_file` | Returned 2 files | **PASS** | `recon_audit.log` |
| FD-03 | Two-Instance Startup | Ports `15001`, `15002` free | `server1.start()`, `server2.start()` | Both bind `127.0.0.1` | Bound `15001`, `15002` | **PASS** | `recon_audit.log` |
| FD-04 | Peer Registration | `PeerDao` initialized | `peerDao1.create(DemoPeer2)` | Assigned `id=1` | `id=1` persisted | **PASS** | `recon_audit.log` |
| FD-05 | Remote File List | Peer 2 serving 2 files | `client1.listRemoteFiles(peer2)` | Returns 2 `SharedFile` items | Returned 2 items | **PASS** | `recon_audit.log` |
| FD-06 | File Download + SHA-256 | Peer 2 serving `alpha.txt`, `beta.bin` | `transfer1.download(...)` for both | Files saved; SHA-256 matches source | `match=true` for both | **PASS** | `recon_audit.log` |
| FD-07 | Duplicate Rejection | `alpha.txt` exists in `downloads` | `transfer1.download(peer2, "alpha.txt")` | Throws `IOException`; existing kept | `Download already exists: alpha.txt` | **PASS** | `recon_audit.log` |
| FD-08 | Unreachable Peer | Port `15999` closed | `client1.listRemoteFiles(offline)` | Throws `ConnectException` | `ConnectException: Connection refused` | **PASS** | `recon_audit.log` |
| FD-09 | Restart Persistence | New `DatabaseManager` on same DB | `PeerDao.findAll()`, `DownloadHistoryDao.findAll()` | 1 peer, 3 history rows (`2 SUCCESS, 1 FAILED`) | Exact match | **PASS** | `recon_audit.log` |
| FD-10 | Download Cancellation | Active transfer + `AtomicBoolean` | `TransferServiceTest.testCancellationClosesSocketAndCleansUp` | Throws `CancellationException`, `.part` deleted, `FAILED` logged | Passed in `VerifyRecon` | **PASS** | `recon_audit.log` |
