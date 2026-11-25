// Source: src\main\java\com\peershare\network\FileTransferClient.java (lines 164-211)
// Commit: 230bb5fb750812f7d1f19a3fc5586932f99e58ed
// Concept: 8 KB Buffered Streaming, SHA-256 & @FunctionalInterface

            // Stream bytes into temporary target and verify byte count
            byte[] buffer = new byte[8192];
            MessageDigest digest = sha256();
            long totalRead = 0;
            try (OutputStream fileOut = Files.newOutputStream(temporaryTarget)) {
                while (totalRead < expectedSize) {
                    long remaining = expectedSize - totalRead;
                    int toRead = (int) Math.min(buffer.length, remaining);
                    int bytesRead = in.read(buffer, 0, toRead);
                    if (bytesRead == -1) {
                        throw new IOException("Connection interrupted while streaming file. Expected "
                                + expectedSize + " bytes but only received " + totalRead);
                    }
                    fileOut.write(buffer, 0, bytesRead);
                    digest.update(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }
                fileOut.flush();
            }

            if (in.read() != -1) {
                throw new IOException("Peer sent more bytes than announced.");
            }
            if (!HexFormat.of().formatHex(digest.digest()).equals(metadata[1])) {
                throw new IOException("Downloaded file checksum does not match.");
            }

            return temporaryTarget;
        });
    }

    private <T> T request(Peer peer, BooleanSupplier cancelled, SocketRequest<T> action) throws IOException {
        try (Socket socket = new Socket();
             SocketDeadline deadline = new SocketDeadline(socket, requestTimeoutMs, cancelled)) {
            try {
                socket.connect(new InetSocketAddress(peer.ipAddress(), peer.port()), connectTimeoutMs);
                socket.setSoTimeout(readTimeoutMs);
                return action.run(socket.getInputStream(), socket.getOutputStream());
            } catch (IOException ex) {
                throw deadline.failure(ex);
            }
        }
    }

    @FunctionalInterface
    private interface SocketRequest<T> {
        T run(InputStream in, OutputStream out) throws IOException;
    }
