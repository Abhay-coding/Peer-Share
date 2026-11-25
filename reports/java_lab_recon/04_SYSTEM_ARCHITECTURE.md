# 04 — System Architecture Reconstruction

**Audited Commit:** `230bb5fb750812f7d1f19a3fc5586932f99e58ed`

## 1. Architectural Pattern
PeerShare implements a **Layered Desktop Architecture** combined with a **Symmetric Peer-to-Peer (Client-Server per Node)** networking model:
- **NOT MVC/MVVM/Microservices:** There are no FXML views, no separate ViewModel/Controller classes, and no distributed service registry. Instead, JavaFX UI classes (`MainView`, `*Tab`) directly hold `ObservableList` state and invoke service/DAO classes on background `javafx.concurrent.Task` threads.
- **Dependency Injection (Constructor-Based):** `MainApp` acts as the composition root, instantiating `AppConfig`, `DatabaseManager`, `PeerServer`, DAOs, services, and `MainView` and wiring them via constructor arguments.

## 2. Package & Layer Responsibilities (Verified 9 Packages, 25 Production Files)

| Layer | Package(s) | Classes / Records | Responsibility |
|---|---|---|---|
| **Bootstrap / Lifecycle** | `com.peershare` | `Launcher`, `MainApp` | JVM entry point (`Launcher`), JavaFX `init()`/`start()`/`stop()` lifecycle, composition root |
| **Configuration** | `com.peershare.config` | `AppConfig` | Loads `config.properties`, validates port/bindAddress, normalizes & isolates folder paths |
| **UI (Presentation)** | `com.peershare.ui` | `MainView`, `SharedFilesTab`, `PeersTab`, `DownloadTab`, `HistoryTab`, `ViewParts` | Sidebar navigation, 4 workspace tabs, `TableView` search/sort, background `Task` dispatch |
| **Service (Business Logic)** | `com.peershare.service` | `FileScanner`, `TransferService` (`+HistoryWriteException`) | Local folder scanning & DB sync; download orchestration, `.part` temp files, hard-link publication, history logging |
| **Networking** | `com.peershare.network` | `PeerServer`, `ClientHandler`, `FileTransferClient` (`+ListRejectedException`, `+SocketRequest`), `Protocol`, `SocketDeadline` | TCP server accept loop (`Semaphore` bounded), per-connection handler, short-lived TCP client, wire framing, deadlines |
| **Data Access (DAO)** | `com.peershare.dao` | `PeerDao`, `SharedFileDao`, `DownloadHistoryDao` | Parameterized JDBC CRUD, batch transactional replacement, manual ResultSet-to-record mapping |
| **Database** | `com.peershare.db` | `DatabaseManager` | SQLite connection factory (`DriverManager`) and DDL schema initialization |
| **Domain Models** | `com.peershare.model` | `Peer`, `SharedFile`, `DownloadHistory` | Immutable Java `record`s representing database/domain entities |
| **Utilities** | `com.peershare.util` | `AppLogger`, `InputValidator` | `java.util.logging` singleton factory; peer input & filename/path-traversal validation |

## 3. Dependency Direction & Circularity Check
- **Acyclic Dependency Graph:** Dependencies flow strictly downward:
  - `com.peershare` (`MainApp`) → `config`, `db`, `dao`, `service`, `network`, `ui`, `util`
  - `com.peershare.ui` → `config`, `dao`, `model`, `network`, `service`, `util`
  - `com.peershare.service` → `config`, `dao`, `model`, `network`, `util`
  - `com.peershare.network` → `config`, `model`, `service` (`ClientHandler` calls `FileScanner.scan()` and `FileScanner.isShareableName()`), `util`
  - `com.peershare.dao` → `db`, `model`
  - `com.peershare.db` → `config`
  - `com.peershare.model`, `com.peershare.util` → JDK only (leaf packages)
- **Zero Circular Dependencies:** `service` depends on `network` (`TransferService` → `FileTransferClient`), and `network` (`ClientHandler`) depends on `service` (`FileScanner`), so at the package level `service ↔ network` is a mutual package reference, though at the **class level** there is **no cycle** (`TransferService` → `FileTransferClient`, `ClientHandler` → `FileScanner`, `PeerServer` → `FileScanner`).

## 4. Application Lifecycle
1. **Startup (`Launcher.main` → `MainApp.main` → `MainApp.init`):**
   - `AppConfig.load(Path.of("."))` reads `config.properties`, creates `shared_files/` and `downloads/`.
   - `DatabaseManager.initialize()` creates `peershare.db` and executes `CREATE TABLE IF NOT EXISTS` for `peer`, `shared_file`, `download_history`.
   - `PeerServer.start()` binds `ServerSocket` on `bindAddress:serverPort` (backlog 50, `SO_REUSEADDR=true`) and starts daemon `PeerServer-AcceptThread`.
2. **UI Construction (`MainApp.start`):**
   - Instantiates DAOs, `FileScanner`, `FileTransferClient`, `TransferService`, and `MainView` (1040×680 `Scene`, min 760×510), calls `selectPage(0)` (Shared Files), and shows `primaryStage`.
3. **Shutdown (`MainApp.stop`):**
   - Calls `peerServer.stop()`, closing `ServerSocket`, closing all active client `Socket`s in `ConcurrentHashMap.newKeySet()`, and joining `acceptThread` (up to 5000 ms).
