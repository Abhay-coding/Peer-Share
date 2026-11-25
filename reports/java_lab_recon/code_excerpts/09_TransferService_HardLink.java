// Source: src\main\java\com\peershare\service\TransferService.java (lines 72-146)
// Commit: 230bb5fb750812f7d1f19a3fc5586932f99e58ed
// Concept: Atomic Hard-Link Publication, .part Cleanup & addSuppressed

    public Path download(Peer peer, String fileName, long expectedSize, BooleanSupplier cancelled) throws IOException {
        Path tempFile = null;
        boolean published = false;
        Path normalizedBase = downloadsDirectory.toAbsolutePath().normalize();

        try {
            if (peer == null || !InputValidator.isSafeFileName(fileName)) {
                throw new IOException("Unsafe or invalid download request.");
            }
            Files.createDirectories(normalizedBase);
            Path destination = normalizedBase.resolve(fileName).normalize();
            if (!destination.startsWith(normalizedBase) || Files.exists(destination)) {
                throw new IOException("Download already exists: " + fileName);
            }

            // Each attempt owns a unique temporary file. Never delete someone else's file.
            tempFile = Files.createTempFile(normalizedBase, "peershare-", ".part");

            // Verify temp file is a regular file (not a symlink) to prevent symlink attacks
            if (!Files.isRegularFile(tempFile)) {
                Files.deleteIfExists(tempFile);
                throw new IOException("Temporary file is not a regular file.");
            }

            client.download(peer, fileName, tempFile, expectedSize, cancelled);
            if (cancelled != null && cancelled.getAsBoolean()) {
                throw new CancellationException("Download cancelled.");
            }

            // Create the final name in one operation. Another download may have claimed it.
            try {
                Files.createLink(destination, tempFile);
            } catch (FileAlreadyExistsException ex) {
                throw new IOException("Download already exists: " + fileName, ex);
            } catch (UnsupportedOperationException ex) {
                throw new IOException("The downloads folder must support hard links.", ex);
            }
            published = true;

            // Record successful download in SQLite
            try {
                recordHistory(fileName, peer.name(), "SUCCESS");
            } catch (SQLException ex) {
                throw new HistoryWriteException(destination, ex);
            }

            return destination;

        } catch (Exception e) {
            // A published file remains a successful transfer even if history storage fails.
            if (!published) {
                try {
                    recordHistory(fileName, peer == null ? "Unknown" : peer.name(), "FAILED");
                } catch (SQLException historyError) {
                    e.addSuppressed(historyError);
                }
            }

            if (e instanceof IOException ioe) {
                throw ioe;
            } else if (e instanceof RuntimeException re) {
                throw re;
            } else {
                throw new IOException("Download failed: " + e.getMessage(), e);
            }
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException cleanupEx) {
                    LOGGER.log(Level.WARNING, "Failed to clean up temp file", cleanupEx);
                }
            }
        }
    }
