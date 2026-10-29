package com.peershare.network;

import com.peershare.config.AppConfig;
import com.peershare.service.FileScanner;
import com.peershare.util.AppLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PeerServer runs in the background, listening for incoming TCP connections
 * from other peers and dispatching each connection to a ClientHandler thread.
 */
public final class PeerServer {
    private static final Logger LOGGER = AppLogger.create();

    private final int configuredPort;
    private final Path sharedDirectory;
    private final FileScanner fileScanner;

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private final Set<Socket> clients = ConcurrentHashMap.newKeySet();
    private volatile boolean running = false;

    public PeerServer(int port, Path sharedDirectory) {
        this.configuredPort = port;
        this.sharedDirectory = sharedDirectory;
        this.fileScanner = new FileScanner(sharedDirectory);
    }

    public PeerServer(AppConfig config) {
        this(config.serverPort(), config.sharedDirectory());
    }

    /**
     * Starts the ServerSocket and launches the background accept loop.
     *
     * @throws IOException if the server cannot bind to the port
     */
    public synchronized void start() throws IOException {
        if (running) {
            return;
        }

        if (acceptThread != null && acceptThread.isAlive()) {
            throw new IOException("Previous server is still stopping.");
        }
        serverSocket = new ServerSocket(configuredPort);
        running = true;

        ServerSocket listener = serverSocket;
        acceptThread = new Thread(() -> listenForClients(listener), "PeerServer-AcceptThread");
        acceptThread.setDaemon(true);
        acceptThread.start();

        LOGGER.log(Level.INFO, "PeerServer started on port {0}", port());
    }

    private void listenForClients(ServerSocket listener) {
        while (running && !listener.isClosed()) {
            try {
                Socket clientSocket = listener.accept();
                if (!running) {
                    clientSocket.close();
                    break;
                }
                clientSocket.setSoTimeout(5000);
                clients.add(clientSocket);
                ClientHandler handler = new ClientHandler(clientSocket, sharedDirectory, fileScanner);
                Thread handlerThread = new Thread(() -> {
                    try {
                        handler.run();
                    } finally {
                        clients.remove(clientSocket);
                    }
                }, "PeerServer-ClientHandler");
                handlerThread.setDaemon(true);
                handlerThread.start();
            } catch (IOException e) {
                if (!running) {
                    break;
                }
                LOGGER.log(Level.WARNING, "Error accepting client connection: {0}", e.getMessage());
            }
        }
    }

    /**
     * Returns the actual port the server is listening on.
     * If started on port 0, this returns the dynamically assigned port.
     */
    public int port() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            return serverSocket.getLocalPort();
        }
        return configuredPort;
    }

    /**
     * Stops the server and closes the ServerSocket.
     */
    public synchronized void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error closing server socket: {0}", e.getMessage());
            }
        }
        for (Socket client : clients) {
            try {
                client.close();
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Error closing client socket", e);
            }
        }
        if (acceptThread != null) {
            try {
                acceptThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
