package com.peershare;

import com.peershare.dao.SharedFileDao;
import com.peershare.db.DatabaseManager;
import com.peershare.model.SharedFile;
import com.peershare.service.FileScanner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class FileScannerTest {
    private FileScannerTest() {
    }

    public static void run() throws Exception {
        testScanningDirectFilesOnly();
        testScanAndStoreSynchronization();
    }

    private static void testScanningDirectFilesOnly() throws Exception {
        Path tempFolder = Files.createTempDirectory("peershare-scan-test");

        // Create two regular files
        Path file1 = tempFolder.resolve("file1.txt");
        Files.writeString(file1, "Hello from file 1");

        Path file2 = tempFolder.resolve("file2.txt");
        Files.writeString(file2, "Sample content for file 2");

        // Create a nested subdirectory with a file inside it
        Path subFolder = tempFolder.resolve("subfolder");
        Files.createDirectory(subFolder);
        Files.writeString(subFolder.resolve("ignored.txt"), "Should be ignored");

        FileScanner scanner = new FileScanner(tempFolder);
        List<SharedFile> files = scanner.scan();

        TestSupport.assertEquals(2, files.size(), "Scanner must only return direct regular files.");

        // Check file names
        List<String> fileNames = files.stream().map(SharedFile::fileName).toList();
        TestSupport.assertTrue(fileNames.contains("file1.txt"), "Result must contain file1.txt.");
        TestSupport.assertTrue(fileNames.contains("file2.txt"), "Result must contain file2.txt.");
        TestSupport.assertTrue(!fileNames.contains("subfolder"), "Subfolders must not be listed as files.");
        TestSupport.assertTrue(!fileNames.contains("ignored.txt"), "Nested files must not be listed.");

        // Check file sizes
        for (SharedFile f : files) {
            TestSupport.assertTrue(f.fileSize() > 0, "File size must be greater than 0.");
            TestSupport.assertTrue(f.lastModified() != null, "Last modified date must not be null.");
        }
    }

    private static void testScanAndStoreSynchronization() throws Exception {
        Path tempSharedFolder = Files.createTempDirectory("peershare-sync-shared");
        Path tempDbFolder = Files.createTempDirectory("peershare-sync-db");

        DatabaseManager dbManager = new DatabaseManager(tempDbFolder.resolve("peershare.db"));
        dbManager.initialize();
        SharedFileDao dao = new SharedFileDao(dbManager);

        // Put two files in shared folder
        Path fileA = tempSharedFolder.resolve("docA.txt");
        Files.writeString(fileA, "Document A");

        Path fileB = tempSharedFolder.resolve("docB.txt");
        Files.writeString(fileB, "Document B");

        FileScanner scanner = new FileScanner(tempSharedFolder);
        scanner.scanAndStore(dao);

        TestSupport.assertEquals(2, dao.findAll().size(), "Both files should be saved to database.");

        // Delete one file from the shared folder
        Files.delete(fileA);

        // Scan and store again
        scanner.scanAndStore(dao);

        List<SharedFile> remainingFiles = dao.findAll();
        TestSupport.assertEquals(1, remainingFiles.size(), "Database must update to reflect deleted file.");
        TestSupport.assertEquals("docB.txt", remainingFiles.getFirst().fileName(), "Only docB.txt should remain.");
    }
}
