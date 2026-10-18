package com.peershare;

import com.peershare.dao.DownloadHistoryDao;
import com.peershare.db.DatabaseManager;
import com.peershare.model.DownloadHistory;
import com.peershare.model.Peer;
import com.peershare.network.FileTransferClient;
import com.peershare.network.PeerServer;
import com.peershare.service.TransferService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class TransferServiceTest {
    private TransferServiceTest() {
    }

    public static void run() throws Exception {
        testSuccessfulDownload();
        testUnsafeFileNameDownloadFailure();
        testMissingFileDownloadFailure();
    }

    private static void testSuccessfulDownload() throws Exception {
        Path tempSharedDir = Files.createTempDirectory("peershare-transfer-shared");
        Path tempDownloadsDir = Files.createTempDirectory("peershare-transfer-downloads");
        Path tempDbDir = Files.createTempDirectory("peershare-transfer-db");

        String fileContent = "This is a small test file for PeerShare download testing.";
        Files.writeString(tempSharedDir.resolve("sample.txt"), fileContent);

        DatabaseManager dbManager = new DatabaseManager(tempDbDir.resolve("peershare.db"));
        dbManager.initialize();
        DownloadHistoryDao historyDao = new DownloadHistoryDao(dbManager);

        PeerServer server = new PeerServer(0, tempSharedDir);
        server.start();

        try {
            Peer peer = new Peer(1, "Alice", "127.0.0.1", server.port());
            FileTransferClient client = new FileTransferClient();
            TransferService service = new TransferService(tempDownloadsDir, historyDao, client);

            Path downloaded = service.download(peer, "sample.txt");

            TestSupport.assertTrue(Files.exists(downloaded), "Downloaded file must exist in downloads folder.");
            TestSupport.assertEquals(fileContent, Files.readString(downloaded), "Content must match original file.");
            TestSupport.assertTrue(!Files.exists(tempDownloadsDir.resolve("sample.txt.part")),
                    "Temporary .part file must be cleaned up after successful download.");

            List<DownloadHistory> history = historyDao.findAll();
            TestSupport.assertEquals(1, history.size(), "One history record must be saved.");
            DownloadHistory record = history.getFirst();
            TestSupport.assertEquals("sample.txt", record.fileName(), "History must record file name.");
            TestSupport.assertEquals("Alice", record.peerName(), "History must record peer name.");
            TestSupport.assertEquals("SUCCESS", record.status(), "History status must be SUCCESS.");
            TestSupport.assertTrue(record.downloadedAt() != null, "downloadedAt must not be null.");
        } finally {
            server.stop();
        }
    }

    private static void testUnsafeFileNameDownloadFailure() throws Exception {
        Path tempSharedDir = Files.createTempDirectory("peershare-unsafe-shared");
        Path tempDownloadsDir = Files.createTempDirectory("peershare-unsafe-downloads");
        Path tempDbDir = Files.createTempDirectory("peershare-unsafe-db");

        DatabaseManager dbManager = new DatabaseManager(tempDbDir.resolve("peershare.db"));
        dbManager.initialize();
        DownloadHistoryDao historyDao = new DownloadHistoryDao(dbManager);

        PeerServer server = new PeerServer(0, tempSharedDir);
        server.start();

        try {
            Peer peer = new Peer(2, "Bob", "127.0.0.1", server.port());
            FileTransferClient client = new FileTransferClient();
            TransferService service = new TransferService(tempDownloadsDir, historyDao, client);

            boolean threwException = false;
            try {
                service.download(peer, "../secret.txt");
            } catch (Exception expected) {
                threwException = true;
            }

            TestSupport.assertTrue(threwException, "Unsafe filename download must throw an exception.");
            TestSupport.assertTrue(!Files.exists(tempDownloadsDir.resolve("secret.txt")),
                    "Destination file must not be created for unsafe request.");

            List<DownloadHistory> history = historyDao.findAll();
            TestSupport.assertEquals(1, history.size(), "Failed download must create one history entry.");
            TestSupport.assertEquals("FAILED", history.getFirst().status(),
                    "Failed download history status must be FAILED.");
        } finally {
            server.stop();
        }
    }

    private static void testMissingFileDownloadFailure() throws Exception {
        Path tempSharedDir = Files.createTempDirectory("peershare-missing-shared");
        Path tempDownloadsDir = Files.createTempDirectory("peershare-missing-downloads");
        Path tempDbDir = Files.createTempDirectory("peershare-missing-db");

        DatabaseManager dbManager = new DatabaseManager(tempDbDir.resolve("peershare.db"));
        dbManager.initialize();
        DownloadHistoryDao historyDao = new DownloadHistoryDao(dbManager);

        PeerServer server = new PeerServer(0, tempSharedDir);
        server.start();

        try {
            Peer peer = new Peer(3, "Charlie", "127.0.0.1", server.port());
            FileTransferClient client = new FileTransferClient();
            TransferService service = new TransferService(tempDownloadsDir, historyDao, client);

            boolean threwException = false;
            try {
                service.download(peer, "missing_file.txt");
            } catch (Exception expected) {
                threwException = true;
            }

            TestSupport.assertTrue(threwException, "Missing file download must throw an exception.");
            TestSupport.assertTrue(!Files.exists(tempDownloadsDir.resolve("missing_file.txt")),
                    "No destination file should exist for missing remote file.");

            List<DownloadHistory> history = historyDao.findAll();
            TestSupport.assertEquals(1, history.size(), "History row must be saved on missing file failure.");
            TestSupport.assertEquals("FAILED", history.getFirst().status(),
                    "History status must be FAILED for missing file.");
        } finally {
            server.stop();
        }
    }
}
