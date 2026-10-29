package com.peershare.service;

import com.peershare.config.AppConfig;
import com.peershare.dao.SharedFileDao;
import com.peershare.model.SharedFile;
import com.peershare.util.InputValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * FileScanner is responsible for scanning the local shared_files folder.
 * It detects all direct files, reads their metadata (name, size, modification time),
 * and can synchronize that list into the local SQLite database via SharedFileDao.
 */
public final class FileScanner {
    private final Path sharedDirectory;

    public FileScanner(Path sharedDirectory) {
        this.sharedDirectory = sharedDirectory;
    }

    public FileScanner(AppConfig config) {
        this(config.sharedDirectory());
    }

    public Path sharedDirectory() {
        return sharedDirectory;
    }

    /**
     * Scans the shared folder for direct regular files.
     * Subdirectories and nested files are ignored for safety.
     *
     * @return a list of SharedFile objects representing the files currently in the folder
     * @throws IOException if the folder cannot be accessed
     */
    public List<SharedFile> scan() throws IOException {
        // If the shared folder does not exist yet, create it
        if (!Files.exists(sharedDirectory)) {
            Files.createDirectories(sharedDirectory);
            return List.of();
        }

        List<SharedFile> files = new ArrayList<>();

        // Use try-with-resources to ensure the directory stream is closed properly
        try (Stream<Path> stream = Files.list(sharedDirectory)) {
            List<Path> entries = stream.toList();

            for (Path entry : entries) {
                // Only include regular files; skip subdirectories
                if (!Files.isSymbolicLink(entry) && Files.isRegularFile(entry)
                        && InputValidator.isSafeFileName(entry.getFileName().toString())) {
                    String fileName = entry.getFileName().toString();
                    long fileSize = Files.size(entry);

                    // Convert file modification timestamp to LocalDateTime
                    FileTime fileTime = Files.getLastModifiedTime(entry);
                    LocalDateTime lastModified = LocalDateTime.ofInstant(
                            fileTime.toInstant(),
                            ZoneId.systemDefault()
                    );

                    // New scanned file has id 0 before it is saved into the database
                    files.add(new SharedFile(0, fileName, fileSize, lastModified));
                }
            }
        }

        // Sort files alphabetically by name so the table list looks neat and organized
        files.sort(Comparator.comparing(SharedFile::fileName));

        return files;
    }

    /**
     * Scans the shared folder and immediately updates the SQLite database.
     * Any files removed from the folder will also be removed from the database.
     *
     * @param dao the SharedFileDao used to persist the records
     * @return the synchronized list of SharedFile records
     * @throws IOException if reading the folder fails
     * @throws SQLException if writing to the database fails
     */
    public List<SharedFile> scanAndStore(SharedFileDao dao) throws IOException, SQLException {
        List<SharedFile> scannedFiles = scan();
        dao.replaceAll(scannedFiles);
        return scannedFiles;
    }
}
