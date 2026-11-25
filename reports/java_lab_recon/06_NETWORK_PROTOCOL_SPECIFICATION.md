# 06 — Networking and Communication Protocol Specification

**Audited Commit:** `230bb5fb750812f7d1f19a3fc5586932f99e58ed`  
**Implementing Files:** `Protocol.java`, `PeerServer.java`, `ClientHandler.java`, `FileTransferClient.java`, `SocketDeadline.java`

## 1. Transport & Addressing Model
- **Transport Protocol:** TCP (`java.net.ServerSocket` / `java.net.Socket`).
- **Default Bind Address:** `127.0.0.1` (`config.properties` / `AppConfig.java` L37). Can be set to `0.0.0.0` for LAN access.
- **Default Port:** `5000` (`config.properties` L1); valid range `1..65535`.
- **Peer Discovery:** Manual registration only (IP/hostname + port stored in SQLite `peer` table). No mDNS/UDP broadcast discovery.
- **Connection Model:** Short-lived request-response TCP connections (one TCP connection per `LIST` or `DOWNLOAD` operation, closed immediately upon completion).

## 2. Protocol Constants & Resource Bounds (`Protocol.java`, `PeerServer.java`, `FileTransferClient.java`, `ClientHandler.java`)

| Parameter | Exact Value | Source Location |
|---|---|---|
| `Protocol.MAX_LINE_BYTES` | `4096` bytes | `Protocol.java` L14 |
| `Protocol.MAX_LIST_FILES` | `500` files | `Protocol.java` L15 |
| `Protocol.MAX_FILE_BYTES` | `104,857,600` bytes (`100L * 1024 * 1024`, 100 MiB) | `Protocol.java` L16 |
| `PeerServer.MAX_CLIENTS` | `10` concurrent connections (`Semaphore`) | `PeerServer.java` L24, L34 |
| Server `accept()` backlog | `50` | `PeerServer.java` L65 |
| Server `SO_REUSEADDR` | `true` | `PeerServer.java` L66 |
| Server client `SO_TIMEOUT` | `5000` ms | `PeerServer.java` L86 |
| Server `ClientHandler` deadline | `120_000` ms (120 s) | `ClientHandler.java` L26, L41 |
| Client default connect timeout | `3000` ms | `FileTransferClient.java` L27 |
| Client default read timeout (`SO_TIMEOUT`) | `15000` ms | `FileTransferClient.java` L28 |
| Client default overall request deadline | `120_000` ms (120 s) | `FileTransferClient.java` L29 |
| Transfer stream buffer size | `8192` bytes (8 KB) | `ClientHandler.java` L103, `FileTransferClient.java` L165 |
| Line encoding & framing | UTF-8, `\n`-terminated (`\r` stripped on read) | `Protocol.java` L35-L66 |

## 3. Wire Protocol Specification

| Direction | Command / Response | Exact Wire Format | Field Ordering & Types |
|---|---|---|---|
| Client → Server | `LIST` | `LIST\n` | Literal command string |
| Server → Client | `LIST` Success | `OK <count>\n` followed by `<count>` lines of `<fileName>\|<fileSize>\|<lastModified>\n` | `count`: int `0..500`; `fileName`: safe String; `fileSize`: long `0..104857600`; `lastModified`: ISO-8601 `LocalDateTime` |
| Server → Client | `LIST` Error | `ERROR Too many shared files\n` | When shareable files `<= 100 MiB` exceed `500` |
| Client → Server | `DOWNLOAD` | `DOWNLOAD <fileName>\n` | `fileName`: plain safe filename |
| Server → Client | `DOWNLOAD` Success | `OK <fileSize> <sha256Hex>\n<raw binary bytes>` | `fileSize`: long `0..104857600`; `sha256Hex`: 64-char lowercase hex SHA-256; followed immediately by exact `fileSize` bytes |
| Server → Client | `DOWNLOAD` Error | `ERROR <message>\n` | E.g. `ERROR Unsafe or invalid filename`, `ERROR File not found`, `ERROR File is too large` |
| Server → Client | Unknown Command | `ERROR Unknown command\n` | Any line not equal to `LIST` or starting with `"DOWNLOAD "` |

*Why byte-by-byte `Protocol.readLine`?* `BufferedReader.readLine()` buffers ahead (e.g., 8 KB) and would consume the start of the raw binary payload following `OK <size> <sha256>\n`. `Protocol.readLine(InputStream)` reads 1 byte at a time until `\n` (enforcing `MAX_LINE_BYTES = 4096`), leaving binary bytes intact on the `InputStream`.

## 4. Representative Request-Response Examples

**Example 1: `LIST` Exchange**
```text
C -> S: LIST\n
S -> C: OK 2\n
S -> C: alpha.txt|57|2026-11-25T14:37:59.012345\n
S -> C: beta.bin|65536|2026-11-25T14:37:59.015678\n
```

**Example 2: `DOWNLOAD` Exchange**
```text
C -> S: DOWNLOAD alpha.txt\n
S -> C: OK 57 e5ab4cee63a80c37793c7f3620165a7fa23f8d1de7026c8ef95c769e7f76951c\n
S -> C: <57 raw bytes of alpha.txt>
```

**Example 3: `DOWNLOAD` Error (Traversal Attempt)**
```text
C -> S: DOWNLOAD ../secret.txt\n
S -> C: ERROR Unsafe or invalid filename\n
```

## 5. Authentication, Encryption, and Integrity
- **Authentication:** **NOT IMPLEMENTED.** Any host that can reach `bindAddress:port` can `LIST` and `DOWNLOAD` any shareable file in `shared_files/`.
- **Encryption:** **NOT IMPLEMENTED.** Plaintext TCP sockets (no TLS/SSL).
- **Transfer Integrity:** **SOURCE & RUNTIME VERIFIED.** Beyond TCP's transport checksum, PeerShare computes a **SHA-256** digest on the sender (`ClientHandler.checksum`) and verifies it incrementally on the receiver (`FileTransferClient.download`), alongside exact byte-count checks (`in.read() != -1` rejects trailing bytes; `bytesRead == -1` rejects truncated streams) and server-side TOCTOU re-checks (`Files.size` and `Files.getLastModifiedTime` after streaming).
