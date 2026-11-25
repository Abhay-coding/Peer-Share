# 03 — Technology Stack and Dependency Analysis

## 1. Build System & Compilation Pipeline
- **Build Mechanism:** Custom script-based compilation pipeline (no Maven `pom.xml` or Gradle `build.gradle`).
- **Windows Scripts:** `scripts/compile.ps1`, `scripts/run.ps1`, `scripts/test.ps1`
- **POSIX Scripts:** `scripts/compile.sh`, `scripts/run.sh`, `scripts/test.sh`
- **Minimum Supported Java Version:** Java 17 (`--release 17` enforced in `compile.ps1` line 25/37 and `compile.sh` line 41/55; uses Java `record`s and text blocks `"""`).
- **Java Version Used for Verification:** OpenJDK `17.0.17` (Temurin-17.0.17+10).

## 2. Core JDK & External Dependencies

| Technology / Library | Exact Version | Source | Role in PeerShare | Files Using It |
|---|---|---|---|---|
| **Java SE / JDK** | `--release 17` (verified on `17.0.17`) | System JDK | Core runtime, records, text blocks, switch/pattern matching | All 25 production + 9 test `.java` files |
| **JavaFX (`javafx-base`, `javafx-controls`, `javafx-graphics`)** | `21.0.12+2` (from `javafx.properties` inside `lib/javafx-base.jar`) | Bundled `lib/javafx-*.jar` (Windows binaries) | Desktop GUI (`Application`, `Stage`, `Scene`, `Tab`, `TableView`, `Task`, `ObservableList`, CSS) | `MainApp`, `MainView`, `PeersTab`, `SharedFilesTab`, `DownloadTab`, `HistoryTab`, `ViewParts` |
| **SQLite JDBC (`org.xerial:sqlite-jdbc`)** | `3.54.1.0` (from `META-INF/MANIFEST.MF` & `pom.properties`) | Bundled `lib/sqlite-jdbc.jar` | Embedded relational database driver (`jdbc:sqlite:...`) | `DatabaseManager`, `PeerDao`, `SharedFileDao`, `DownloadHistoryDao` |
| **JDBC (`java.sql.*`)** | JDK 17 Core | JDK | `Connection`, `DriverManager`, `PreparedStatement`, `Statement`, `ResultSet`, `SQLException` | `DatabaseManager`, all 3 DAOs |
| **Java Networking (`java.net.*`)** | JDK 17 Core | JDK | TCP `ServerSocket`, `Socket`, `InetAddress`, `InetSocketAddress`, `SocketTimeoutException` | `PeerServer`, `ClientHandler`, `FileTransferClient`, `SocketDeadline` |
| **Java NIO & I/O (`java.nio.file.*`, `java.io.*`)** | JDK 17 Core | JDK | `Path`, `Files` (`list`, `createLink`, `createTempFile`, `isRegularFile`, `isSymbolicLink`, `toRealPath`), `InputStream`/`OutputStream` | `AppConfig`, `FileScanner`, `TransferService`, `ClientHandler`, `FileTransferClient`, `Protocol`, `ViewParts` |
| **Java Concurrency (`java.util.concurrent.*`)** | JDK 17 Core | JDK | `Semaphore`, `ConcurrentHashMap`, `ScheduledThreadPoolExecutor`, `AtomicBoolean`, `CancellationException`, `Callable` | `PeerServer`, `SocketDeadline`, `DownloadTab`, `PeersTab`, `TransferService` |
| **Java Security (`java.security.MessageDigest`)** | JDK 17 Core | JDK | SHA-256 digest computation and `HexFormat` encoding | `ClientHandler`, `FileTransferClient` |
| **Java Logging (`java.util.logging.*`)** | JDK 17 Core | JDK | `Logger`, `ConsoleHandler`, `SimpleFormatter` | `AppLogger`, `MainApp`, `PeerServer`, `ClientHandler`, `TransferService`, UI tabs |
| **JavaFX CSS** | JavaFX 21 | `src/main/resources/com/peershare/ui/peershare.css` | Custom theme colors, typography (`Segoe UI`), table/button/sidebar styling | Loaded by `MainView` |
| **Custom Test Runner** | N/A (No JUnit) | `src/test/java/com/peershare/` | Lightweight `AssertionError`-based test harness (`TestSupport` + `AllTests`) | All 9 test classes |

## 3. Platform-Specific Dependency Note
- `lib/javafx-*.jar` bundled in the repository contain **Windows native libraries** (`README.md` line 15). Running on macOS or Linux requires replacing the three `javafx-*.jar` files in `lib/` with platform-matching JavaFX 17+ JARs.
- Both `run.ps1` and `test.ps1` pass `--enable-native-access=ALL-UNNAMED` to `java` to allow SQLite JDBC / JavaFX native library loading without warnings on newer JDKs.
