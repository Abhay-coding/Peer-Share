// Source: src\main\java\com\peershare\network\ClientHandler.java (lines 79-130)
// Commit: 230bb5fb750812f7d1f19a3fc5586932f99e58ed
// Concept: SHA-256 Digest & TOCTOU Re-check

    private void handleDownload(OutputStream out, String fileName) throws IOException {
        if (!FileScanner.isShareableName(fileName)) {
            Protocol.writeLine(out, Protocol.STATUS_ERROR + " Unsafe or invalid filename");
            return;
        }

        Path normalizedBase = sharedDirectory.toAbsolutePath().normalize();
        Path target = normalizedBase.resolve(fileName).normalize();
        if (!target.startsWith(normalizedBase) || Files.isSymbolicLink(target) || !Files.isRegularFile(target)) {
            Protocol.writeLine(out, Protocol.STATUS_ERROR + " File not found");
            return;
        }

        long fileSize = Files.size(target);
        var lastModified = Files.getLastModifiedTime(target);
        if (fileSize > Protocol.MAX_FILE_BYTES) {
            Protocol.writeLine(out, Protocol.STATUS_ERROR + " File is too large");
            return;
        }
        String checksum = checksum(target);
        Protocol.writeLine(out, Protocol.STATUS_OK + " " + fileSize + " " + checksum);

        // Send exactly the announced number of bytes; detect a shrinking source.
        try (InputStream fileIn = Files.newInputStream(target)) {
            byte[] buffer = new byte[8192];
            long sent = 0;
            while (sent < fileSize) {
                int read = fileIn.read(buffer, 0, (int) Math.min(buffer.length, fileSize - sent));
                if (read == -1) throw new IOException("Shared file changed during transfer.");
                out.write(buffer, 0, read);
                sent += read;
            }
        }
        if (Files.size(target) != fileSize || !Files.getLastModifiedTime(target).equals(lastModified)) {
            throw new IOException("Shared file changed during transfer.");
        }
        out.flush();
    }

    private static String checksum(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] bytes = new byte[8192];
                int read;
                while ((read = in.read(bytes)) != -1) digest.update(bytes, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
