# 10 — Security, Reliability and Defensive Programming

**Audited Commit:** `230bb5fb750812f7d1f19a3fc5586932f99e58ed`

## 1. Verified Defensive Mechanisms (`SOURCE VERIFIED`)

| Category | Mechanism | Implementation Location |
|---|---|---|
| **Path Traversal & Filename Sanitization** | Rejects `null`, blank, `.`, `..`, `/`, `\`, `\|`, `:`, `< > " ? *`, Windows reserved device names (`CON`, `PRN`, `AUX`, `NUL`, `COM1-9`, `LPT1-9`), trailing space/dot, control chars `<32` / `127`, and `.gitkeep`. | `InputValidator.isSafeFileName` (L31-L45), `FileScanner.isShareableName` (L40-L42) |
| **Symlink & Directory Containment** | `AppConfig.resolveInsideRoot` resolves existing links via `toRealPath()` and rejects root/outside/overlapping folders. `FileScanner` & `ClientHandler` check `!Files.isSymbolicLink` and `target.startsWith(normalizedBase)`. | `AppConfig.java` L42-L72, `FileScanner.java` L67, `ClientHandler.java` L85-L88 |
| **SQL Injection Prevention** | All SQL queries use `PreparedStatement` with `?` parameter binding; zero string concatenation of user input. | `PeerDao.java`, `SharedFileDao.java`, `DownloadHistoryDao.java` |
| **Atomic File Publication & Overwrite Protection** | Downloads to unique `peershare-*.part` (`Files.createTempFile`), verifies size + SHA-256, then publishes via `Files.createLink(destination, tempFile)` (OS-level atomic hard link that fails on collision). `.part` deleted in `finally`. | `TransferService.java` L88-L145 |
| **TOCTOU Source-File Check** | `ClientHandler` records `fileSize` and `lastModified` before streaming and re-verifies both after streaming; throws `IOException` if file shrank or changed. | `ClientHandler.java` L92-L114 |
| **Resource Exhaustion Bounds** | `Protocol.MAX_LINE_BYTES = 4096`, `MAX_LIST_FILES = 500`, `MAX_FILE_BYTES = 100 MiB`, `PeerServer.MAX_CLIENTS = 10` (`Semaphore`). | `Protocol.java` L14-L16, `PeerServer.java` L24, L91 |
| **Timeouts & Cancellation** | Connect timeout `3s`, read timeout `15s` (client) / `5s` (server), overall `SocketDeadline` `120s`, 100 ms cancellation polling. | `FileTransferClient.java`, `SocketDeadline.java`, `ClientHandler.java` |
| **Loopback-Default Exposure** | `server.bindAddress` defaults to `127.0.0.1` and only accepts `127.0.0.1` or `0.0.0.0`. | `AppConfig.java` L37-L40 |
| **Graceful Shutdown** | `MainApp.stop()` calls `PeerServer.stop()`, closing `ServerSocket` and all tracked client sockets, joining `acceptThread`. | `MainApp.java` L88-L93, `PeerServer.java` L134-L157 |

## 2. Verified Limitations & Trust Model (`NOT IMPLEMENTED` / `SOURCE VERIFIED`)
- **Trust Model:** PeerShare assumes a **trusted local network**.
- **No Authentication or Authorization:** Any host able to reach `bindAddress:port` can `LIST` and `DOWNLOAD` all shareable files.
- **No Encryption (No TLS):** All protocol headers and file bytes travel in plaintext over TCP.
- **No Peer Identity Verification:** No certificates, shared secrets, or host-key pinning.
- **No Automatic Peer Discovery / NAT Traversal:** Peers must be manually entered; no UPnP/STUN/relay for internet-wide use.
- **Hard-Link Filesystem Requirement:** `Files.createLink` requires the `downloads/` filesystem to support hard links (e.g., NTFS, ext4, APFS; may fail on FAT32/exFAT/some network mounts).
- **Assessment:** PeerShare is well-hardened for an undergraduate Java lab project on a trusted LAN, but is **not** enterprise-grade or safe for untrusted public networks without TLS and authentication.
