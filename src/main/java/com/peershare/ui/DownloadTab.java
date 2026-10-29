package com.peershare.ui;

import com.peershare.dao.PeerDao;
import com.peershare.model.Peer;
import com.peershare.model.SharedFile;
import com.peershare.network.FileTransferClient;
import com.peershare.service.TransferService;
import com.peershare.util.AppLogger;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DownloadTab allows the user to browse a remote peer's shared files and download them.
 */
public final class DownloadTab extends Tab {
    private static final Logger LOGGER = AppLogger.create();

    private final PeerDao peerDao;
    private final FileTransferClient client;
    private final TransferService transferService;

    private final ComboBox<Peer> peerSelector = new ComboBox<>();
    private final ObservableList<Peer> peerList = FXCollections.observableArrayList();
    private final ObservableList<SharedFile> remoteFileList = FXCollections.observableArrayList();
    private final TableView<SharedFile> remoteTable = new TableView<>();

    private final Button viewFilesButton = new Button("View Peer Files");
    private final Button downloadButton = new Button("Download Selected File");
    private final Button refreshPeersButton = new Button("Refresh Peers");

    private final ProgressIndicator progress = new ProgressIndicator();
    private final Label statusLabel = new Label("Select a peer and click 'View Peer Files'.");
    private Peer listedPeer;

    public DownloadTab(PeerDao peerDao, FileTransferClient client, TransferService transferService) {
        super("Download");
        this.peerDao = peerDao;
        this.client = client;
        this.transferService = transferService;
        setClosable(false);

        setContent(createContent());
        refreshPeerSelector();
    }

    private Pane createContent() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(15));

        progress.setVisible(false);
        progress.setPrefSize(20, 20);

        // Peer Selector Configuration
        peerSelector.setItems(peerList);
        peerSelector.valueProperty().addListener((obs, oldPeer, newPeer) -> {
            listedPeer = null;
            remoteFileList.clear();
            downloadButton.setDisable(true);
        });
        downloadButton.setDisable(true);
        peerSelector.setPromptText("Select a Peer...");
        peerSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(Peer peer) {
                if (peer == null) return "";
                return peer.name() + " (" + peer.ipAddress() + ":" + peer.port() + ")";
            }

            @Override
            public Peer fromString(String string) {
                return null;
            }
        });

        viewFilesButton.setOnAction(e -> handleViewRemoteFiles());
        refreshPeersButton.setOnAction(e -> refreshPeerSelector());
        downloadButton.setOnAction(e -> handleDownloadSelectedFile());

        HBox topBar = new HBox(10, new Label("Remote Peer:"), peerSelector, viewFilesButton, refreshPeersButton);
        topBar.setAlignment(Pos.CENTER_LEFT);

        setupTable();
        VBox.setVgrow(remoteTable, Priority.ALWAYS);

        HBox bottomBar = new HBox(12, downloadButton, progress, statusLabel);
        bottomBar.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(topBar, remoteTable, bottomBar);
        return root;
    }

    private void setupTable() {
        TableColumn<SharedFile, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().fileName()));
        nameCol.setPrefWidth(280);

        TableColumn<SharedFile, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(data -> new SimpleStringProperty(formatBytes(data.getValue().fileSize())));
        sizeCol.setPrefWidth(120);

        TableColumn<SharedFile, String> dateCol = new TableColumn<>("Last Modified");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().lastModified() != null
                        ? data.getValue().lastModified().toString().replace('T', ' ')
                        : ""));
        dateCol.setPrefWidth(200);

        remoteTable.getColumns().addAll(nameCol, sizeCol, dateCol);
        remoteTable.setItems(remoteFileList);
        remoteTable.setPlaceholder(new Label("No remote files listed. Select a peer and click 'View Peer Files'."));
    }

    public void refreshPeerSelector() {
        try {
            Peer current = peerSelector.getValue();
            peerList.setAll(peerDao.findAll());
            if (current != null) {
                for (Peer p : peerList) {
                    if (p.id() == current.id()) {
                        peerSelector.setValue(p);
                        break;
                    }
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load peers for dropdown", ex);
        }
    }

    private void handleViewRemoteFiles() {
        Peer peer = peerSelector.getValue();
        if (peer == null) {
            showAlert(Alert.AlertType.WARNING, "Peer Selection Required", "Please select a peer from the dropdown list.");
            return;
        }

        listedPeer = null;
        remoteFileList.clear();
        viewFilesButton.setDisable(true);
        downloadButton.setDisable(true);
        progress.setVisible(true);
        statusLabel.setText("Connecting to " + peer.name() + " (" + peer.ipAddress() + ":" + peer.port() + ")...");

        Task<List<SharedFile>> task = new Task<>() {
            @Override
            protected List<SharedFile> call() throws Exception {
                return client.listRemoteFiles(peer);
            }
        };

        task.setOnSucceeded(e -> {
            if (!peer.equals(peerSelector.getValue())) {
                viewFilesButton.setDisable(false);
                progress.setVisible(false);
                return;
            }
            remoteFileList.setAll(task.getValue());
            listedPeer = peer;
            statusLabel.setText("Found " + remoteFileList.size() + " file(s) on " + peer.name());
            viewFilesButton.setDisable(false);
            downloadButton.setDisable(remoteFileList.isEmpty());
            progress.setVisible(false);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            LOGGER.log(Level.WARNING, "Failed connecting to peer for file list", ex);
            statusLabel.setText("Connection failed: " + ex.getMessage());
            viewFilesButton.setDisable(false);
            downloadButton.setDisable(true);
            progress.setVisible(false);

            showAlert(Alert.AlertType.ERROR, "Connection Error",
                    "Could not retrieve files from peer " + peer.name() + ":\n" + ex.getMessage());
        });

        Thread thread = new Thread(task, "DownloadTab-ListFilesTask");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleDownloadSelectedFile() {
        Peer peer = peerSelector.getValue();
        SharedFile file = remoteTable.getSelectionModel().getSelectedItem();

        if (peer == null || !peer.equals(listedPeer)) {
            showAlert(Alert.AlertType.WARNING, "Peer Selection Required", "Please select a peer.");
            return;
        }

        if (file == null) {
            showAlert(Alert.AlertType.WARNING, "File Selection Required", "Please select a file to download.");
            return;
        }

        viewFilesButton.setDisable(true);
        downloadButton.setDisable(true);
        progress.setVisible(true);
        statusLabel.setText("Downloading " + file.fileName() + " from " + peer.name() + "...");

        Task<Path> task = new Task<>() {
            @Override
            protected Path call() throws Exception {
                return transferService.download(peer, file.fileName(), file.fileSize());
            }
        };

        task.setOnSucceeded(e -> {
            Path target = task.getValue();
            statusLabel.setText("Successfully downloaded: " + target.getFileName());
            viewFilesButton.setDisable(false);
            downloadButton.setDisable(!peer.equals(listedPeer));
            progress.setVisible(false);

            showAlert(Alert.AlertType.INFORMATION, "Download Complete",
                    "File '" + file.fileName() + "' was successfully downloaded to:\n" + target.toAbsolutePath());
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            LOGGER.log(Level.WARNING, "Download failed", ex);
            statusLabel.setText("Download failed: " + ex.getMessage());
            viewFilesButton.setDisable(false);
            downloadButton.setDisable(!peer.equals(listedPeer));
            progress.setVisible(false);

            showAlert(Alert.AlertType.ERROR, "Download Failed",
                    "Could not download '" + file.fileName() + "' from " + peer.name() + ":\n" + ex.getMessage());
        });

        Thread thread = new Thread(task, "DownloadTab-DownloadTask");
        // Let an in-progress transfer finish and write history before the JVM exits.
        thread.start();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }
}
