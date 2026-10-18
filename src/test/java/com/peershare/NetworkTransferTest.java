package com.peershare;

import com.peershare.model.Peer;
import com.peershare.model.SharedFile;
import com.peershare.network.FileTransferClient;
import com.peershare.network.PeerServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class NetworkTransferTest {
    private NetworkTransferTest() {
    }

    public static void run() throws Exception {
        testRemoteFileList();
        testUnavailablePeerConnectFailure();
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
}
