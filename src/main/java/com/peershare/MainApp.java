package com.peershare;

import com.peershare.config.AppConfig;
import com.peershare.dao.DownloadHistoryDao;
import com.peershare.dao.PeerDao;
import com.peershare.dao.SharedFileDao;
import com.peershare.db.DatabaseManager;
import com.peershare.network.FileTransferClient;
import com.peershare.network.PeerServer;
import com.peershare.network.PeerTls;
import com.peershare.service.FileScanner;
import com.peershare.service.TransferService;
import com.peershare.ui.MainView;
import com.peershare.util.AppLogger;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.BindException;
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
    private SSLContext lanSecurity;
    private String sharingStatus = "";

    @Override
    public void init() throws Exception {
        LOGGER.info("Starting PeerShare initialization...");

        // 1. Load application configuration
        config = AppConfig.load(Path.of("."));

        // 2. Initialize SQLite database schema
        databaseManager = new DatabaseManager(config);
        databaseManager.initialize();
        LOGGER.info("SQLite database initialized at: " + config.databasePath());

        String password = System.getenv("PEERSHARE_TLS_PASSWORD");
        if (password != null && !password.isBlank()) {
            Path keys = config.projectRoot().resolve(".peershare-tls");
            try {
                lanSecurity = PeerTls.load(keys.resolve("identity.p12"), keys.resolve("trusted.p12"), password.toCharArray());
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "LAN certificates unavailable", ex);
            }
        }

        if (!config.sharingEnabled()) {
            sharingStatus = "Receive only";
        } else if (!config.bindAddress().equals("127.0.0.1") && lanSecurity == null) {
            sharingStatus = "Set up LAN certificates first.";
        } else {
            peerServer = new PeerServer(config, lanSecurity);
            try {
                peerServer.start();
            } catch (IOException ex) {
                sharingStatus = ex instanceof BindException ? "Port " + config.serverPort() + " is unavailable."
                        : "Couldn’t start the sharing server.";
                LOGGER.log(Level.WARNING, sharingStatus, ex);
                peerServer = null;
            }
        }
    }

    @Override
    public void start(Stage primaryStage) {
        // Instantiate DAOs
        PeerDao peerDao = new PeerDao(databaseManager);
        SharedFileDao sharedFileDao = new SharedFileDao(databaseManager);
        DownloadHistoryDao historyDao = new DownloadHistoryDao(databaseManager);

        // Instantiate Services
        FileScanner fileScanner = new FileScanner(config);
        FileTransferClient client = new FileTransferClient(lanSecurity);
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
                peerServer,
                sharingStatus
        );

        Scene scene = new Scene(mainView, 1040, 680);

        primaryStage.setTitle("PeerShare");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(760);
        primaryStage.setMinHeight(510);
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
