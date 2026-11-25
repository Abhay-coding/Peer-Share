# 19 — Evidence Ledger

**Audited Commit:** `230bb5fb750812f7d1f19a3fc5586932f99e58ed`

| Claim ID | Claim | Classification | Evidence Reference |
|---|---|---|---|
| E-01 | Repository has 25 production `.java` files (2131 SLOC) and 9 test `.java` files (877 SLOC) | **SOURCE VERIFIED** | `metrics.json`, `VerifyRecon` reflection |
| E-02 | Target bytecode is Java 17 (`--release 17`); verified on OpenJDK `17.0.17` | **SOURCE & RUNTIME VERIFIED** | `scripts/compile.ps1`, `logs/compile.log` |
| E-03 | Bundled JavaFX is `21.0.12+2` and SQLite JDBC is `3.54.1.0` | **SOURCE VERIFIED** | `lib/javafx-base.jar` (`javafx.properties`), `lib/sqlite-jdbc.jar` (`MANIFEST.MF`) |
| E-04 | `.\scripts\compile.ps1` compiles cleanly with 0 errors | **RUNTIME VERIFIED** | `logs/compile.log` (`COMPILE_EXIT_CODE=0`) |
| E-05 | `.\scripts\test.ps1` fails on Windows at `NetworkTransferTest.testStopClosesConnectedClient` (`SocketException: Connection reset`) | **RUNTIME VERIFIED** | `logs/test_execution.log`, `logs/test_execution_2.log` |
| E-06 | 36 of 37 individual test methods pass when invoked independently | **RUNTIME VERIFIED** | `logs/recon_audit.log` (`AUDIT_SUMMARY: passed=36, failed=1`) |
| E-07 | Two-instance loopback file list, text/binary download, SHA-256 match, duplicate guard, and DB reopen persistence succeed | **RUNTIME VERIFIED** | `logs/recon_audit.log` |
| E-08 | `DOWNLOAD` protocol exchanges `OK <size> <sha256Hex>` and verifies SHA-256 | **SOURCE & RUNTIME VERIFIED** | `ClientHandler.java` L98-L99, `FileTransferClient.java` L151-L189 |
| E-09 | `TransferService` uses `Files.createLink` (hard link) to prevent destination overwrite | **SOURCE VERIFIED** | `TransferService.java` L103-L108 |
| E-10 | Existing 4 screenshots in `docs/screenshots/` match current UI code | **SOURCE VERIFIED** | Visual inspection vs. `MainView.java` & `*Tab.java` |
| E-11 | Live interactive GUI window capture in background agent shell | **BLOCKED** | `MainWindowHandle=0` in non-interactive shell |
| E-12 | TLS encryption, authentication, automatic peer discovery, and Java `enum`s | **NOT IMPLEMENTED** | Full source audit of `src/main/java` |
