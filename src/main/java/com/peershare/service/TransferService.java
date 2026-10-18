package com.peershare.service;

import com.peershare.config.AppConfig;
import com.peershare.dao.DownloadHistoryDao;
import com.peershare.model.DownloadHistory;
import com.peershare.model.Peer;
import com.peershare.network.FileTransferClient;
import com.peershare.util.AppLogger;
import com.peershare.util.InputValidator;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TransferService manages the downloading of files from remote peers.
 * It coordinates with FileTransferClient, writes files to a temporary .part
 * location, finalizes them atomically upon verification, and logs every
 * outcome (SUCCESS or FAILED) into the local SQLite database.
 */
public final class TransferService {
    private static final Logger LOGGER = AppLogger.create();

    private final Path downloadsDirectory;
    private final DownloadHistoryDao historyDao;
    private final FileTransferClient client;

    public TransferService(Path downloadsDirectory, DownloadHistoryDao historyDao, FileTransferClient client) {
        this.downloadsDirectory = downloadsDirectory;
        this.historyDao = historyDao;
        this.client = client;
    }

    public TransferService(AppConfig config, DownloadHistoryDao historyDao, FileTransferClient client) {
        this(config.downloadDirectory(), historyDao, client);
    }

    public TransferService(AppConfig config, DownloadHistoryDao historyDao) {
        this(config.downloadDirectory(), historyDao, new FileTransferClient());
    }

    public Path downloadsDirectory() {
        return downloadsDirectory;
    }

    /**
     * Downloads a file from a remote peer, validates the filename, streams bytes
     * to a temporary .part file, moves it to the final download path, and records
     * the result in download history.
     *
     * @param peer the peer to download from
     * @param fileName the plain name of the file to download
     * @return the final Path of the downloaded file
     * @throws IOException if network transfer fails or file is rejected
     */
    public Path download(Peer peer, String fileName) throws IOException {
        Files.createDirectories(downloadsDirectory);

        Path normalizedBase = downloadsDirectory.toAbsolutePath().normalize();
        Path tempFile = normalizedBase.resolve(fileName + ".part");
        Path destination = normalizedBase.resolve(fileName).normalize();

        try {
            // Validate filename against path traversal attacks (e.g. ../secret.txt)
            if (!InputValidator.isSafeFileName(fileName) || !destination.startsWith(normalizedBase)) {
                throw new IllegalArgumentException("Unsafe or invalid filename: " + fileName);
            }

            // Stream from peer to temporary .part file
            client.download(peer, fileName, tempFile);

            // Atomically finalize the file so no half-written files exist in downloads
            try {
                Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            // Record successful download in SQLite
            recordHistory(fileName, peer.name(), "SUCCESS");

            return destination;

        } catch (Exception e) {
            // Clean up any incomplete .part file
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException cleanupEx) {
                LOGGER.log(Level.WARNING, "Failed to clean up temp file: {0}", cleanupEx.getMessage());
            }

            // Record failed download in SQLite
            recordHistory(fileName, peer.name(), "FAILED");

            if (e instanceof IOException ioe) {
                throw ioe;
            } else if (e instanceof RuntimeException re) {
                throw re;
            } else {
                throw new IOException("Download failed: " + e.getMessage(), e);
            }
        }
    }

    private void recordHistory(String fileName, String peerName, String status) {
        try {
            historyDao.create(new DownloadHistory(0, fileName, peerName, LocalDateTime.now(), status));
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not record download history to database: {0}", e.getMessage());
        }
    }
}
