package com.peershare;

import com.peershare.config.AppConfig;
import com.peershare.dao.DownloadHistoryDao;
import com.peershare.dao.PeerDao;
import com.peershare.dao.SharedFileDao;
import com.peershare.db.DatabaseManager;
import com.peershare.network.FileTransferClient;
import com.peershare.network.PeerServer;
import com.peershare.service.FileScanner;
import com.peershare.service.TransferService;
import com.peershare.ui.MainView;
import com.peershare.util.AppLogger;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MainApp is the main JavaFX application lifecycle coordinator.
 * It loads configuration, initializes the SQLite database, starts the background
 * TCP server, and constructs the user interface.
 */
public final class MainApp extends Application {
    private static final Logger LOGGER = AppLogger.create();

    private AppConfig config;
    private DatabaseManager databaseManager;
    private PeerServer peerServer;

    @Override
    public void init() throws Exception {
        LOGGER.info("Starting PeerShare initialization...");

        // 1. Load application configuration
        config = AppConfig.load(Path.of("."));

        // 2. Initialize SQLite database schema
        databaseManager = new DatabaseManager(config);
        databaseManager.initialize();
        LOGGER.info("SQLite database initialized at: " + config.databasePath());

        // 3. Start background TCP server
        peerServer = new PeerServer(config);
        peerServer.start();
        LOGGER.info("PeerServer listening on port: " + peerServer.port());
    }

    @Override
    public void start(Stage primaryStage) {
        // Instantiate DAOs
        PeerDao peerDao = new PeerDao(databaseManager);
        SharedFileDao sharedFileDao = new SharedFileDao(databaseManager);
        DownloadHistoryDao historyDao = new DownloadHistoryDao(databaseManager);

        // Instantiate Services
        FileScanner fileScanner = new FileScanner(config);
        FileTransferClient client = new FileTransferClient();
        TransferService transferService = new TransferService(config, historyDao, client);

        // Build User Interface
        MainView mainView = new MainView(
                config,
                peerDao,
                sharedFileDao,
                historyDao,
                fileScanner,
                transferService,
                client,
                peerServer
        );

        Scene scene = new Scene(mainView, 850, 600);

        primaryStage.setTitle("PeerShare — P2P File Sharing Application");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(650);
        primaryStage.setMinHeight(450);
        primaryStage.show();

        LOGGER.info("PeerShare GUI launched successfully.");
    }

    @Override
    public void stop() {
        LOGGER.info("Shutting down PeerShare...");
        if (peerServer != null) {
            peerServer.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
