# 17 — Viva Voce Preparation (65 Questions & Live Demo Script)

All answers below are grounded in the actual PeerShare source code (commit `230bb5fb750812f7d1f19a3fc5586932f99e58ed`).

## A. Project Fundamentals (Q1–Q6)
1. **What is PeerShare?** A Java 17 + JavaFX desktop application for direct local-network file sharing without a central server or user accounts.
2. **Is PeerShare truly peer-to-peer or client-server?** Symmetric peer-to-peer (client-server per node): every instance runs both `PeerServer` (server) and `FileTransferClient` (client), connecting directly over TCP.
3. **Why did you choose a local-network tool over cloud storage?** Avoids internet dependency, accounts, and external server round-trips for classroom/lab file sharing.
4. **What are the four main screens?** Shared Files (`SharedFilesTab`), Peers (`PeersTab`), Download (`DownloadTab`), and History (`HistoryTab`).
5. **How are configuration settings stored?** In `config.properties` (`server.port`, `server.bindAddress`, `shared.directory`, `downloads.directory`), loaded by `AppConfig.load()`.
6. **What happens if `shared.directory` and `downloads.directory` overlap?** `AppConfig.load()` rejects the configuration with `IllegalArgumentException` to prevent serving partial downloads or overwriting shared files.

## B. Java & OOP (Q7–Q13)
7. **Why did you use Java `record`s for `Peer`, `SharedFile`, and `DownloadHistory`?** Records provide immutable fields, canonical constructors, `equals`, `hashCode`, `toString`, and accessors with minimal boilerplate.
8. **Where is encapsulation demonstrated?** Every class keeps fields `private final` (e.g., `AppConfig`, `PeerServer`, `DownloadTab`) and exposes controlled methods/accessors.
9. **Where is inheritance demonstrated?** `MainApp extends Application`, `MainView extends BorderPane`, `*Tab extends Tab`, `ListRejectedException extends IOException`, `HistoryWriteException extends IOException`.
10. **Is runtime polymorphism explicitly implemented in custom classes?** Primarily via JDK/JavaFX interfaces (`Runnable` in `ClientHandler`, `AutoCloseable` in `SocketDeadline`, `SocketRequest<T>` functional interface, `BooleanSupplier` for cancellation) rather than a custom abstract class hierarchy.
11. **Where is compile-time polymorphism (overloading) used?** `FileTransferClient` has 3 constructors and 3 `download(...)` overloads; `TransferService` has 3 constructors and 3 `download(...)` overloads; `DatabaseManager` has 2 constructors.
12. **Why are `Protocol`, `InputValidator`, `AppLogger`, `ViewParts`, and `Launcher` declared `final` with `private` constructors?** They are stateless utility/entry classes that should never be instantiated or subclassed.
13. **What is `SocketRequest<T>`?** A `@FunctionalInterface` in `FileTransferClient` that allows `IOException` to be thrown from a lambda passed to `request(...)`.

## C. Collections & Generics (Q14–Q18)
14. **Which collections are used in PeerShare?** `ArrayList`, `List.of()`, `FXCollections.observableArrayList()`, `FilteredList`, `SortedList`, and `ConcurrentHashMap.newKeySet()`.
15. **How is live search and sorting implemented in tables?** `ViewParts.searchable(...)` wraps an `ObservableList<T>` in a `FilteredList<T>` (bound to search `TextField`) and then a `SortedList<T>` (bound to `table.comparatorProperty()`).
16. **Where are Java Generics authored?** `ViewParts.searchable<T>(...)`, `DownloadTab.runTask<T>(...)`, `PeersTab.runChange<T>(...)`, and `FileTransferClient.request<T>(...)`.
17. **Where is the Stream API used?** `FileScanner.scan()` (`stream.toList()`), `ClientHandler.handleList()` (`.stream().filter(...).toList()`), and `DownloadTab.refreshPeerSelector()` (`.stream().filter(...).findFirst()`).
18. **How are `SharedFile` lists sorted by default?** `FileScanner.scan()` sorts with `files.sort(Comparator.comparing(SharedFile::fileName))`.

## D. Exception Handling (Q19–Q24)
19. **What custom exceptions exist?** `FileTransferClient.ListRejectedException` (carries `fileLimitExceeded` boolean) and `TransferService.HistoryWriteException` (carries `savedFile` `Path`).
20. **Why does `TransferService` use `e.addSuppressed(historyError)`?** If a download fails and recording the `FAILED` history row also throws `SQLException`, `addSuppressed` preserves the primary transfer exception instead of masking it.
21. **How are resources guaranteed to be closed?** `try-with-resources` is used for JDBC (`Connection`, `PreparedStatement`, `ResultSet`), `Files.list`, `Socket`, `SocketDeadline`, and file streams.
22. **Why does `SocketDeadline` throw `CancellationException` inside `failure(IOException)`?** When the user cancels, closing the socket causes `in.read()` to throw `IOException`; `SocketDeadline.failure` translates that into `CancellationException` so the UI shows `"Download cancelled."` instead of a network error.
23. **How does `DownloadTab` differentiate user errors?** `fileListFailure()` inspects `ListRejectedException`, `SocketTimeoutException`, `ConnectException`/`NoRouteToHostException`/`UnknownHostException` to show plain-English messages.
24. **What happens if `AppConfig.load()` encounters an invalid port?** `Integer.parseInt` throws `NumberFormatException` or range check throws `IllegalArgumentException`.

## E. Multithreading & Concurrency (Q25–Q30)
25. **How do you prevent the JavaFX UI from freezing?** All network and SQLite operations run inside `javafx.concurrent.Task` on background threads (`SharedFiles-ScannerTask`, `PeersTab-DatabaseTask`, `DownloadTab-ListFiles`, `DownloadTab-Transfer`, `HistoryTab-DatabaseTask`).
26. **Why is `DownloadTab-Transfer` non-daemon while `DownloadTab-ListFiles` is daemon?** `worker.setDaemon(!download)` lets an active file download finish writing its file and history before the JVM exits, whereas read-only list queries can terminate immediately.
27. **How does `PeerServer` handle multiple simultaneous clients?** `listenForClients` spawns a `PeerServer-ClientHandler` daemon thread per accepted socket, bounded by `new Semaphore(10)`.
28. **Why is `clients` in `PeerServer` a `ConcurrentHashMap.newKeySet()`?** The accept thread adds sockets and handler threads remove them in `finally`, while `stop()` iterates them from the FX shutdown thread.
29. **How does `DownloadTab` cancel an in-flight download?** Clicking `Cancel download` sets an `AtomicBoolean` to `true`; `SocketDeadline` polls `cancelled::get` every 100 ms and closes the `Socket`.
30. **Why is `PeerServer.running` declared `volatile`?** Ensures visibility across the FX thread calling `stop()` and `PeerServer-AcceptThread` checking `while (running)`.

## F. Networking & Sockets (Q31–Q38)
31. **How can PeerShare act as both client and server?** `MainApp.init()` starts `PeerServer` on `server.port`, while `DownloadTab` uses `FileTransferClient` to open outbound `Socket`s to other peers' ports.
32. **Why is TCP appropriate for file transfer?** TCP guarantees ordered, reliable byte delivery and flow control, unlike UDP.
33. **How does the custom protocol distinguish commands and responses?** Client sends `LIST\n` or `DOWNLOAD <name>\n`; server replies `OK ...\n` or `ERROR <msg>\n`.
34. **Why does `Protocol.readLine` read one byte at a time instead of `BufferedReader`?** `BufferedReader` buffers ahead and would swallow the beginning of raw binary file bytes sent immediately after `OK <size> <sha256>\n`.
35. **Why is `server.bindAddress` restricted to `127.0.0.1` by default?** Prevents exposing `shared_files/` to untrusted LAN hosts unless the user explicitly sets `0.0.0.0`.
36. **What timeouts protect client requests?** Connect timeout `3000 ms`, `SO_TIMEOUT` read timeout `15000 ms`, and overall `SocketDeadline` `120_000 ms`.
37. **What happens when multiple users request files simultaneously?** Up to `10` concurrent connections are served in parallel; an 11th connection is immediately closed (`!clientSlots.tryAcquire()`).
38. **What happens if a peer disconnects mid-transfer?** `in.read()` returns `-1` before `expectedSize` bytes are read, throwing `IOException("Connection interrupted...")`; `TransferService` deletes the `.part` file and logs `FAILED`.

## G. JavaFX (Q39–Q43)
39. **Why does `Launcher.java` exist separately from `MainApp.java`?** `Launcher` does not extend `Application`, allowing JavaFX to launch cleanly from the classpath (`-cp`) without requiring `--module-path` flags.
40. **How is navigation implemented in `MainView`?** Four `ToggleButton`s in a `ToggleGroup` swap the `content` `StackPane` child and trigger each tab's refresh method.
41. **How are file sizes and dates formatted in tables?** `ViewParts.bytes(long)` formats `B`, `KB`, `MB`; `ViewParts.date(LocalDateTime)` uses `DateTimeFormatter.ofLocalizedDateTime(MEDIUM, SHORT)`.
42. **How are `SUCCESS` and `FAILED` styled in `HistoryTab`?** Custom `TableCell` maps `"SUCCESS"` → `"Saved"` (`.transfer-success` green) and `"FAILED"` → `"Failed"` (`.transfer-failed` red).
43. **How is `Remove` confirmation handled?** `ViewParts.confirm(...)` displays a modal `Alert.AlertType.CONFIRMATION` with `Cancel` as the default button.

## H. JDBC & SQLite (Q44–Q49)
44. **Why SQLite instead of MySQL/PostgreSQL?** Serverless, zero-install, single local file (`peershare.db`), ideal for a standalone desktop app.
45. **Why store only metadata in SQLite and not file bytes?** Avoids database bloat; the OS filesystem handles large binary files efficiently.
46. **How are SQLite resources released?** Every DAO wraps `Connection`, `PreparedStatement`, and `ResultSet` in `try-with-resources`.
47. **How is SQL injection prevented?** All queries use `PreparedStatement` with `?` placeholders.
48. **How does `SharedFileDao.replaceAll` maintain atomicity?** Disables `autoCommit`, runs `DELETE FROM shared_file` + `addBatch()`/`executeBatch()`, calls `commit()`, or `rollback()` in `catch (SQLException)`.
49. **Why does `download_history` store `peer_name` as `TEXT` instead of a foreign key?** Preserves historical logs even if the user later deletes or edits the peer in `PeersTab`.

## I. File Handling & Security (Q50–Q57)
50. **How does PeerShare prevent path traversal?** `InputValidator.isSafeFileName` rejects `..`, `/`, `\`, `|`, `:`, `< > " ? *`, Windows device names, and control chars; `ClientHandler` & `TransferService` also normalize and check `target.startsWith(normalizedBase)`.
51. **How are symbolic links handled?** `FileScanner` and `ClientHandler` reject `Files.isSymbolicLink(...)`; `AppConfig` resolves symlinks via `toRealPath()`.
52. **Why download to a `.part` file first?** Prevents incomplete/corrupt files from appearing in `downloads/` under the final filename.
53. **How does PeerShare avoid overwriting existing files (including race conditions)?** `TransferService` uses `Files.createLink(destination, tempFile)`, an OS-level atomic hard-link creation that fails with `FileAlreadyExistsException` if `destination` already exists.
54. **How is transfer integrity verified?** `ClientHandler` computes SHA-256 and sends it in `OK <size> <sha256>`; `FileTransferClient` computes SHA-256 while streaming and rejects mismatches, truncation, or extra bytes.
55. **How does `ClientHandler` detect if a file changes while being sent?** Compares `Files.size` and `Files.getLastModifiedTime` before and after streaming.
56. **Does PeerShare provide encryption?** No; transfers are plaintext TCP.
57. **Why is `.gitkeep` excluded in `FileScanner`?** Commit `230bb5f` added `! ".gitkeep".equalsIgnoreCase(fileName)` in `isShareableName` so repository placeholder files are not advertised or downloadable.

## J. Architecture, Testing & Limitations (Q58–Q65)
58. **Why is the DAO pattern used?** Isolates all SQL and `ResultSet` mapping from UI and service classes.
59. **How is `MainApp` structured as a composition root?** `MainApp.start()` instantiates DAOs, services, and passes them into `MainView` via constructors.
60. **Why was a custom test harness (`TestSupport` + `AllTests`) used instead of JUnit?** Keeps dependencies minimal (only JavaFX and SQLite JDBC in `lib/`).
61. **How do network tests run without two computers?** `PeerServer(0, tempDir)` binds ephemeral OS port `0` on `127.0.0.1` and `server.port()` returns the assigned port.
62. **What happens in `AllTests` on Windows right now?** `NetworkTransferTest.testStopClosesConnectedClient` throws `SocketException: Connection reset` because Windows TCP sends RST when closing an idle server socket; the other 36 audited test methods pass.
63. **What are the main limitations of PeerShare?** No TLS encryption, no authentication, no auto-discovery, hard-link requirement on `downloads/`, single-file download at a time, no resume.
64. **How would you add encryption in the future?** Replace `ServerSocket`/`Socket` with `SSLServerSocket`/`SSLSocket` using a keystore/truststore or pairing secret.
65. **How would you support resumable downloads?** Extend the protocol with `DOWNLOAD <file> <offset>` and persist/validate partial `.part` hashes.

---

## Live Laboratory Demonstration Script (5–7 Minutes)
1. **(0:00–1:00) Architecture & Build:** Show `config.properties`, explain `127.0.0.1` default, run `.\scripts\compile.ps1` (shows clean Java 17 compilation).
2. **(1:00–2:30) Two-Instance Launch:** Start Peer 1 (port 5000) and Peer 2 (port 5001) in separate folders. Show `Shared Files` tab on Peer 2 with sample files.
3. **(2:30–4:00) Peer Registration & `LIST`:** On Peer 1 `Peers` tab, test invalid port validation, then add `127.0.0.1:5001`. Click `Browse files` → switches to `Download` tab and lists Peer 2's files.
4. **(4:00–5:30) Download, Duplicate Guard & History:** Download a file → show `Saved ... to downloads.`, verify in `History` (`Saved` in green). Try downloading the same file again → show duplicate protection (`Download already exists`).
5. **(5:30–6:30) Automated Tests & Wrap-up:** Run `.\scripts\test.ps1` (note the 36/37 pass and the Windows TCP RST behavior on `testStopClosesConnectedClient`), and summarize key Java concepts (records, NIO hard links, SHA-256, `Task`, JDBC transactions).
