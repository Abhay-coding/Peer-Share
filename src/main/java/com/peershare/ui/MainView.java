package com.peershare.ui;

import com.peershare.config.AppConfig;
import com.peershare.dao.DownloadHistoryDao;
import com.peershare.dao.PeerDao;
import com.peershare.dao.SharedFileDao;
import com.peershare.network.FileTransferClient;
import com.peershare.network.PeerServer;
import com.peershare.service.FileScanner;
import com.peershare.service.TransferService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

/**
 * MainView is the primary container for PeerShare.
 * It houses the application header and the four main functional tabs.
 */
public final class MainView extends BorderPane {
    private final PeersTab peersTab;
    private final SharedFilesTab sharedFilesTab;
    private final DownloadTab downloadTab;
    private final HistoryTab historyTab;

    public MainView(AppConfig config,
                    PeerDao peerDao,
                    SharedFileDao sharedFileDao,
                    DownloadHistoryDao historyDao,
                    FileScanner fileScanner,
                    TransferService transferService,
                    FileTransferClient client,
                    PeerServer peerServer) {

        // Header Section
        HBox header = new HBox(20);
        header.setPadding(new Insets(12, 16, 12, 16));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #2c3e50;");

        Label titleLabel = new Label("PeerShare");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label serverInfo = new Label(String.format("Listening on Port: %d  |  Shared: %s  |  Downloads: %s",
                peerServer.port(), config.sharedDirectory().getFileName(), config.downloadDirectory().getFileName()));
        serverInfo.setStyle("-fx-font-size: 13px; -fx-text-fill: #ecf0f1;");

        header.getChildren().addAll(titleLabel, serverInfo);
        setTop(header);

        // TabPane Section
        TabPane tabPane = new TabPane();

        peersTab = new PeersTab(peerDao);
        sharedFilesTab = new SharedFilesTab(fileScanner, sharedFileDao);
        downloadTab = new DownloadTab(peerDao, client, transferService);
        historyTab = new HistoryTab(historyDao);

        tabPane.getTabs().addAll(peersTab, sharedFilesTab, downloadTab, historyTab);

        // Auto-refresh when tabs become active
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == downloadTab) {
                downloadTab.refreshPeerSelector();
            } else if (newTab == historyTab) {
                historyTab.refreshHistory();
            } else if (newTab == peersTab) {
                peersTab.refreshPeers();
            }
        });

        setCenter(tabPane);
    }
}
