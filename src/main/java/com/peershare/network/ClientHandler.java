package com.peershare.network;

import com.peershare.model.SharedFile;
import com.peershare.service.FileScanner;
import com.peershare.util.AppLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ClientHandler processes a single TCP client connection on its own thread.
 * It reads commands (such as LIST or DOWNLOAD) and returns appropriate responses.
 */
public final class ClientHandler implements Runnable {
    private static final Logger LOGGER = AppLogger.create();
    private static final int REQUEST_TIMEOUT_MS = 120_000;

    private final Socket socket;
    private final Path sharedDirectory;
    private final FileScanner fileScanner;

    public ClientHandler(Socket socket, Path sharedDirectory, FileScanner fileScanner) {
        this.socket = socket;
        this.sharedDirectory = sharedDirectory;
        this.fileScanner = fileScanner;
    }

    @Override
    public void run() {
        try (socket;
             SocketDeadline deadline = new SocketDeadline(socket, REQUEST_TIMEOUT_MS, null);
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            String requestLine = Protocol.readLine(in);
            if (requestLine == null || requestLine.isBlank()) {
                return;
            }

            if (requestLine.equals(Protocol.CMD_LIST)) {
                handleList(out);
            } else if (requestLine.startsWith(Protocol.CMD_DOWNLOAD + " ")) {
                String requestedFile = requestLine.substring(Protocol.CMD_DOWNLOAD.length() + 1);
                handleDownload(out, requestedFile);
            } else {
                Protocol.writeLine(out, Protocol.STATUS_ERROR + " Unknown command");
            }
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Client connection ended: {0}", e.getMessage());
        }
    }

    private void handleList(OutputStream out) throws IOException {
        // Folder is scanned at request time so remote peers always see current files
        List<SharedFile> scanned = fileScanner.scan();
        List<SharedFile> files = scanned.stream()
                .filter(file -> file.fileSize() <= Protocol.MAX_FILE_BYTES)
                .limit(Protocol.MAX_LIST_FILES)
                .toList();
        int omitted = scanned.size() - files.size();
        Protocol.writeLine(out, Protocol.STATUS_OK + " " + files.size() + (omitted == 0 ? "" : " " + omitted));

        for (SharedFile file : files) {
            Protocol.writeLine(out, file.fileName() + "|" + file.fileSize() + "|" + file.lastModified());
        }
    }

    private void handleDownload(OutputStream out, String fileName) throws IOException {
        if (!FileScanner.isShareableName(fileName)) {
            Protocol.writeLine(out, Protocol.STATUS_ERROR + " Unsafe or invalid filename");
            return;
        }

        Path normalizedBase = sharedDirectory.toAbsolutePath().normalize();
        Path target = normalizedBase.resolve(fileName).normalize();
        if (!target.startsWith(normalizedBase) || Files.isSymbolicLink(target) || !Files.isRegularFile(target)) {
            Protocol.writeLine(out, Protocol.STATUS_ERROR + " File not found");
            return;
        }

        var initial = Files.readAttributes(target, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!initial.isRegularFile()) {
            Protocol.writeLine(out, Protocol.STATUS_ERROR + " File not found");
            return;
        }
        long fileSize = initial.size();
        if (fileSize > Protocol.MAX_FILE_BYTES) {
            Protocol.writeLine(out, Protocol.STATUS_ERROR + " File is too large");
            return;
        }
        String checksum = checksum(target, fileSize);
        Protocol.writeLine(out, Protocol.STATUS_OK + " " + fileSize + " " + checksum);

        // Send exactly the announced number of bytes; detect a shrinking source.
        try (InputStream fileIn = Files.newInputStream(target, LinkOption.NOFOLLOW_LINKS)) {
            byte[] buffer = new byte[8192];
            long sent = 0;
            while (sent < fileSize) {
                int read = fileIn.read(buffer, 0, (int) Math.min(buffer.length, fileSize - sent));
                if (read == -1) throw new IOException("Shared file changed during transfer.");
                out.write(buffer, 0, read);
                sent += read;
            }
        }
        var current = Files.readAttributes(target, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!current.isRegularFile() || current.size() != fileSize
                || !current.lastModifiedTime().equals(initial.lastModifiedTime())
                || !Objects.equals(current.fileKey(), initial.fileKey())) {
            Protocol.writeLine(out, Protocol.STATUS_ERROR + " Shared file changed during transfer. Refresh and try again.");
            return;
        }
        Protocol.writeLine(out, Protocol.STATUS_COMPLETE);
    }

    private static String checksum(Path file, long size) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
                byte[] bytes = new byte[8192];
                long remaining = size;
                while (remaining > 0) {
                    int read = in.read(bytes, 0, (int) Math.min(bytes.length, remaining));
                    if (read == -1) throw new IOException("Shared file changed before transfer.");
                    digest.update(bytes, 0, read);
                    remaining -= read;
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
