# 16 — Limitations, Future Scope and Academic Learning Outcomes

## 1. Verified Technical Limitations

| Limitation | Current Behavior | Technical Cause | Practical Consequence | Potential Future Improvement |
|---|---|---|---|---|
| **No Encryption (Plaintext TCP)** | Protocol headers and file bytes are sent unencrypted. | Uses plain `Socket` / `ServerSocket`. | Vulnerable to eavesdropping on untrusted Wi-Fi/LAN. | Wrap sockets with `SSLServerSocket` / `SSLSocket` (TLS). |
| **No Authentication** | Any reachable host can `LIST` and `DOWNLOAD` shared files. | No login, token, or peer approval handshake. | Relies entirely on `127.0.0.1` default or trusted LAN. | Add pre-shared PIN/token or mutual TLS peer certificates. |
| **Manual Peer Discovery** | Users must type IP address and port manually. | No UDP broadcast, mDNS, or central directory. | Inconvenient when DHCP changes LAN IPs. | Add mDNS / UDP LAN discovery beacon. |
| **Hard-Link Filesystem Dependency** | `Files.createLink` publishes downloads. | OS must support hard links on `downloads/`. | Fails with `UnsupportedOperationException` on FAT32/exFAT. | Fallback to `Files.move(..., ATOMIC_MOVE)` when hard links unsupported. |
| **Single-File Download at a Time** | UI disables controls (`busy = true`) during download. | Single `activeDownload` state in `DownloadTab`. | Cannot queue multiple files or browse while downloading. | Add a transfer queue backed by `ExecutorService`. |
| **No Resumable Transfers** | Interrupted downloads delete `.part` and restart from byte 0. | Protocol lacks byte-range offset (`DOWNLOAD <file> <offset>`). | Large files near 100 MB must restart on Wi-Fi drop. | Support byte-range resume headers. |
| **Flat Directory Sharing** | Subdirectories in `shared_files/` are ignored. | `FileScanner` filters `Files.isRegularFile` directly. | Cannot share nested folder trees. | Add optional recursive listing or ZIP-on-the-fly. |
| **Windows `AllTests` Failure** | `testStopClosesConnectedClient` throws `Connection reset`. | Closing server-side socket sends TCP RST on Windows Winsock. | `.\scripts\test.ps1` exits `1` before `TransferServiceTest`. | Handle `SocketException` or `shutdownOutput()` in test/server. |
| **Platform-Specific `lib/` JARs** | Bundled `javafx-*.jar` are Windows-only. | Platform natives inside JavaFX JARs. | macOS/Linux users must swap JavaFX JARs. | Adopt Maven/Gradle with OS-classifier dependencies. |

## 2. Academic Learning Outcomes (`SOURCE VERIFIED`)
1. **Object-Oriented Programming & Java 17:** Immutable `record`s, constructor/method overloading, encapsulation, `final` utility classes.
2. **GUI & Event-Driven Programming:** JavaFX `Application` lifecycle, `TableView` cell factories, `FilteredList`/`SortedList`, CSS theming, non-blocking `javafx.concurrent.Task`.
3. **Network Programming:** TCP `ServerSocket`/`Socket`, custom text+binary framing (`Protocol.readLine`), timeouts, `SO_REUSEADDR`.
4. **Concurrency:** Daemon threads, `Semaphore(10)`, `ConcurrentHashMap.newKeySet()`, `ScheduledThreadPoolExecutor`, `AtomicBoolean`, `volatile`.
5. **Database Integration:** JDBC `DriverManager`, `PreparedStatement`, `ResultSet`, `RETURN_GENERATED_KEYS`, manual transactions, DAO pattern.
6. **File I/O & Defensive Programming:** NIO `Path`/`Files`, path-traversal checks, symlink containment, TOCTOU checks, SHA-256 `MessageDigest`, `Files.createLink`.
7. **Automated Testing:** Custom assertion harness (`TestSupport`), loopback integration tests, failure-path & concurrency-race testing.
