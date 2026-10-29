package com.peershare.network;

import com.peershare.model.Peer;
import com.peershare.model.SharedFile;
import com.peershare.util.InputValidator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * FileTransferClient opens short-lived TCP connections to remote peers
 * to request their list of shared files or download a file.
 */
public final class FileTransferClient {
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 3000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 15000;

    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public FileTransferClient() {
        this(DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
    }

    public FileTransferClient(int connectTimeoutMs, int readTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    /**
     * Connects to a remote peer and requests its shared file list using the LIST command.
     *
     * @param peer the peer to connect to
     * @return the list of SharedFile metadata offered by the remote peer
     * @throws IOException if the peer is unavailable or returns an error
     */
    public List<SharedFile> listRemoteFiles(Peer peer) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(peer.ipAddress(), peer.port()), connectTimeoutMs);
            socket.setSoTimeout(readTimeoutMs);
        } catch (IOException e) {
            socket.close();
            throw new IOException("Could not connect to peer " + peer.name() + " at "
                    + peer.ipAddress() + ":" + peer.port() + " (" + e.getMessage() + ")", e);
        }

        try (socket;
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            // Send LIST command
            Protocol.writeLine(out, Protocol.CMD_LIST);

            // Read header response (e.g., "OK 3" or "ERROR ...")
            String header = Protocol.readLine(in);
            if (header == null) {
                throw new IOException("Connection closed by peer before sending response.");
            }

            if (header.startsWith(Protocol.STATUS_ERROR)) {
                String errorMsg = header.substring(Protocol.STATUS_ERROR.length()).trim();
                throw new IOException("Peer returned error: " + errorMsg);
            }

            if (!header.startsWith(Protocol.STATUS_OK)) {
                throw new IOException("Unexpected protocol response from peer: " + header);
            }

            int count = parseCount(header);
            List<SharedFile> remoteFiles = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                String line = Protocol.readLine(in);
                if (line == null) {
                    throw new IOException("Connection interrupted while reading file list.");
                }

                String[] parts = line.split("\\|", 3);
                if (parts.length != 3 || !InputValidator.isSafeFileName(parts[0])) {
                    throw new IOException("Invalid file list entry from peer.");
                }
                String fileName = parts[0];
                long fileSize = parseSize(parts[1]);
                LocalDateTime lastModified;
                try {
                    lastModified = LocalDateTime.parse(parts[2]);
                } catch (RuntimeException ex) {
                    throw new IOException("Invalid date in file list.", ex);
                }

                remoteFiles.add(new SharedFile(0, fileName, fileSize, lastModified));
            }

            return remoteFiles;
        }
    }

    /**
     * Downloads a remote file from the specified peer to a temporary target path.
     *
     * @param peer the remote peer to download from
     * @param fileName the plain name of the file to download
     * @param temporaryTarget the file path where downloaded bytes will be saved
     * @return the temporary target path upon complete byte verification
     * @throws IOException if network fails, peer returns an error, or bytes are incomplete
     */
    public Path download(Peer peer, String fileName, Path temporaryTarget) throws IOException {
        return download(peer, fileName, temporaryTarget, -1);
    }

    public Path download(Peer peer, String fileName, Path temporaryTarget, long listedSize) throws IOException {
        if (!InputValidator.isSafeFileName(fileName)) {
            throw new IOException("Unsafe or invalid filename.");
        }
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(peer.ipAddress(), peer.port()), connectTimeoutMs);
            socket.setSoTimeout(readTimeoutMs);
        } catch (IOException e) {
            socket.close();
            throw new IOException("Could not connect to peer " + peer.name() + " at "
                    + peer.ipAddress() + ":" + peer.port() + " (" + e.getMessage() + ")", e);
        }

        try (socket;
             InputStream in = socket.getInputStream();
             OutputStream out = socket.getOutputStream()) {

            // Send DOWNLOAD command
            Protocol.writeLine(out, Protocol.CMD_DOWNLOAD + " " + fileName);

            // Read header response (e.g. "OK 2048 <sha256>" or "ERROR ...")
            String header = Protocol.readLine(in);
            if (header == null) {
                throw new IOException("Connection closed by peer before sending response.");
            }

            if (header.startsWith(Protocol.STATUS_ERROR)) {
                String errorMsg = header.substring(Protocol.STATUS_ERROR.length()).trim();
                throw new IOException("Peer returned error: " + errorMsg);
            }

            if (!header.startsWith(Protocol.STATUS_OK)) {
                throw new IOException("Unexpected protocol response from peer: " + header);
            }

            String[] metadata = header.substring(Protocol.STATUS_OK.length()).trim().split(" ", 2);
            if (metadata.length != 2 || !metadata[1].matches("[0-9a-f]{64}")) {
                throw new IOException("Invalid download header from peer.");
            }
            long expectedSize = parseSize(metadata[0]);
            if (listedSize >= 0 && expectedSize != listedSize) {
                throw new IOException("File size changed since it was listed. Refresh the file list.");
            }

            if (temporaryTarget.getParent() != null) {
                Files.createDirectories(temporaryTarget.getParent());
            }

            // Stream bytes into temporary target and verify byte count
            byte[] buffer = new byte[8192];
            MessageDigest digest = sha256();
            long totalRead = 0;
            long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(2);
            try (OutputStream fileOut = Files.newOutputStream(temporaryTarget)) {
                while (totalRead < expectedSize) {
                    setRemainingTimeout(socket, deadline);
                    int toRead = (int) Math.min(buffer.length, expectedSize - totalRead);
                    int bytesRead = in.read(buffer, 0, toRead);
                    if (bytesRead == -1) {
                        throw new IOException("Connection interrupted while streaming file. Expected "
                                + expectedSize + " bytes but only received " + totalRead);
                    }
                    fileOut.write(buffer, 0, bytesRead);
                    digest.update(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }
                fileOut.flush();
            }

            setRemainingTimeout(socket, deadline);
            if (in.read() != -1) {
                throw new IOException("Peer sent more bytes than announced.");
            }
            if (!HexFormat.of().formatHex(digest.digest()).equals(metadata[1])) {
                throw new IOException("Downloaded file checksum does not match.");
            }

            return temporaryTarget;
        }
    }

    private static int parseCount(String header) throws IOException {
        try {
            int count = Integer.parseInt(header.substring(Protocol.STATUS_OK.length()).trim());
            if (count < 0 || count > Protocol.MAX_LIST_FILES) throw new NumberFormatException();
            return count;
        } catch (NumberFormatException ex) {
            throw new IOException("Invalid file count from peer.", ex);
        }
    }

    private static long parseSize(String value) throws IOException {
        try {
            long size = Long.parseLong(value);
            if (size < 0 || size > Protocol.MAX_FILE_BYTES) throw new NumberFormatException();
            return size;
        } catch (NumberFormatException ex) {
            throw new IOException("Invalid file size from peer.", ex);
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private void setRemainingTimeout(Socket socket, long deadline) throws IOException {
        long remaining = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
        if (remaining <= 0) throw new IOException("Download timed out.");
        socket.setSoTimeout((int) Math.min(readTimeoutMs, remaining));
    }
}
