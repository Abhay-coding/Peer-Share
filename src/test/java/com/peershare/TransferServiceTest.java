package com.peershare;

import com.peershare.dao.DownloadHistoryDao;
import com.peershare.db.DatabaseManager;
import com.peershare.model.DownloadHistory;
import com.peershare.model.Peer;
import com.peershare.network.FileTransferClient;
import com.peershare.network.PeerServer;
import com.peershare.network.Protocol;
import com.peershare.service.TransferService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TransferServiceTest {
    private TransferServiceTest() {
    }

    public static void run() throws Exception {
        testSuccessfulDownload();
        testUnsafeFileNameDownloadFailure();
        testMissingFileDownloadFailure();
        testExistingDownloadIsPreserved();
        testFileCreatedDuringDownloadIsPreserved();
        testCancellationClosesSocketAndCleansUp();
        testRejectedNameCannotDeleteAnotherPartFile();
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
            DownloadHistory record = history.get(0);
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
            TestSupport.assertEquals("FAILED", history.get(0).status(),
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
            TestSupport.assertEquals("FAILED", history.get(0).status(),
                    "History status must be FAILED for missing file.");
        } finally {
            server.stop();
        }
    }

    private static void testExistingDownloadIsPreserved() throws Exception {
        Path shared = Files.createTempDirectory("peershare-existing-shared");
        Path downloads = Files.createTempDirectory("peershare-existing-downloads");
        Path db = Files.createTempDirectory("peershare-existing-db");
        Files.writeString(shared.resolve("same.txt"), "new content");
        Files.writeString(downloads.resolve("same.txt"), "keep this");
        DatabaseManager manager = new DatabaseManager(db.resolve("peershare.db"));
        manager.initialize();
        DownloadHistoryDao history = new DownloadHistoryDao(manager);
        TransferService service = new TransferService(downloads, history, new FileTransferClient());
        try {
            service.download(new Peer(1, "Alice", "127.0.0.1", 5000), "same.txt");
            throw new AssertionError("Existing download should be refused.");
        } catch (IOException expected) {
            TestSupport.assertEquals("keep this", Files.readString(downloads.resolve("same.txt")),
                    "Existing data must not be overwritten.");
        }
    }

    private static void testFileCreatedDuringDownloadIsPreserved() throws Exception {
        Path downloads = Files.createTempDirectory("peershare-conflicting-download");
        Path destination = downloads.resolve("same.txt");
        Path db = Files.createTempDirectory("peershare-conflicting-db");
        DatabaseManager manager = new DatabaseManager(db.resolve("peershare.db"));
        manager.initialize();
        DownloadHistoryDao history = new DownloadHistoryDao(manager);
        TransferService service = new TransferService(downloads, history, new FileTransferClient());
        byte[] bytes = "incoming file".getBytes(StandardCharsets.UTF_8);
        String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));

        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            server.setSoTimeout(5000);
            FutureTask<Void> reply = new FutureTask<>(() -> {
                try (var client = server.accept()) {
                    client.setSoTimeout(5000);
                    TestSupport.assertEquals("DOWNLOAD same.txt", Protocol.readLine(client.getInputStream()),
                            "The transfer must start before the destination is created.");
                    Files.writeString(destination, "keep this");
                    Protocol.writeLine(client.getOutputStream(), "OK " + bytes.length + " " + checksum);
                    client.getOutputStream().write(bytes);
                }
                return null;
            });
            new Thread(reply, "TransferServiceTest-Peer").start();
            Peer peer = new Peer(1, "Alice", server.getInetAddress().getHostAddress(), server.getLocalPort());
            try {
                service.download(peer, "same.txt", bytes.length);
                throw new AssertionError("A file created during download must be preserved.");
            } catch (IOException expected) {
                TestSupport.assertTrue(expected.getMessage().contains("Download already exists"),
                        "The transfer should report a destination conflict.");
            } finally {
                reply.get(6, TimeUnit.SECONDS);
            }
        }

        TestSupport.assertEquals("keep this", Files.readString(destination), "Concurrent file must not be overwritten.");
        try (var entries = Files.list(downloads)) {
            TestSupport.assertEquals(List.of(destination), entries.toList(), "The temporary download must be removed.");
        }
        TestSupport.assertEquals("FAILED", history.findAll().get(0).status(), "The conflict must be recorded as failed.");
    }

    private static void testCancellationClosesSocketAndCleansUp() throws Exception {
        Path downloads = Files.createTempDirectory("peershare-cancel-download");
        Path db = Files.createTempDirectory("peershare-cancel-db");
        DatabaseManager manager = new DatabaseManager(db.resolve("peershare.db"));
        manager.initialize();
        DownloadHistoryDao history = new DownloadHistoryDao(manager);
        TransferService service = new TransferService(downloads, history, new FileTransferClient());
        AtomicBoolean cancelled = new AtomicBoolean();

        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            server.setSoTimeout(5000);
            FutureTask<Void> reply = new FutureTask<>(() -> {
                try (var client = server.accept()) {
                    client.setSoTimeout(5000);
                    TestSupport.assertEquals("DOWNLOAD sample.txt", Protocol.readLine(client.getInputStream()),
                            "The download must be active before it is cancelled.");
                    cancelled.set(true);
                    TestSupport.assertEquals(-1, client.getInputStream().read(), "Cancellation must close the socket.");
                }
                return null;
            });
            new Thread(reply, "TransferServiceTest-CancelPeer").start();
            Peer peer = new Peer(1, "Alice", server.getInetAddress().getHostAddress(), server.getLocalPort());
            try {
                service.download(peer, "sample.txt", 1, cancelled::get);
                throw new AssertionError("The download should have been cancelled.");
            } catch (CancellationException expected) {
                TestSupport.assertEquals("Download cancelled.", expected.getMessage(), "Report cancellation separately.");
            } finally {
                reply.get(6, TimeUnit.SECONDS);
            }
        }

        try (var entries = Files.list(downloads)) {
            TestSupport.assertEquals(List.of(), entries.toList(), "Cancellation must remove the temporary download.");
        }
        TestSupport.assertEquals("FAILED", history.findAll().get(0).status(),
                "The cancelled attempt must retain its history record.");
    }

    private static void testRejectedNameCannotDeleteAnotherPartFile() throws Exception {
        Path downloads = Files.createTempDirectory("peershare-unsafe-parts");
        Path db = Files.createTempDirectory("peershare-unsafe-parts-db");
        Path unrelated = downloads.resolve("other.part");
        Files.writeString(unrelated, "keep this");
        DatabaseManager manager = new DatabaseManager(db.resolve("peershare.db"));
        manager.initialize();
        TransferService service = new TransferService(downloads, new DownloadHistoryDao(manager),
                new FileTransferClient());
        try {
            service.download(new Peer(1, "Alice", "127.0.0.1", 5000), "../other");
            throw new AssertionError("Unsafe name should be refused.");
        } catch (IOException expected) {
            TestSupport.assertEquals("keep this", Files.readString(unrelated),
                    "A rejected request must not delete an unrelated temporary file.");
        }
    }
}
