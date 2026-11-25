# 00 — Executive Summary

**Project:** PeerShare (`Zylus08/Peer-Share`)  
**Audited Commit:** `230bb5fb750812f7d1f19a3fc5586932f99e58ed` (`main`)  
**Audit Date:** 2026-11-25  

## 1. Purpose of Reconnaissance
This reconnaissance dossier establishes a complete, source-verified and runtime-verified technical evidence base for **PeerShare**, an undergraduate Java Programming Laboratory desktop application. All findings are grounded in direct inspection of the 25 production Java files, 1 CSS stylesheet, 9 test files, 4 bundled JARs, 6 build/run/test scripts, and isolated execution on OpenJDK 17.0.17 (Windows 11).

## 2. What PeerShare Is
PeerShare is a **symmetric peer-to-peer (client-server per node) local-network file-sharing desktop application** built in Java 17 with JavaFX 21.0.12, SQLite JDBC 3.54.1.0, and raw TCP sockets (`ServerSocket`/`Socket`). Every running instance simultaneously operates:
- A **TCP server** (`PeerServer` + `ClientHandler`) serving files from a sandboxed local `shared_files/` folder; and
- A **TCP client** (`FileTransferClient` + `TransferService`) browsing remote peers' file lists and downloading files into a local `downloads/` folder.

## 3. Key Verified Capabilities
- **Safe Local Configuration (`AppConfig`)**: Loads `config.properties`, restricts `server.bindAddress` to `127.0.0.1` (default) or `0.0.0.0`, validates ports `1..65535`, and enforces non-overlapping subfolder paths inside the project root (resolving symlinks).
- **Custom TCP Application Protocol (`Protocol`, `PeerServer`, `ClientHandler`, `FileTransferClient`)**: Line-delimited UTF-8 headers (`LIST`, `DOWNLOAD <file>`, `OK ...`, `ERROR ...`) paired with raw binary streaming, 8 KB buffering, SHA-256 checksum verification, 100 MB max file size, 500 max listed files, 10 concurrent client slots (`Semaphore`), and `SocketDeadline` timeouts/cancellation.
- **Atomic File Publication (`TransferService`)**: Downloads to unique `peershare-*.part` temporary files, verifies byte count and SHA-256 digest, publishes via `Files.createLink` (hard link) to prevent overwriting existing files, and cleans up `.part` files in `finally`.
- **SQLite Persistence (`DatabaseManager`, DAOs)**: Stores `peer`, `shared_file`, and `download_history` metadata using `PreparedStatement` and `try-with-resources`, including manual transaction management in `SharedFileDao.replaceAll`.
- **Responsive JavaFX GUI (`MainView`, 4 Tabs)**: Uses `javafx.concurrent.Task` on background threads, `FilteredList`/`SortedList` for live search and sorting, and custom CSS styling (`peershare.css`).

## 4. Build, Test, and Runtime Verdict
- **Compilation (`.\scripts\compile.ps1`)**: **PASS** (`javac --release 17`, zero errors, zero warnings).
- **Automated Test Suite (`.\scripts\test.ps1` / `AllTests`)**: **PARTIAL FAIL ON WINDOWS** — `NetworkTransferTest.testStopClosesConnectedClient()` fails on Windows JDK 17 with `java.net.SocketException: Connection reset` at line 221 (expecting `-1` EOF when server closes an idle accepted socket, whereas Windows TCP sends RST). Individual audit of all 37 test methods shows **36 PASS, 1 FAIL**.
- **Isolated Functional Demo**: **PASS** — Two-instance loopback file listing, binary & text file transfer, byte-for-byte SHA-256 verification, duplicate rejection, unreachable peer error handling, and SQLite persistence across database re-open all succeeded.
- **GUI Screenshot Capture**: Existing 4 genuine screenshots in `docs/screenshots/` were verified against the current UI code; live headless/background GUI window capture in this non-interactive shell returned `WINDOW_HANDLE=0`.

**Overall Readiness Verdict:** **REPORT READY WITH DOCUMENTED LIMITATIONS**
