package com.peershare.service;

import com.peershare.config.AppConfig;
import com.peershare.dao.DownloadHistoryDao;
import com.peershare.model.DownloadHistory;
import com.peershare.model.Peer;
import com.peershare.network.FileTransferClient;
import com.peershare.util.AppLogger;
import com.peershare.util.InputValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TransferService manages the downloading of files from remote peers.
 * It coordinates with FileTransferClient, writes files to a temporary .part
 * location, moves them into place upon verification, and attempts to log
 * each outcome (SUCCESS or FAILED) in the local SQLite database.
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
        return download(peer, fileName, -1);
    }

    /** expectedSize is the size shown in the remote file list, or -1 when no list was used. */
    public Path download(Peer peer, String fileName, long expectedSize) throws IOException {
        Path tempFile = null;
        boolean published = false;
        Path normalizedBase = downloadsDirectory.toAbsolutePath().normalize();

        try {
            if (peer == null || !InputValidator.isSafeFileName(fileName)) {
                throw new IOException("Unsafe or invalid download request.");
            }
            Files.createDirectories(normalizedBase);
            Path destination = normalizedBase.resolve(fileName).normalize();
            if (!destination.startsWith(normalizedBase) || Files.exists(destination)) {
                throw new IOException("Download already exists: " + fileName);
            }

            // Each attempt owns a unique temporary file. Never delete someone else's file.
            tempFile = Files.createTempFile(normalizedBase, "peershare-", ".part");

            client.download(peer, fileName, tempFile, expectedSize);

            // Move within the same folder; the default move refuses an existing target.
            Files.move(tempFile, destination);
            tempFile = null;
            published = true;

            // Record successful download in SQLite
            recordHistory(fileName, peer.name(), "SUCCESS");

            return destination;

        } catch (Exception e) {
            // Clean up any incomplete .part file
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException cleanupEx) {
                    LOGGER.log(Level.WARNING, "Failed to clean up temp file", cleanupEx);
                }
            }

            // A published file remains a successful transfer even if history storage fails.
            if (!published) {
                try {
                    recordHistory(fileName, peer == null ? "Unknown" : peer.name(), "FAILED");
                } catch (IOException historyError) {
                    e.addSuppressed(historyError);
                }
            }

            if (e instanceof IOException ioe) {
                throw ioe;
            } else if (e instanceof RuntimeException re) {
                throw re;
            } else {
                throw new IOException("Download failed: " + e.getMessage(), e);
            }
        }
    }

    private void recordHistory(String fileName, String peerName, String status) throws IOException {
        try {
            historyDao.create(new DownloadHistory(0, fileName, peerName, LocalDateTime.now(), status));
        } catch (SQLException e) {
            throw new IOException("Transfer finished, but history could not be saved: " + e.getMessage(), e);
        }
    }
}
