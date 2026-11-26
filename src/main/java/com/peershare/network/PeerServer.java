package com.peershare.network;

import com.peershare.config.AppConfig;
import com.peershare.service.FileScanner;
import com.peershare.util.AppLogger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PeerServer runs in the background, listening for incoming TCP connections
 * from other peers and dispatching each connection to a ClientHandler thread.
 */
public final class PeerServer {
    private static final Logger LOGGER = AppLogger.create();
    public static final int MAX_CLIENTS = 10;
    private static final int MAX_CONNECTIONS_PER_PEER = 2;

    private final int configuredPort;
    private final String bindAddress;
    private final Path sharedDirectory;
    private final FileScanner fileScanner;
    private final SSLContext tls;

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private final Set<Socket> clients = ConcurrentHashMap.newKeySet();
    private final Semaphore clientSlots = new Semaphore(MAX_CLIENTS);
    private final ConcurrentHashMap<InetAddress, Integer> connectionsByPeer = new ConcurrentHashMap<>();
    private volatile boolean running = false;

    public PeerServer(int port, Path sharedDirectory) {
        this(port, sharedDirectory, "127.0.0.1");
    }

    public PeerServer(int port, Path sharedDirectory, String bindAddress) {
        this(port, sharedDirectory, bindAddress, null);
    }

    public PeerServer(int port, Path sharedDirectory, String bindAddress, SSLContext tls) {
        this.configuredPort = port;
        this.sharedDirectory = sharedDirectory;
        this.bindAddress = bindAddress;
        this.fileScanner = new FileScanner(sharedDirectory);
        this.tls = tls;
    }

    public PeerServer(AppConfig config) {
        this(config.serverPort(), config.sharedDirectory(), config.bindAddress());
    }

    public PeerServer(AppConfig config, SSLContext tls) {
        this(config.serverPort(), config.sharedDirectory(), config.bindAddress(), tls);
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
        InetAddress address = InetAddress.getByName(bindAddress);
        if (tls == null && !address.isLoopbackAddress()) {
            throw new IOException("Set up trusted LAN certificates before enabling network sharing.");
        }
        ServerSocket listener = tls == null ? new ServerSocket() : tls.getServerSocketFactory().createServerSocket();
        try {
            if (listener instanceof SSLServerSocket secure) {
                secure.setEnabledProtocols(new String[]{"TLSv1.3"});
                secure.setNeedClientAuth(true);
            }
            listener.bind(new InetSocketAddress(address, configuredPort), 50);
        } catch (IOException | RuntimeException ex) {
            try {
                listener.close();
            } catch (IOException closeError) {
                ex.addSuppressed(closeError);
            }
            throw ex;
        }
        serverSocket = listener;
        running = true;

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
                try {
                    clientSocket.setSoTimeout(5000);
                } catch (IOException e) {
                    clientSocket.close();
                    throw e;
                }
                if (!clientSlots.tryAcquire()) {
                    clientSocket.close();
                    continue;
                }
                InetAddress remote = clientSocket.getInetAddress();
                if (tls != null && connectionsByPeer.merge(remote, 1, Integer::sum) > MAX_CONNECTIONS_PER_PEER) {
                    releasePeer(remote);
                    clientSlots.release();
                    clientSocket.close();
                    continue;
                }
                clients.add(clientSocket);
                ClientHandler handler = new ClientHandler(clientSocket, sharedDirectory, fileScanner);
                Thread handlerThread = new Thread(() -> {
                    try {
                        handler.run();
                    } finally {
                        clients.remove(clientSocket);
                        if (tls != null) {
                            releasePeer(remote);
                        }
                        clientSlots.release();
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

    private void releasePeer(InetAddress address) {
        connectionsByPeer.computeIfPresent(address, (key, count) -> count == 1 ? null : count - 1);
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

    public String bindAddress() {
        return bindAddress;
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
