package com.peershare;

import com.peershare.config.AppConfig;

import java.nio.file.Files;
import java.nio.file.Path;

public final class AppConfigTest {
    private AppConfigTest() {
    }

    public static void run() throws Exception {
        Path projectRoot = Files.createTempDirectory("peershare-config-test");
        Files.writeString(projectRoot.resolve("config.properties"), String.join(System.lineSeparator(),
                "server.port=5001",
                "shared.directory=shared_files",
                "downloads.directory=downloads"));

        AppConfig config = AppConfig.load(projectRoot);

        TestSupport.assertEquals(5001, config.serverPort(), "The configured port must be read.");
        TestSupport.assertEquals(projectRoot.resolve("shared_files"), config.sharedDirectory(),
                "Shared files must stay below the project folder.");
        TestSupport.assertEquals(projectRoot.resolve("downloads"), config.downloadDirectory(),
                "Downloads must stay below the project folder.");
        TestSupport.assertEquals(projectRoot.resolve("peershare.db"), config.databasePath(),
                "The database must stay below the project folder.");
        TestSupport.assertTrue(Files.isDirectory(config.sharedDirectory()), "The shared folder must be created.");
        TestSupport.assertTrue(Files.isDirectory(config.downloadDirectory()), "The download folder must be created.");
    }
}
