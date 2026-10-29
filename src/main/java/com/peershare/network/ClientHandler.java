package com.peershare.network;

import com.peershare.model.SharedFile;
import com.peershare.service.FileScanner;
import com.peershare.util.AppLogger;
import com.peershare.util.InputValidator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ClientHandler processes a single TCP client connection on its own thread.
 * It reads commands (such as LIST or DOWNLOAD) and returns appropriate responses.
 */
public final class ClientHandler implements Runnable {
    private static final Logger LOGGER = AppLogger.create();

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
        List<SharedFile> files = fileScanner.scan().stream()
                .filter(file -> file.fileSize() <= Protocol.MAX_FILE_BYTES)
                .toList();
        if (files.size() > Protocol.MAX_LIST_FILES) {
            Protocol.writeLine(out, Protocol.STATUS_ERROR + " Too many shared files");
            return;
        }
        Protocol.writeLine(out, Protocol.STATUS_OK + " " + files.size());

        for (SharedFile file : files) {
            Protocol.writeLine(out, file.fileName() + "|" + file.fileSize() + "|" + file.lastModified());
        }
    }

    private void handleDownload(OutputStream out, String fileName) throws IOException {
        if (!InputValidator.isSafeFileName(fileName)) {
            Protocol.writeLine(out, Protocol.STATUS_ERROR + " Unsafe or invalid filename");
            return;
        }

        Path normalizedBase = sharedDirectory.toAbsolutePath().normalize();
        Path target = normalizedBase.resolve(fileName).normalize();
        if (!target.startsWith(normalizedBase) || Files.isSymbolicLink(target) || !Files.isRegularFile(target)) {
            Protocol.writeLine(out, Protocol.STATUS_ERROR + " File not found");
            return;
        }

        long fileSize = Files.size(target);
        var lastModified = Files.getLastModifiedTime(target);
        if (fileSize > Protocol.MAX_FILE_BYTES) {
            Protocol.writeLine(out, Protocol.STATUS_ERROR + " File is too large");
            return;
        }
        String checksum = checksum(target);
        Protocol.writeLine(out, Protocol.STATUS_OK + " " + fileSize + " " + checksum);

        // Send exactly the announced number of bytes; detect a shrinking source.
        try (InputStream fileIn = Files.newInputStream(target)) {
            byte[] buffer = new byte[8192];
            long sent = 0;
            while (sent < fileSize) {
                int read = fileIn.read(buffer, 0, (int) Math.min(buffer.length, fileSize - sent));
                if (read == -1) throw new IOException("Shared file changed during transfer.");
                out.write(buffer, 0, read);
                sent += read;
            }
        }
        if (Files.size(target) != fileSize || !Files.getLastModifiedTime(target).equals(lastModified)) {
            throw new IOException("Shared file changed during transfer.");
        }
        out.flush();
    }

    private static String checksum(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] bytes = new byte[8192];
                int read;
                while ((read = in.read(bytes)) != -1) digest.update(bytes, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
