package com.peershare;

import com.peershare.model.Peer;
import com.peershare.model.SharedFile;
import com.peershare.network.FileTransferClient;
import com.peershare.network.PeerServer;
import com.peershare.network.Protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class NetworkTransferTest {
    private NetworkTransferTest() {
    }

    public static void run() throws Exception {
        testRemoteFileList();
        testUnavailablePeerConnectFailure();
        testInvalidListCount();
        testMalformedListRow();
        testExtraDownloadBytes();
        testTruncatedDownload();
        testWrongChecksum();
        testListedSizeChanged();
        testStopClosesConnectedClient();
        testConnectionLimit();
        testOversizedFileIsNotListed();
    }

    private static void testRemoteFileList() throws Exception {
        Path tempSharedDir = Files.createTempDirectory("peershare-net-shared");
        Files.writeString(tempSharedDir.resolve("sample1.txt"), "Content of file 1");
        Files.writeString(tempSharedDir.resolve("sample2.txt"), "Longer content for file 2");

        // Port 0 tells the OS to allocate an available free port
        PeerServer server = new PeerServer(0, tempSharedDir);
        server.start();

        try {
            int allocatedPort = server.port();
            TestSupport.assertTrue(allocatedPort > 0, "Server must have a valid allocated port.");

            Peer remotePeer = new Peer(1, "LocalPeer", "127.0.0.1", allocatedPort);
            FileTransferClient client = new FileTransferClient();

            List<SharedFile> remoteFiles = client.listRemoteFiles(remotePeer);

            TestSupport.assertEquals(2, remoteFiles.size(), "Remote list must return 2 files.");

            List<String> fileNames = remoteFiles.stream().map(SharedFile::fileName).toList();
            TestSupport.assertTrue(fileNames.contains("sample1.txt"), "sample1.txt must be in list.");
            TestSupport.assertTrue(fileNames.contains("sample2.txt"), "sample2.txt must be in list.");
        } finally {
            server.stop();
        }
    }

    private static void testUnavailablePeerConnectFailure() throws Exception {
        // Find an unused port by briefly opening and closing a ServerSocket
        int unusedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            unusedPort = socket.getLocalPort();
        }

        Peer unavailablePeer = new Peer(2, "OfflinePeer", "127.0.0.1", unusedPort);
        FileTransferClient client = new FileTransferClient();

        boolean failedAsExpected = false;
        try {
            client.listRemoteFiles(unavailablePeer);
        } catch (IOException expected) {
            failedAsExpected = true;
        }

        TestSupport.assertTrue(failedAsExpected,
                "Connecting to an unavailable peer must throw an IOException.");
    }

    private static void testInvalidListCount() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            Thread responder = new Thread(() -> respond(server, "OK -1\n"));
            responder.start();
            try {
                new FileTransferClient().listRemoteFiles(
                        new Peer(1, "BadPeer", "127.0.0.1", server.getLocalPort()));
                throw new AssertionError("Negative list count must be rejected.");
            } catch (IOException expected) {
                TestSupport.assertTrue(expected.getMessage().contains("Invalid file count"),
                        "Malformed count must produce a controlled protocol error.");
            }
            responder.join();
        }
    }

    private static void testExtraDownloadBytes() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            Thread responder = new Thread(() -> respond(server,
                    "OK 3 ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad\nabcdef"));
            responder.start();
            Path temporary = Files.createTempFile("peershare-extra-", ".part");
            try {
                new FileTransferClient().download(
                        new Peer(1, "BadPeer", "127.0.0.1", server.getLocalPort()),
                        "notes.txt", temporary, 3);
                throw new AssertionError("Extra bytes must be rejected.");
            } catch (IOException expected) {
                TestSupport.assertTrue(expected.getMessage().contains("more bytes"),
                        "Extra payload must produce a controlled error.");
            } finally {
                Files.deleteIfExists(temporary);
                responder.join();
            }
        }
    }

    private static void testMalformedListRow() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            Thread responder = new Thread(() -> respond(server, "OK 1\nfile.txt|not-a-size|2026-01-01T00:00:00\n"));
            responder.start();
            try {
                new FileTransferClient().listRemoteFiles(
                        new Peer(1, "BadPeer", "127.0.0.1", server.getLocalPort()));
                throw new AssertionError("Malformed file row must be rejected.");
            } catch (IOException expected) {
                TestSupport.assertTrue(expected.getMessage().contains("Invalid file size"),
                        "Malformed metadata must produce a controlled error.");
            } finally {
                responder.join();
            }
        }
    }

    private static void testTruncatedDownload() throws Exception {
        expectBadDownload("OK 6 " + "0".repeat(64) + "\nabc",
                6, "Connection interrupted");
    }

    private static void testWrongChecksum() throws Exception {
        expectBadDownload("OK 3 " + "0".repeat(64) + "\nabc", 3, "checksum does not match");
    }

    private static void testListedSizeChanged() throws Exception {
        expectBadDownload("OK 3 ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad\nabc",
                4, "File size changed");
    }

    private static void expectBadDownload(String response, long listedSize, String errorText) throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            Thread responder = new Thread(() -> respond(server, response));
            responder.start();
            Path temporary = Files.createTempFile("peershare-bad-", ".part");
            try {
                new FileTransferClient().download(
                        new Peer(1, "BadPeer", "127.0.0.1", server.getLocalPort()),
                        "notes.txt", temporary, listedSize);
                throw new AssertionError("Bad download must be rejected: " + errorText);
            } catch (IOException expected) {
                TestSupport.assertTrue(expected.getMessage().contains(errorText),
                        "Expected controlled error containing: " + errorText + ", actual: " + expected.getMessage());
            } finally {
                Files.deleteIfExists(temporary);
                responder.join();
            }
        }
    }

    private static void testStopClosesConnectedClient() throws Exception {
        Path shared = Files.createTempDirectory("peershare-stop-shared");
        PeerServer server = new PeerServer(0, shared);
        server.start();
        try (Socket idleClient = new Socket("127.0.0.1", server.port())) {
            idleClient.setSoTimeout(2000);
            server.stop();
            TestSupport.assertEquals(-1, idleClient.getInputStream().read(),
                    "Stopping the server should close an accepted client.");
            server.start();
            Peer peer = new Peer(1, "Restarted", "127.0.0.1", server.port());
            TestSupport.assertEquals(0, new FileTransferClient().listRemoteFiles(peer).size(),
                    "Server should answer after restart.");
        } finally {
            server.stop();
        }
    }

    private static void testConnectionLimit() throws Exception {
        PeerServer server = new PeerServer(0, Files.createTempDirectory("peershare-cap-shared"));
        server.start();
        List<Socket> held = new java.util.ArrayList<>();
        try {
            for (int i = 0; i < PeerServer.MAX_CLIENTS; i++) {
                held.add(new Socket("127.0.0.1", server.port()));
            }
            // Let the accept thread register the held connections before checking the next one.
            Thread.sleep(150);
            try (Socket extra = new Socket("127.0.0.1", server.port())) {
                extra.setSoTimeout(2000);
                TestSupport.assertEquals(-1, extra.getInputStream().read(),
                        "An extra client must be closed instead of starting another worker.");
            }
        } finally {
            for (Socket socket : held) socket.close();
            server.stop();
        }
    }

    private static void testOversizedFileIsNotListed() throws Exception {
        Path shared = Files.createTempDirectory("peershare-limit-shared");
        Path big = shared.resolve("too-big.bin");
        try (java.io.RandomAccessFile file = new java.io.RandomAccessFile(big.toFile(), "rw")) {
            file.setLength(Protocol.MAX_FILE_BYTES + 1);
        }
        Files.writeString(shared.resolve("small.txt"), "hello");
        PeerServer server = new PeerServer(0, shared);
        server.start();
        try {
            List<SharedFile> files = new FileTransferClient().listRemoteFiles(
                    new Peer(1, "Local", "127.0.0.1", server.port()));
            TestSupport.assertEquals(1, files.size(), "Oversized files should be omitted from remote lists.");
            TestSupport.assertEquals("small.txt", files.get(0).fileName(),
                    "A small file should remain available when another file is too large.");
        } finally {
            server.stop();
            Files.deleteIfExists(big);
        }
    }

    private static void respond(ServerSocket server, String response) {
        try (Socket socket = server.accept()) {
            Protocol.readLine(socket.getInputStream());
            socket.getOutputStream().write(response.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
