package com.peershare.ui;

import com.peershare.config.AppConfig;
import com.peershare.dao.DownloadHistoryDao;
import com.peershare.dao.PeerDao;
import com.peershare.dao.SharedFileDao;
import com.peershare.model.Peer;
import com.peershare.network.FileTransferClient;
import com.peershare.network.PeerServer;
import com.peershare.service.FileScanner;
import com.peershare.service.TransferService;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

public final class MainView extends BorderPane {
    private final ToggleGroup navigation = new ToggleGroup();
    private final VBox sidebar = new VBox(6);
    private final BorderPane workspace = new BorderPane();
    private final StackPane content = new StackPane();
    private final Label title = ViewParts.label("", "page-title");
    private final Label description = ViewParts.label("", "secondary-text");

    public MainView(AppConfig config, PeerDao peerDao, SharedFileDao sharedFileDao,
                    DownloadHistoryDao historyDao, FileScanner fileScanner,
                    TransferService transferService, FileTransferClient client, PeerServer peerServer) {
        getStyleClass().add("app-shell");
        getStylesheets().add(MainView.class.getResource("peershare.css").toExternalForm());
        sidebar.getStyleClass().add("sidebar");

        HBox brand = new HBox(12, icon("M3 8h14m-4-4 4 4-4 4M17 16H3m4-4-4 4 4 4", "brand-icon"),
                ViewParts.label("PeerShare", "brand-name"));
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("brand");
        sidebar.getChildren().add(brand);

        VBox heading = new VBox(6, title, description);
        heading.getStyleClass().add("page-header");
        workspace.setTop(heading);
        workspace.setCenter(content);
        setLeft(sidebar);
        setCenter(workspace);

        SharedFilesTab shared = new SharedFilesTab(fileScanner, sharedFileDao);
        HistoryTab history = new HistoryTab(historyDao, config.downloadDirectory());
        DownloadTab download = new DownloadTab(peerDao, client, transferService, config.downloadDirectory(),
                () -> selectPage(1), () -> {
                    if (content.getChildren().contains(history.getContent())) {
                        history.refreshHistory();
                    }
                });
        PeersTab peers = new PeersTab(peerDao, peer -> browsePeer(download, peer));

        addPage(shared, "Choose which files others can download from this computer.",
                "M2 5h7l2 2h10v13H2z", shared::refreshFiles);
        addPage(peers, "Save the address and port of another computer running PeerShare.",
                "M3 3h14v11H3zM7 18h6M10 14v4M20 7h2v11h-5", peers::refreshPeers);
        addPage(download, "Browse a computer’s shared files and save a copy here.",
                "M12 2v12m-4-4 4 4 4-4M4 15v5h16v-5", download::refreshPeerSelector);
        addPage(history, "See what finished and which downloads need another try.",
                "M4 8a8 8 0 1 1 0 8M4 3v5h5m3-2v6l4 2", history::refreshHistory);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        String access = peerServer.bindAddress().equals("127.0.0.1") ? "This computer only" : "Local network";
        Label address = ViewParts.label(access, "network-detail");
        address.setWrapText(true);
        VBox network = new VBox(5, ViewParts.label("Sharing on port " + peerServer.port(), "network-title"), address);
        network.getStyleClass().add("network");
        sidebar.getChildren().addAll(spacer, network);
        selectPage(0);
    }

    private void addPage(Tab page, String help, String path, Runnable refresh) {
        ToggleButton button = new ToggleButton(page.getText(), icon(path, "nav-icon"));
        button.getStyleClass().add("nav-button");
        button.setToggleGroup(navigation);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> {
            button.setSelected(true);
            if (content.getChildren().contains(page.getContent())) {
                return;
            }
            button.requestFocus();
            title.setText(page.getText());
            description.setText(help);
            content.getChildren().setAll(page.getContent());
            refresh.run();
        });
        sidebar.getChildren().add(button);
    }

    private void selectPage(int index) {
        ((ToggleButton) navigation.getToggles().get(index)).fire();
    }

    private void browsePeer(DownloadTab download, Peer peer) {
        selectPage(2);
        download.browse(peer);
    }

    private static SVGPath icon(String path, String style) {
        SVGPath icon = new SVGPath();
        icon.setContent(path);
        icon.getStyleClass().add(style);
        return icon;
    }
}
