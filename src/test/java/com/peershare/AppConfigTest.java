package com.peershare;

import com.peershare.config.AppConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AppConfigTest {
    private AppConfigTest() {
    }

    public static void run() throws Exception {
        Path projectRoot = Files.createTempDirectory("peershare-config-test").toRealPath();
        Files.writeString(projectRoot.resolve("config.properties"), String.join(System.lineSeparator(),
                "server.port=5001",
                "shared.directory=shared_files",
                "downloads.directory=downloads"));

        AppConfig config = AppConfig.load(projectRoot);

        TestSupport.assertEquals(5001, config.serverPort(), "The configured port must be read.");
        TestSupport.assertEquals("127.0.0.1", config.bindAddress(),
                "The default server must accept only same-computer connections.");
        TestSupport.assertEquals(projectRoot.resolve("shared_files"), config.sharedDirectory(),
                "Shared files must stay below the project folder.");
        TestSupport.assertEquals(projectRoot.resolve("downloads"), config.downloadDirectory(),
                "Downloads must stay below the project folder.");
        TestSupport.assertEquals(projectRoot.resolve("peershare.db"), config.databasePath(),
                "The database must stay below the project folder.");
        TestSupport.assertTrue(Files.isDirectory(config.sharedDirectory()), "The shared folder must be created.");
        TestSupport.assertTrue(Files.isDirectory(config.downloadDirectory()), "The download folder must be created.");

        Files.writeString(projectRoot.resolve("config.properties"), String.join(System.lineSeparator(),
                "server.port=5001",
                "server.bindAddress=0.0.0.0",
                "shared.directory=shared_files",
                "downloads.directory=downloads"));
        TestSupport.assertEquals("0.0.0.0", AppConfig.load(projectRoot).bindAddress(),
                "LAN access must require an explicit setting.");

        Files.writeString(projectRoot.resolve("config.properties"), String.join(System.lineSeparator(),
                "server.port=5001",
                "server.bindAddress=example.com",
                "shared.directory=shared_files",
                "downloads.directory=downloads"));
        TestSupport.expectIllegalArgument(() -> {
            try {
                AppConfig.load(projectRoot);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }, "server.bindAddress must be 127.0.0.1 or 0.0.0.0.");

        String subfolders = "Configured folders must be subfolders of the project folder.";
        String separate = "Shared files and downloads must use separate, non-overlapping folders.";
        expectInvalidFolders(projectRoot, ".", "downloads", subfolders);
        expectInvalidFolders(projectRoot, "shared_files", ".", subfolders);
        expectInvalidFolders(projectRoot, "other", "other", separate);
        expectInvalidFolders(projectRoot, "other", "other/downloads", separate);
        expectInvalidFolders(projectRoot, "other/shared", "other", separate);
        TestSupport.assertTrue(!Files.exists(projectRoot.resolve("other")),
                "Invalid configurations must leave the filesystem unchanged.");
        testLinkedFolder(projectRoot);
    }

    private static void testLinkedFolder(Path projectRoot) throws IOException {
        Path alias = projectRoot.resolve("linked");
        Path outside = Files.createTempDirectory("peershare-config-outside");
        try {
            Files.createSymbolicLink(alias, outside);
        } catch (IOException | UnsupportedOperationException ex) {
            // Windows may require permission to create symlinks.
            return;
        }
        expectInvalidFolders(projectRoot, "linked/new", "downloads",
                "Configured folders must resolve to subfolders of the project folder.");
        TestSupport.assertTrue(!Files.exists(outside.resolve("new")), "Validation must not create folders outside the project.");
    }

    private static void expectInvalidFolders(Path root, String shared, String downloads, String message) throws IOException {
        Files.writeString(root.resolve("config.properties"), String.join(System.lineSeparator(),
                "server.port=5001", "shared.directory=" + shared, "downloads.directory=" + downloads));
        TestSupport.expectIllegalArgument(() -> {
            try {
                AppConfig.load(root);
            } catch (IOException ex) {
                throw new AssertionError(ex);
            }
        }, message);
    }
}
