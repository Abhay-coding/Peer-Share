# 21 — Self-Contained Report Authoring Handoff (PeerShare)

**Repository:** `https://github.com/Zylus08/Peer-Share.git`  
**Baseline Commit:** `230bb5fb750812f7d1f19a3fc5586932f99e58ed` (`main`)  
**R0.1 Scoped Test Fix:** `src/test/java/com/peershare/NetworkTransferTest.java` (lines 220–226: accept `SocketException` TCP RST alongside `-1` EOF on Windows server-close test)  
**Authoring Verdict:** **REPORT READY WITH DOCUMENTED LIMITATIONS**

This document is self-contained: another AI assistant or technical writer can author the complete undergraduate Java Programming Laboratory project report directly from this file without re-auditing the repository.

---

## 1. Verified Project Abstract (230 words)
> **PeerShare** is a cross-platform desktop file-sharing application developed in Java 17 that enables direct, peer-to-peer file exchange over local area networks without requiring a central server, user accounts, or internet connectivity. Conventional file-sharing methods such as cloud storage platforms introduce external server dependencies and internet requirements, while physical media are inconvenient for rapid classroom or laboratory collaboration. PeerShare addresses these limitations by embedding both a multithreaded TCP server and a TCP client within every running instance, allowing any node to simultaneously serve local files and download files from registered peers.
>
> The application is structured into modular layers encompassing configuration management, JDBC/SQLite persistence via the Data Access Object (DAO) pattern, NIO-based filesystem services, a custom text-and-binary TCP application protocol, and an event-driven JavaFX graphical user interface. To ensure transfer reliability and defensive operation, PeerShare enforces strict filename and path-traversal validation, bounds concurrent client connections and file sizes, verifies downloaded payloads using SHA-256 checksums, and publishes completed downloads atomically from temporary `.part` files using filesystem hard links to prevent overwriting existing files. All network and database operations execute on background worker threads via `javafx.concurrent.Task`, preserving UI responsiveness. Verified through automated unit and integration tests as well as two-instance loopback demonstrations, PeerShare demonstrates practical application of Java networking, concurrency, file I/O, database connectivity, and object-oriented design.

---

## 2. Problem Statement and Objectives
- **Problem Statement:** Students and small teams on a local network often rely on cloud storage (requiring internet, accounts, and external servers) or USB flash drives (inconvenient physical media) to share files. PeerShare provides a zero-account, offline-capable desktop tool for direct local-network file transfer.
- **Primary Objective:** Build a reliable Java desktop application for direct TCP file sharing between symmetric peer instances without a central server.
- **Secondary Objectives:**
  1. Sandbox shared and downloaded files to non-overlapping project subfolders (`shared_files/`, `downloads/`).
  2. Persist saved peers, shared-file metadata, and download history in an embedded SQLite database (`peershare.db`).
  3. Keep the JavaFX GUI responsive during network and database I/O using `javafx.concurrent.Task`.
  4. Enforce defensive validation (path traversal, symlinks, SHA-256 integrity, hard-link overwrite protection, timeouts).

---

## 3. Scope, Exclusions, and Requirements
- **Architecture Classification:** Symmetric Peer-to-Peer (client-server per node). Every running instance embeds both a TCP server (`PeerServer`) and a TCP client (`FileTransferClient`).
- **Exclusions:** No TLS encryption, no user authentication/authorization, no automatic LAN/mDNS peer discovery, no NAT traversal, no resumable transfers, no recursive directory sharing.
- **Functional Requirements:**
  - **FR1:** Load port (`1..65535`), bind address (`127.0.0.1` default or `0.0.0.0`), and folder paths from `config.properties`.
  - **FR2:** Scan and index direct regular non-symlink files in `shared_files/` (excluding `.gitkeep`).
  - **FR3:** Create, read, update, search, and delete saved peers in SQLite.
  - **FR4:** Retrieve, search, and sort a remote peer's shared file list (`LIST`).
  - **FR5:** Download a selected file (`DOWNLOAD <name>`) with SHA-256 verification, duplicate rejection, and cancellation.
  - **FR6:** Record and display `SUCCESS` (`Saved`) and `FAILED` (`Failed`) download history; allow deleting entries without deleting files.
- **Non-Functional Requirements:** Java 17+ (`--release 17`), bounded resources (`MAX_FILE_BYTES = 100 MiB` = `104,857,600` bytes, `MAX_LIST_FILES = 500`, `MAX_CLIENTS = 10`, `MAX_LINE_BYTES = 4096`), non-blocking UI.

---

## 4. Complete Technology Stack

| Component | Version / Detail | Source | Role |
|---|---|---|---|
| **Java SE / JDK** | `--release 17` (verified on OpenJDK `17.0.17`) | System JDK | Core language, `record`s, text blocks, NIO, `java.net`, `java.sql` |
| **JavaFX** | `21.0.12+2` (`javafx-base`, `controls`, `graphics`) | `lib/javafx-*.jar` (Windows natives) | Desktop GUI, `TableView`, `FilteredList`/`SortedList`, `Task`, CSS |
| **SQLite JDBC** | `org.xerial:sqlite-jdbc` `3.54.1.0` | `lib/sqlite-jdbc.jar` | Embedded relational DB driver (`jdbc:sqlite:...`) |
| **Build & Test** | Custom PowerShell (`scripts/*.ps1`) & POSIX (`scripts/*.sh`) | `scripts/` | Compilation (`javac --release 17`) & custom `AllTests` runner (no Maven/Gradle/JUnit) |

---

## 5. Exact Project Statistics (`metrics.json`)
- **Production:** `25` `.java` files, `9` packages, `22` top-level classes, `3` top-level `record`s, `3` nested classes/interfaces (`1` `@FunctionalInterface`), `9` anonymous classes, `0` enums, `37` compiled `.class` files, `135` declared methods, `44` constructors, `2,131` SLOC (`2,558` total lines, `290` blank, `137` comment).
- **Tests:** `9` `.java` files, `1` package (`com.peershare`), `9` classes, `52` declared methods, `877` SLOC (`1,028` total lines), `37` audited test methods.
- **Other:** `1` CSS file (`109` lines), `4` JARs, `6` scripts, `4` UI tabs, `3` SQLite tables, `2` protocol commands (`LIST`, `DOWNLOAD`), `60` lines in `README.md`, `739` lines in `EXPLANATION.md`.

---

## 6. Architecture & Module-Wise Implementation
PeerShare follows a **Layered Desktop Architecture** with constructor-based dependency injection rooted in `MainApp`:
1. `com.peershare` (`Launcher`, `MainApp`): JVM bootstrap + JavaFX `init()`/`start()`/`stop()` lifecycle.
2. `com.peershare.config` (`AppConfig`): Parses `config.properties`, validates `server.bindAddress` (`127.0.0.1` or `0.0.0.0`), resolves symlinks, prevents root/overlapping folders.
3. `com.peershare.db` (`DatabaseManager`): SQLite `DriverManager` factory and DDL initialization.
4. `com.peershare.model` (`Peer`, `SharedFile`, `DownloadHistory`): Immutable Java 17 `record`s.
5. `com.peershare.dao` (`PeerDao`, `SharedFileDao`, `DownloadHistoryDao`): Parameterized JDBC CRUD + transactional batch replacement.
6. `com.peershare.service` (`FileScanner`, `TransferService`, `HistoryWriteException`): Filesystem scanning, `.part` temp file orchestration, hard-link publication (`Files.createLink`), history logging.
7. `com.peershare.network` (`PeerServer`, `ClientHandler`, `FileTransferClient`, `ListRejectedException`, `SocketRequest`, `Protocol`, `SocketDeadline`): TCP accept loop (`Semaphore(10)`), request dispatch, SHA-256 verification, deadlines.
8. `com.peershare.ui` (`MainView`, `SharedFilesTab`, `PeersTab`, `DownloadTab`, `HistoryTab`, `ViewParts`, `peershare.css`): 4-tab sidebar UI, live search/sort, `javafx.concurrent.Task` workers.
9. `com.peershare.util` (`AppLogger`, `InputValidator`): `java.util.logging` setup and peer/filename validation.

---

## 7. Java Concept Evidence Matrix (Academic Mapping)
- **Explicitly Implemented:** Java 17 `record`s (`Peer.java` L3), constructor/method overloading (`FileTransferClient.java` L35-L50, `TransferService.java` L63-L72), `@FunctionalInterface` (`SocketRequest<T>` in `FileTransferClient.java` L208-L211), generic methods (`ViewParts.searchable<T>` L69-L92), custom checked exceptions (`HistoryWriteException` in `TransferService.java` L153-L165, `ListRejectedException` in `FileTransferClient.java` L214-L226), `addSuppressed` (`TransferService.java` L126), daemon threads (`PeerServer.java` L71, L105), `ScheduledThreadPoolExecutor` + `volatile` (`SocketDeadline.java` L14-L27), DAO pattern (`com.peershare.dao.*`), `final` utility classes with `private` constructors (`Protocol`, `InputValidator`, `AppLogger`, `ViewParts`, `Launcher`).
- **Framework/Library Demonstrated:** `Application` subclassing (`MainApp.java`), `Task<T>` (`DownloadTab.java`), `FilteredList`/`SortedList` (`ViewParts.java`), `Semaphore(10)` & `ConcurrentHashMap.newKeySet()` (`PeerServer.java`), `AtomicBoolean` (`DownloadTab.java`), `ServerSocket`/`Socket`, NIO `Files.createLink`/`toRealPath`, JDBC `PreparedStatement` & manual transactions (`SharedFileDao.java`), `MessageDigest` SHA-256 (`ClientHandler.java`, `FileTransferClient.java`).
- **Confirmed Not Present:** `enum` types, custom `abstract class` hierarchies, `ExecutorService` for client handling, FXML, Java Object Serialization.

---

## 8. Exact Communication Protocol (`Protocol.java`, `ClientHandler.java`, `FileTransferClient.java`)
- **Transport:** Short-lived TCP connections; UTF-8 `\n`-terminated header lines (`Protocol.readLine` reads byte-by-byte up to `MAX_LINE_BYTES = 4096` so `BufferedReader` does not consume binary payload bytes).
- **Constants:** `MAX_FILE_BYTES = 104,857,600` (100 MiB), `MAX_LIST_FILES = 500`, `MAX_CLIENTS = 10`, buffer = `8192` bytes, connect timeout = `3000 ms`, client read timeout = `15000 ms`, server client read timeout = `5000 ms`, overall `SocketDeadline` = `120,000 ms`.
- **Commands & Responses:**
  - `LIST\n` → `OK <count>\n` + `<count>` lines of `<fileName>|<fileSize>|<lastModified>\n`, or `ERROR Too many shared files\n`.
  - `DOWNLOAD <fileName>\n` → `OK <fileSize> <sha256Hex>\n` + exact `<fileSize>` raw bytes, or `ERROR <message>\n`.
  - Unknown command → `ERROR Unknown command\n`.
- **Integrity vs. Security:** SHA-256 verifies payload integrity against corruption/truncation/TOCTOU changes; **no encryption (no TLS) and no authentication** are implemented.

---

## 9. Source-Grounded Algorithms (7 Core Algorithms)
1. **Shared Directory Scanning (`FileScanner.scan`):** Auto-creates `sharedDirectory` if missing; opens `Files.list` in `try-with-resources`; filters `!isSymbolicLink && isRegularFile && isShareableName` (excludes `.gitkeep`); extracts size & `LocalDateTime`; sorts alphabetically ($O(N \log N)$ time, $O(N)$ space).
2. **Peer Registration (`InputValidator.validatePeer` + `PeerDao.create`):** Validates non-blank name/host and integer port `1..65535`; executes parameterized `INSERT` with `RETURN_GENERATED_KEYS` ($O(1)$).
3. **Remote File-List Retrieval (`FileTransferClient.listRemoteFiles`):** Opens socket with `SocketDeadline`; sends `LIST\n`; validates `OK <0..500>`; parses `<name>|<size>|<date>` rows ($O(M)$ time & space, $M \le 500$).
4. **File Serving (`ClientHandler.handleDownload`):** Validates `isShareableName` & `target.startsWith(normalizedBase)` & `!isSymbolicLink`; checks `size <= 100 MiB`; computes SHA-256; writes `OK <size> <sha256>\n`; streams 8 KB chunks; re-checks `size` and `lastModified` post-transfer (TOCTOU guard) ($O(S)$ time, $O(1)$ auxiliary space).
5. **File Download & Publication (`TransferService.download` + `FileTransferClient.download`):** Checks destination does not exist; creates unique `peershare-*.part` via `Files.createTempFile`; streams 8 KB chunks while computing SHA-256; verifies `in.read() == -1` and SHA-256 equality; publishes atomically via `Files.createLink(destination, tempFile)`; logs `SUCCESS` (or `FAILED` if unpublished); deletes `.part` in `finally` ($O(S)$ time, $O(1)$ space).
6. **Cancellation & Cleanup (`DownloadTab` + `SocketDeadline`):** `Cancel download` sets `AtomicBoolean`; `SocketDeadline` polls every `100 ms` and closes `Socket`, throwing `CancellationException("Download cancelled.")`; `.part` deleted in `finally`, `FAILED` recorded.
7. **History Recording (`TransferService.recordHistory`):** Inserts `DownloadHistory` row; if `published == true` and insert throws `SQLException`, throws `HistoryWriteException(destination, ex)` preserving the saved file.

---

## 10. Database Schema, Data Dictionary & Normalization
**DDL (`DatabaseManager.java` L42-L68):**
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
- **Relationships:** No SQL foreign keys, unique constraints, or check constraints.
- **Normalization (Explicit Functional Dependencies):**
  - `peer`: `id -> {name, ip_address, port}` (1NF, 2NF, 3NF).
  - `shared_file`: `id -> {file_name, file_size, last_modified}` (1NF, 2NF, 3NF).
  - `download_history`: `id -> {file_name, peer_name, downloaded_at, status}` (1NF, 2NF, 3NF; `peer_name` is an intentional historical `TEXT` snapshot so logs survive peer deletion).
- **Transactions:** `SharedFileDao.replaceAll` disables `autoCommit`, deletes all rows, batch-inserts new rows, and calls `commit()` (or `rollback()` on `SQLException`).

---

## 11. Verified Code Excerpts (12 Key Excerpts)
All 12 verbatim excerpts are stored in `reports/java_lab_recon/code_excerpts/`:
1. `01_Peer_Record.java` (`Peer.java` L1-L4) — Java 17 `record`
2. `02_AppConfig_PathContainment.java` (`AppConfig.java` L53-L72) — NIO path normalization & symlink containment
3. `03_SharedFileDao_Transaction.java` (`SharedFileDao.java` L39-L67) — JDBC manual transaction & batch insert
4. `04_Protocol_ReadLine.java` (`Protocol.java` L35-L66) — Byte-by-byte framing preserving binary stream
5. `05_PeerServer_SemaphoreAccept.java` (`PeerServer.java` L65-L114) — `ServerSocket` accept loop & `Semaphore(10)`
6. `06_ClientHandler_TOCTOU_Checksum.java` (`ClientHandler.java` L79-L130) — SHA-256 & TOCTOU re-check
7. `07_SocketDeadline_Timer.java` (`SocketDeadline.java` L13-L68) — `ScheduledThreadPoolExecutor` & `AutoCloseable`
8. `08_FileTransferClient_Streaming.java` (`FileTransferClient.java` L164-L211) — 8 KB streaming, SHA-256, `@FunctionalInterface`
9. `09_TransferService_HardLink.java` (`TransferService.java` L72-L146) — `.part` temp file, `Files.createLink`, `addSuppressed`
10. `10_DownloadTab_JavaFXTask.java` (`DownloadTab.java` L189-L245) — `javafx.concurrent.Task` & `AtomicBoolean` cancellation
11. `11_ViewParts_GenericsFilter.java` (`ViewParts.java` L69-L92) — Generic `FilteredList`/`SortedList` search
12. `12_InputValidator_Security.java` (`InputValidator.java` L31-L45) — Path-traversal & Windows reserved-name validation

---

## 12. UI Screens & Genuine Screenshots
- **UI Layout (`MainView.java`, `peershare.css`):** `1040×680` `BorderPane` with `#172541` sidebar, 4 `ToggleButton`s, and bottom port/bind status.
- **4 Verified Repository Screenshots (`reports/java_lab_recon/screenshots/`):**
  - Fig 1: `shared-files.png` — **Shared Files** tab with 4 sample files, search, `Refresh`, `Open shared folder`.
  - Fig 2: `peers.png` — **Peers** tab with `Edit peer` form, `Save changes`, `Browse files`, `Remove`, and saved peer table.
  - Fig 3: `download.png` — **Download** tab with peer selector, remote file table, `Download file`, `Open downloads`.
  - Fig 4: `history.png` — **History** tab with green `Saved` and red `Failed` status cells and `Remove entry`.
- **Interactive Capture Status:** Live GUI window capture in the non-interactive agent shell was **BLOCKED** (`MainWindowHandle=0`). Manual reproduction steps are in `14_SCREENSHOT_MANIFEST.md`.

---

## 13. Diagrams Inventory (`reports/java_lab_recon/diagrams/`)
16 validated Mermaid `.mmd` sources (`01_system_architecture.mmd` through `16_deployment_two_peers.mmd`). Local SVG/PNG rendering is **BLOCKED** (`mmdc`, `dot`, `plantuml` not installed).

---

## 14. Build, Automated Tests (5-Run Stability), and Functional Demo Results
- **Compilation (`.\scripts\compile.ps1`):** **PASS** (`exit 0`, zero warnings).
- **Automated Test Suite (`.\scripts\test.ps1` after scoped fix in `NetworkTransferTest.java` L221-L226):**
  - **5 Consecutive Runs (`r01_five_run_stability.log`):** Run 1 `PASS (0)`, Run 2 `PASS (0)`, Run 3 `PASS (0)`, Run 4 `PASS (0)`, Run 5 `PASS (0)` — `All tests passed.` (`37/37` test methods passing).
  - **Coverage:** Numerical coverage unavailable (no coverage tool).
- **Isolated Functional Demo (`recon_audit.log`):** Two-instance loopback (`15001` & `15002`) verified `FileScanner`, `PeerDao`, `LIST`, text & binary `DOWNLOAD` with 100% SHA-256 match, duplicate rejection, unreachable peer `ConnectException`, and SQLite persistence across DB re-open.

---

## 15. Academic Evidence Reconciliation (Docs vs. Code)
1. **Max File Size:** `Protocol.MAX_FILE_BYTES = 100L * 1024 * 1024` (`104,857,600` bytes = **100 MiB**), referred to colloquially as "100 MB" in `README.md`.
2. **Java vs. JavaFX Versions:** Code targets **Java 17** (`--release 17`); bundled Windows JavaFX JARs are **21.0.12+2**; `sqlite-jdbc` is **3.54.1.0** (earlier `EXPLANATION.md` notes didn't list exact versions).
3. **SHA-256 Integrity:** Confirmed implemented on **both** sender (`ClientHandler.checksum`) and receiver (`FileTransferClient.download`). **Does NOT provide encryption or authentication.**
4. **Atomic Publication:** Confirmed via `Files.createLink(destination, tempFile)` from unique `peershare-*.part`.
5. **Cancellation:** Confirmed via `AtomicBoolean` + `SocketDeadline` (`100 ms` poll) → `CancellationException`.
6. **History Statuses:** Database stores only `"SUCCESS"` or `"FAILED"` (cancelled transfers record `"FAILED"`).
7. **OOP Claims:** `record`s, overloading, framework inheritance, and interfaces are verified; `enum`s and custom `abstract class` hierarchies are **NOT** present.

---

## 16. Limitations, Future Scope, References & Manual Confirmations
- **Limitations:** No TLS, no authentication, manual peer entry, hard-link requirement on `downloads/`, single active download, no resume, flat `shared_files/`, Windows-only `lib/javafx-*.jar`.
- **Authoritative References:** Oracle Java 17 API Docs, OpenJFX 21 API Docs, SQLite Docs, Xerial `sqlite-jdbc` Docs, RFC 9293 (TCP).
- **Requires Manual Confirmation:** Student full name(s), roll number(s), faculty guide, institution, semester, and optional interactive desktop screenshots (active progress bar, validation error, two side-by-side windows).
