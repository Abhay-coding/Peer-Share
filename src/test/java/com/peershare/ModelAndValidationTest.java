package com.peershare;

import com.peershare.model.DownloadHistory;
import com.peershare.model.Peer;
import com.peershare.model.SharedFile;
import com.peershare.util.InputValidator;

import java.time.LocalDateTime;

public final class ModelAndValidationTest {
    private ModelAndValidationTest() {
    }

    public static void run() {
        storesPeerDetails();
        storesSharedFileDetails();
        storesDownloadHistoryDetails();
        acceptsValidPeerInput();
        rejectsBlankPeerName();
        rejectsPortOutsideValidRange();
        rejectsNonNumericPort();
        acceptsPlainFileName();
        rejectsUnsafeFileName();
    }

    private static void storesPeerDetails() {
        Peer peer = new Peer(7, "Rahul", "192.168.1.20", 5000);

        TestSupport.assertEquals("Rahul", peer.name(), "A peer must keep its name.");
        TestSupport.assertEquals(5000, peer.port(), "A peer must keep its port.");
    }

    private static void storesSharedFileDetails() {
        LocalDateTime changedAt = LocalDateTime.of(2026, 10, 18, 10, 30);
        SharedFile sharedFile = new SharedFile(3, "notes.pdf", 2048, changedAt);

        TestSupport.assertEquals("notes.pdf", sharedFile.fileName(), "A shared file must keep its filename.");
        TestSupport.assertEquals(2048L, sharedFile.fileSize(), "A shared file must keep its size.");
    }

    private static void storesDownloadHistoryDetails() {
        LocalDateTime downloadedAt = LocalDateTime.of(2026, 10, 18, 11, 0);
        DownloadHistory history = new DownloadHistory(5, "notes.pdf", "Rahul", downloadedAt, "SUCCESS");

        TestSupport.assertEquals("SUCCESS", history.status(), "History must keep the transfer result.");
        TestSupport.assertEquals(downloadedAt, history.downloadedAt(), "History must keep the download time.");
    }

    private static void acceptsValidPeerInput() {
        InputValidator.validatePeer("Rahul", "192.168.1.20", "5000");
    }

    private static void rejectsBlankPeerName() {
        TestSupport.expectIllegalArgument(
                () -> InputValidator.validatePeer("   ", "192.168.1.20", "5000"),
                "Peer name is required.");
    }

    private static void rejectsPortOutsideValidRange() {
        TestSupport.expectIllegalArgument(
                () -> InputValidator.validatePeer("Rahul", "192.168.1.20", "65536"),
                "Port must be between 1 and 65535.");
    }

    private static void rejectsNonNumericPort() {
        TestSupport.expectIllegalArgument(
                () -> InputValidator.validatePeer("Rahul", "192.168.1.20", "five thousand"),
                "Port must be a whole number.");
    }

    private static void acceptsPlainFileName() {
        TestSupport.assertTrue(InputValidator.isSafeFileName("notes.pdf"),
                "A plain filename inside shared_files must be accepted.");
    }

    private static void rejectsUnsafeFileName() {
        TestSupport.assertTrue(!InputValidator.isSafeFileName("../secret.txt"),
                "A parent-folder filename must be rejected.");
        TestSupport.assertTrue(!InputValidator.isSafeFileName("folder/notes.pdf"),
                "A filename with a folder separator must be rejected.");
        TestSupport.assertTrue(InputValidator.isSafeFileName("chapter..two.txt"),
                "A plain filename with two dots must be accepted.");
        TestSupport.assertTrue(!InputValidator.isSafeFileName("bad|name.txt"),
                "A protocol separator in a filename must be rejected.");
        TestSupport.assertTrue(!InputValidator.isSafeFileName("CON.txt"),
                "A reserved device name must be rejected.");
    }
}
