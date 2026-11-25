// Source: src\main\java\com\peershare\config\AppConfig.java (lines 53-72)
// Commit: 230bb5fb750812f7d1f19a3fc5586932f99e58ed
// Concept: NIO Path Normalization & Symlink Containment

    private static Path resolveInsideRoot(Path root, String configuredDirectory) throws IOException {
        if (configuredDirectory == null || configuredDirectory.isBlank()) {
            throw new IllegalArgumentException("Set both shared.directory and downloads.directory.");
        }
        Path directory = root.resolve(configuredDirectory).normalize();
        if (!directory.startsWith(root) || directory.equals(root)) {
            throw new IllegalArgumentException("Configured folders must be subfolders of the project folder.");
        }

        // Resolve existing links before creating any missing folders.
        Path existing = directory;
        while (!Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            existing = existing.getParent();
        }
        Path resolved = existing.toRealPath().resolve(existing.relativize(directory));
        if (!resolved.startsWith(root) || resolved.equals(root)) {
            throw new IllegalArgumentException("Configured folders must resolve to subfolders of the project folder.");
        }
        return resolved;
    }
