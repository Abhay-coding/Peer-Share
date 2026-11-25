# 07 — File Transfer Pipeline and Algorithms

**Audited Commit:** `230bb5fb750812f7d1f19a3fc5586932f99e58ed`

## 1. End-to-End File Lifecycle
1. **Sender Placement:** User places regular file in `shared_files/` (or configured `shared.directory`).
2. **Local Indexing (`FileScanner.scanAndStore`):** Scans direct regular non-symlink files (excluding `.gitkeep` and unsafe names), extracts `fileSize` and `lastModified`, sorts by `fileName`, and atomically replaces `shared_file` table rows.
3. **Remote Discovery (`FileTransferClient.listRemoteFiles`):** Receiver connects over TCP, sends `LIST\n`, receives `OK <count>` and `fileName|fileSize|lastModified` rows (filtered `<= 100 MiB`).
4. **Download Initialization (`TransferService.download`):** Validates `peer != null` and `InputValidator.isSafeFileName(fileName)`, checks `downloads/<fileName>` does not already exist, creates unique `peershare-*.part` temp file via `Files.createTempFile`.
5. **Streaming & Verification (`FileTransferClient.download`):** Sends `DOWNLOAD <fileName>\n`, parses `OK <size> <sha256>`, verifies `listedSize` match (if provided), streams in `8192`-byte chunks while updating `MessageDigest("SHA-256")`, verifies EOF (`in.read() == -1`) and SHA-256 hex equality.
6. **Atomic Publication (`TransferService.download`):** Publishes `destination` via `Files.createLink(destination, tempFile)` (single OS hard-link operation that fails with `FileAlreadyExistsException` if destination appeared mid-transfer).
7. **Cleanup & History (`TransferService.download`):** `finally` block deletes `tempFile`. Records `SUCCESS` (or `FAILED` if not published) in `download_history`.

---

## 2. Core Algorithms

### Algorithm 1: Shared Directory Scanning (`FileScanner.scan()`)
- **Input:** `Path sharedDirectory`
- **Output:** `List<SharedFile>` sorted alphabetically by `fileName`
- **Data Structures:** `ArrayList<SharedFile>`, `Stream<Path>`
- **Complexity:** Time $O(N \log N)$ where $N$ is the number of direct entries in `sharedDirectory` (dominated by `Comparator.comparing(SharedFile::fileName)` sort); Auxiliary Space $O(N)$.
```text
1. IF NOT Files.exists(sharedDirectory) THEN
       Files.createDirectories(sharedDirectory)
       RETURN empty list
2. OPEN Stream<Path> entries = Files.list(sharedDirectory) (try-with-resources)
3. FOR EACH entry IN entries:
       fileName <- entry.getFileName().toString()
       IF NOT Files.isSymbolicLink(entry)
          AND Files.isRegularFile(entry)
          AND isShareableName(fileName) THEN
              size <- Files.size(entry)
              modTime <- LocalDateTime.ofInstant(Files.getLastModifiedTime(entry).toInstant(), systemDefault)
              files.add(new SharedFile(0, fileName, size, modTime))
4. SORT files by SharedFile::fileName
5. RETURN files
```
- **Edge Cases:** Missing directory is auto-created; subdirectories, symlinks, `.gitkeep`, and Windows-reserved/unsafe filenames are ignored.

### Algorithm 2: Peer Registration & Validation (`InputValidator.validatePeer` + `PeerDao.create`)
- **Input:** `name`, `host`, `portText`
- **Output:** Persisted `Peer` with generated `id`
- **Complexity:** Time $O(1)$ validation + $O(1)$ SQLite single-row insert.
```text
1. IF name is null OR blank -> THROW IllegalArgumentException("Peer name is required.")
2. IF host is null OR blank -> THROW IllegalArgumentException("Peer IP address is required.")
3. port <- Integer.parseInt(portText.trim()) (catch NumberFormatException -> IllegalArgumentException)
4. IF port < 1 OR port > 65535 -> THROW IllegalArgumentException("Port must be between 1 and 65535.")
5. Execute INSERT INTO peer (name, ip_address, port) VALUES (?, ?, ?) with RETURN_GENERATED_KEYS
6. RETURN new Peer(generatedId, name.strip(), host.strip(), port)
```

### Algorithm 3: Remote File-List Retrieval (`FileTransferClient.listRemoteFiles`)
- **Input:** `Peer peer`
- **Output:** `List<SharedFile>`
- **Complexity:** Time $O(M)$ where $M \le 500$ is the number of listed files; Space $O(M)$.
```text
1. Open Socket with SocketDeadline(requestTimeoutMs=120s), connectTimeout=3s, soTimeout=15s
2. writeLine(out, "LIST")
3. header <- readLine(in)
4. IF header == null -> THROW IOException
5. IF header starts with "ERROR" -> THROW ListRejectedException(errorMsg)
6. IF NOT header starts with "OK" -> THROW IOException
7. count <- parseCount(header)  // must be 0..500
8. FOR i = 0 TO count - 1:
       line <- readLine(in)
       split into 3 parts by "|"
       validate InputValidator.isSafeFileName(parts[0]), parseSize(parts[1]), LocalDateTime.parse(parts[2])
       remoteFiles.add(new SharedFile(0, parts[0], size, date))
9. RETURN remoteFiles
```

### Algorithm 4: File Serving (`ClientHandler.handleDownload`)
- **Input:** `OutputStream out`, `String fileName`
- **Output:** Protocol header + raw file bytes written to `out`
- **Complexity:** Time $O(S)$ where $S$ is file size in bytes (two linear passes: SHA-256 computation + socket streaming); Auxiliary Space $O(1)$ (`8192`-byte buffer).
```text
1. IF NOT FileScanner.isShareableName(fileName) -> writeLine(out, "ERROR Unsafe or invalid filename"); RETURN
2. target <- sharedDirectory.toAbsolutePath().normalize().resolve(fileName).normalize()
3. IF NOT target.startsWith(base) OR Files.isSymbolicLink(target) OR NOT Files.isRegularFile(target)
       -> writeLine(out, "ERROR File not found"); RETURN
4. fileSize <- Files.size(target); lastModified <- Files.getLastModifiedTime(target)
5. IF fileSize > 100 MiB -> writeLine(out, "ERROR File is too large"); RETURN
6. checksum <- sha256Hex(target)
7. writeLine(out, "OK " + fileSize + " " + checksum)
8. Stream exact fileSize bytes in 8192-byte chunks (throw IOException on early EOF)
9. Re-check Files.size(target) == fileSize AND Files.getLastModifiedTime(target).equals(lastModified)
10. out.flush()
```

### Algorithm 5: File Download & Publication (`TransferService.download` + `FileTransferClient.download`)
- **Input:** `Peer peer`, `String fileName`, `long expectedSize`, `BooleanSupplier cancelled`
- **Output:** `Path destination`
- **Complexity:** Time $O(S)$ where $S$ is file size in bytes; Auxiliary Space $O(1)$ (`8192`-byte buffer + `MessageDigest`).
```text
1. Validate peer != null AND isSafeFileName(fileName)
2. destination <- downloadsDir.normalize().resolve(fileName).normalize()
3. IF NOT destination.startsWith(base) OR Files.exists(destination) -> THROW IOException("Download already exists")
4. tempFile <- Files.createTempFile(base, "peershare-", ".part")
5. Verify Files.isRegularFile(tempFile)
6. Client sends "DOWNLOAD " + fileName, parses "OK <expectedSize> <sha256>"
7. IF listedSize >= 0 AND expectedSize != listedSize -> THROW IOException("File size changed...")
8. Stream expectedSize bytes into tempFile in 8192-byte chunks while updating SHA-256 digest
9. Verify in.read() == -1 (no extra bytes) AND computed SHA-256 == header SHA-256
10. IF cancelled -> THROW CancellationException
11. Files.createLink(destination, tempFile); published <- true
12. Record "SUCCESS" in download_history (on SQLException -> throw HistoryWriteException(destination))
13. ON ANY EXCEPTION: if NOT published -> record "FAILED" in download_history (attach SQLException via addSuppressed)
14. FINALLY: Files.deleteIfExists(tempFile)
```

### Algorithm 6: Download Cancellation & Cleanup (`DownloadTab` + `SocketDeadline` + `TransferService`)
- **Mechanism:** User clicks **Cancel download** → sets `AtomicBoolean activeDownload` to `true`. `SocketDeadline` polls `cancelled.getAsBoolean()` every `100 ms` on `PeerShare-SocketDeadline` daemon thread and calls `socket.close()`, unblocking `in.read()`. `SocketDeadline.failure(ex)` throws `CancellationException("Download cancelled.")`. `TransferService` records `FAILED` in `download_history` and deletes `tempFile` in `finally`.

### Algorithm 7: Download History Recording (`TransferService.recordHistory` + `DownloadHistoryDao.create`)
- **Input:** `fileName`, `peerName`, `status` (`"SUCCESS"` or `"FAILED"`)
- **Output:** Persisted `DownloadHistory` row with `LocalDateTime.now()` and generated `id`.
- **Edge Case:** If `published == true` and `recordHistory` throws `SQLException`, `TransferService` throws `HistoryWriteException(destination, ex)` without deleting the downloaded file.
