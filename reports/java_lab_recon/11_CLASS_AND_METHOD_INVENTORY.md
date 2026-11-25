# 11 — Class-Level and Method-Level Inventory

**Audited Commit:** `230bb5fb750812f7d1f19a3fc5586932f99e58ed`  
**Totals (via Reflection on `out/main`):** 25 top-level types (22 classes, 3 records), 3 nested classes/interfaces, 9 anonymous classes, 135 declared non-synthetic methods, 44 constructors.

## 1. Production Class Inventory

| FQCN | Superclass / Interfaces | Key Methods | Dependencies |
|---|---|---|---|
| `com.peershare.Launcher` | `Object` | `main(String[])` | `MainApp` |
| `com.peershare.MainApp` | `javafx.application.Application` | `init()`, `start(Stage)`, `stop()`, `main(String[])` | `AppConfig`, `DatabaseManager`, `PeerServer`, DAOs, Services, `MainView`, `AppLogger` |
| `com.peershare.config.AppConfig` | `Object` | `load(Path)`, `resolveInsideRoot(Path, String)`, `serverPort()`, `bindAddress()`, `sharedDirectory()`, `downloadDirectory()`, `databasePath()` | JDK NIO/Properties |
| `com.peershare.db.DatabaseManager` | `Object` | `DatabaseManager(Path)`, `DatabaseManager(AppConfig)`, `getConnection()`, `initialize()`, `databasePath()` | `AppConfig`, `java.sql.*` |
| `com.peershare.model.Peer` | `java.lang.Record` | `(long id, String name, String ipAddress, int port)` | None |
| `com.peershare.model.SharedFile` | `java.lang.Record` | `(long id, String fileName, long fileSize, LocalDateTime lastModified)` | `LocalDateTime` |
| `com.peershare.model.DownloadHistory` | `java.lang.Record` | `(long id, String fileName, String peerName, LocalDateTime downloadedAt, String status)` | `LocalDateTime` |
| `com.peershare.dao.PeerDao` | `Object` | `create(Peer)`, `findAll()`, `update(Peer)`, `deleteById(long)` | `DatabaseManager`, `Peer` |
| `com.peershare.dao.SharedFileDao` | `Object` | `findAll()`, `replaceAll(List<SharedFile>)` | `DatabaseManager`, `SharedFile` |
| `com.peershare.dao.DownloadHistoryDao` | `Object` | `create(DownloadHistory)`, `findAll()`, `deleteById(long)` | `DatabaseManager`, `DownloadHistory` |
| `com.peershare.network.Protocol` | `Object` | `readLine(InputStream)`, `writeLine(OutputStream, String)` | JDK IO/Charset |
| `com.peershare.network.SocketDeadline` | `Object` / `AutoCloseable` | `SocketDeadline(Socket, int, BooleanSupplier)`, `failure(IOException)`, `close()` | `ScheduledThreadPoolExecutor`, `Socket` |
| `com.peershare.network.PeerServer` | `Object` | `start()`, `listenForClients(ServerSocket)`, `port()`, `bindAddress()`, `stop()` | `AppConfig`, `FileScanner`, `ClientHandler`, `AppLogger` |
| `com.peershare.network.ClientHandler` | `Object` / `Runnable` | `run()`, `handleList(OutputStream)`, `handleDownload(OutputStream, String)`, `checksum(Path)` | `Protocol`, `SocketDeadline`, `FileScanner`, `AppLogger` |
| `com.peershare.network.FileTransferClient` | `Object` | `listRemoteFiles(Peer)`, `download(...)` (3 overloads), `request(...)`, `parseCount()`, `parseSize()`, `sha256()` | `Protocol`, `SocketDeadline`, `InputValidator`, `Peer`, `SharedFile` |
| `com.peershare.network.FileTransferClient$ListRejectedException` | `IOException` | `fileLimitExceeded()` | `Protocol` |
| `com.peershare.network.FileTransferClient$SocketRequest<T>` | `@FunctionalInterface` | `run(InputStream, OutputStream)` | `IOException` |
| `com.peershare.service.FileScanner` | `Object` | `isShareableName(String)`, `scan()`, `scanAndStore(SharedFileDao)`, `sharedDirectory()` | `AppConfig`, `SharedFileDao`, `SharedFile`, `InputValidator` |
| `com.peershare.service.TransferService` | `Object` | `download(...)` (3 overloads), `recordHistory(...)`, `downloadsDirectory()` | `AppConfig`, `DownloadHistoryDao`, `FileTransferClient`, `InputValidator`, `AppLogger` |
| `com.peershare.service.TransferService$HistoryWriteException` | `IOException` | `savedFile()` | `Path`, `SQLException` |
| `com.peershare.ui.MainView` | `BorderPane` | `MainView(...)`, `addPage(...)`, `selectPage(int)`, `browsePeer(...)`, `icon(...)` | All 4 tabs, `ViewParts`, `peershare.css` |
| `com.peershare.ui.SharedFilesTab` | `Tab` | `setUpFileTable(TableView)`, `refreshFiles()` | `FileScanner`, `SharedFileDao`, `ViewParts`, `AppLogger` |
| `com.peershare.ui.PeersTab` | `Tab` | `setUpTable()`, `updateActions()`, `savePeer()`, `removePeer()`, `refreshPeers()`, `runChange(...)`, `clearForm()` | `PeerDao`, `InputValidator`, `ViewParts`, `AppLogger` |
| `com.peershare.ui.DownloadTab` | `Tab` | `refreshPeerSelector()`, `browse(Peer)`, `listFiles(Peer)`, `downloadFile()`, `runTask(...)`, `fileListFailure(...)` | `PeerDao`, `FileTransferClient`, `TransferService`, `Protocol`, `ViewParts` |
| `com.peershare.ui.HistoryTab` | `Tab` | `setUpTable()`, `displayedStatus(String)`, `refreshHistory()`, `removeEntry()`, `updateHistory(...)` | `DownloadHistoryDao`, `ViewParts`, `AppLogger` |
| `com.peershare.ui.ViewParts` | `Object` | `searchable(...)`, `folderButton(...)`, `confirm(...)`, `label(...)`, `page(...)`, `row(...)`, `bytes(long)`, `date(LocalDateTime)` | `AppLogger`, JavaFX Controls, `java.awt.Desktop` |
| `com.peershare.util.AppLogger` | `Object` | `create()` | `java.util.logging.*` |
| `com.peershare.util.InputValidator` | `Object` | `validatePeer(...)`, `parsePort(String)`, `isSafeFileName(String)` | JDK String/Regex |

## 2. Academically Significant Code Excerpts
Extracted into `reports/java_lab_recon/code_excerpts/`:
1. `01_Peer_Record.java` — `Peer.java` L1-L4 (Java 17 `record`).
2. `02_AppConfig_PathContainment.java` — `AppConfig.java` L53-L72 (NIO path normalization & symlink containment).
3. `03_SharedFileDao_Transaction.java` — `SharedFileDao.java` L39-L67 (JDBC manual transaction & batch insert).
4. `04_Protocol_ReadLine.java` — `Protocol.java` L35-L66 (Byte-by-byte framing to preserve binary stream).
5. `05_PeerServer_SemaphoreAccept.java` — `PeerServer.java` L65-L114 (`ServerSocket` + `Semaphore` + daemon threads).
6. `06_ClientHandler_TOCTOU_Checksum.java` — `ClientHandler.java` L79-L130 (SHA-256 + TOCTOU re-check).
7. `07_SocketDeadline_Timer.java` — `SocketDeadline.java` L13-L68 (`ScheduledThreadPoolExecutor` + `AutoCloseable`).
8. `08_FileTransferClient_Streaming.java` — `FileTransferClient.java` L164-L211 (8 KB streaming + SHA-256 + `@FunctionalInterface`).
9. `09_TransferService_HardLink.java` — `TransferService.java` L72-L146 (`.part` temp file + `Files.createLink` + `addSuppressed`).
10. `10_DownloadTab_JavaFXTask.java` — `DownloadTab.java` L189-L245 (`javafx.concurrent.Task` + `AtomicBoolean` cancellation).
11. `11_ViewParts_GenericsFilter.java` — `ViewParts.java` L69-L92 (Generic `FilteredList`/`SortedList` table search).
12. `12_InputValidator_Security.java` — `InputValidator.java` L31-L45 (Path-traversal & Windows reserved-name validation).
