# 12 — Build and Runtime Verification

**Audited Commit:** `230bb5fb750812f7d1f19a3fc5586932f99e58ed`  
**Environment:** Windows 11 (`10.0.26300.0`), OpenJDK `17.0.17` (Temurin-17.0.17+10), `javac 17.0.17`

## 1. Compilation Verification
- **Command:** `.\scripts\compile.ps1`
- **Log:** [compile.log](file:///e:/Peer-Share/reports/java_lab_recon/logs/compile.log)
- **Result:** **PASS** (`COMPILE_EXIT_CODE=0`), zero compiler errors, zero compiler warnings.
- **Details:** Compiles 25 production `.java` files (`--release 17`) to `out\main`, copies `src\main\resources\*` (`peershare.css`) to `out\main`, and compiles 9 test `.java` files to `out\test` against `lib\*.jar`.

## 2. Automated Test Suite Execution
- **Command:** `.\scripts\test.ps1`
- **Logs:** [test_execution.log](file:///e:/Peer-Share/reports/java_lab_recon/logs/test_execution.log), [test_execution_2.log](file:///e:/Peer-Share/reports/java_lab_recon/logs/test_execution_2.log)
- **Result:** **FAIL (`EXIT_CODE=1`)** at `NetworkTransferTest.testStopClosesConnectedClient` (line 221):
  `java.net.SocketException: Connection reset` at `NioSocketImpl.implRead`.
- **Root Cause Analysis:** On Windows JDK 17, closing the server-side `Socket` in `PeerServer.stop()` while the idle client has an unread socket causes Windows Winsock to send a TCP `RST` (`SocketException: Connection reset`) rather than a graceful `FIN` (`-1` EOF). Because `AllTests.main()` aborts on the first uncaught exception, `TransferServiceTest` did not run under `AllTests`.
- **Per-Test Reflection Audit:** Using `VerifyRecon` ([recon_audit.log](file:///e:/Peer-Share/reports/java_lab_recon/logs/recon_audit.log)), all 37 individual test methods across all 7 test suites were invoked independently: **36 PASSED, 1 FAILED** (`NetworkTransferTest.testStopClosesConnectedClient`).

## 3. Isolated Runtime & Two-Instance Functional Verification
- **Harness:** `VerifyRecon.runFunctionalDemo()` in an isolated temp directory (`%TEMP%\peershare-r0-demo*`) using ports `15001` and `15002`.
- **Log:** [recon_audit.log](file:///e:/Peer-Share/reports/java_lab_recon/logs/recon_audit.log)
- **Observed Results:**
  - `AppConfig.load` created isolated `shared_files/`, `downloads/`, and `peershare.db` paths.
  - `DatabaseManager.initialize()` created all 3 tables.
  - `FileScanner.scanAndStore()` indexed 2 sample files (`alpha.txt`, `beta.bin`).
  - `PeerServer` bound `127.0.0.1:15001` and `127.0.0.1:15002` cleanly and stopped cleanly.
  - `FileTransferClient.listRemoteFiles()` returned 2 files.
  - `TransferService.download()` transferred both text (`57 B`) and binary (`65,536 B`) files with **100% SHA-256 match** (`alpha.txt`: `e5ab4cee...`, `beta.bin`: `4b640d85...`).
  - Duplicate download rejected (`Download already exists: alpha.txt`), unreachable peer handled (`ConnectException`), and SQLite persistence verified across `DatabaseManager` re-open (1 peer, 3 history rows: 2 `SUCCESS`, 1 `FAILED`).

## 4. GUI Launch Verification
- Launching `com.peershare.Launcher` in an isolated temp directory stayed running (`HAS_EXITED=False`), confirming JavaFX initialization without crash, but `MainWindowHandle=0` in this non-interactive background shell session, so interactive GUI window capture is **BLOCKED** in this shell environment.
