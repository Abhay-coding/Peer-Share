// Source: src\main\java\com\peershare\network\PeerServer.java (lines 65-114)
// Commit: 230bb5fb750812f7d1f19a3fc5586932f99e58ed
// Concept: ServerSocket Accept Loop & Semaphore Bounding

        serverSocket = new ServerSocket(configuredPort, 50, InetAddress.getByName(bindAddress));
        serverSocket.setReuseAddress(true);
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
                clients.add(clientSocket);
                ClientHandler handler = new ClientHandler(clientSocket, sharedDirectory, fileScanner);
                Thread handlerThread = new Thread(() -> {
                    try {
                        handler.run();
                    } finally {
                        clients.remove(clientSocket);
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
