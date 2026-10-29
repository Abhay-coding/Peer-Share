package com.peershare;

import com.peershare.dao.DownloadHistoryDao;
import com.peershare.dao.PeerDao;
import com.peershare.dao.SharedFileDao;
import com.peershare.db.DatabaseManager;
import com.peershare.model.DownloadHistory;
import com.peershare.model.Peer;
import com.peershare.model.SharedFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public final class DatabaseDaoTest {
    private DatabaseDaoTest() {
    }

    public static void run() throws Exception {
        Path databasePath = Files.createTempDirectory("peershare-db-test").resolve("peershare.db");
        DatabaseManager databaseManager = new DatabaseManager(databasePath);
        databaseManager.initialize();

        testsPeerCrud(databaseManager);
        testsSharedFileReplacement(databaseManager);
        testsDownloadHistory(databaseManager);
    }

    private static void testsPeerCrud(DatabaseManager databaseManager) throws Exception {
        PeerDao peerDao = new PeerDao(databaseManager);
        Peer created = peerDao.create(new Peer(0, "Rahul", "192.168.1.20", 5000));

        TestSupport.assertTrue(created.id() > 0, "Creating a peer must assign a database ID.");
        TestSupport.assertEquals(1, peerDao.findAll().size(), "The saved peer must be returned by SELECT.");

        Peer updated = new Peer(created.id(), "Rahul Sharma", "192.168.1.25", 5001);
        peerDao.update(updated);
        Peer stored = peerDao.findAll().get(0);
        TestSupport.assertEquals("Rahul Sharma", stored.name(), "UPDATE must save the new peer name.");
        TestSupport.assertEquals(5001, stored.port(), "UPDATE must save the new peer port.");

        peerDao.deleteById(created.id());
        TestSupport.assertEquals(0, peerDao.findAll().size(), "DELETE must remove the peer.");
    }

    private static void testsSharedFileReplacement(DatabaseManager databaseManager) throws Exception {
        SharedFileDao fileDao = new SharedFileDao(databaseManager);
        LocalDateTime changedAt = LocalDateTime.of(2026, 10, 18, 12, 0);
        fileDao.replaceAll(List.of(
                new SharedFile(0, "notes.pdf", 2048, changedAt),
                new SharedFile(0, "image.png", 4096, changedAt)));

        TestSupport.assertEquals(2, fileDao.findAll().size(), "The scan result must be stored in the database.");

        fileDao.replaceAll(List.of(new SharedFile(0, "notes.pdf", 2048, changedAt)));
        TestSupport.assertEquals(1, fileDao.findAll().size(), "Replacement must remove files no longer shared.");
    }

    private static void testsDownloadHistory(DatabaseManager databaseManager) throws Exception {
        DownloadHistoryDao historyDao = new DownloadHistoryDao(databaseManager);
        LocalDateTime downloadedAt = LocalDateTime.of(2026, 10, 18, 12, 15);
        DownloadHistory created = historyDao.create(
                new DownloadHistory(0, "notes.pdf", "Rahul", downloadedAt, "SUCCESS"));

        TestSupport.assertTrue(created.id() > 0, "Creating history must assign a database ID.");
        TestSupport.assertEquals("SUCCESS", historyDao.findAll().get(0).status(),
                "History SELECT must return the saved status.");

        historyDao.deleteById(created.id());
        TestSupport.assertEquals(0, historyDao.findAll().size(), "DELETE must remove the history entry.");
    }
}
