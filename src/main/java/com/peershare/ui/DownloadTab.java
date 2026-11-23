package com.peershare.ui;

import com.peershare.dao.PeerDao;
import com.peershare.model.Peer;
import com.peershare.model.SharedFile;
import com.peershare.network.FileTransferClient;
import com.peershare.service.TransferService;
import com.peershare.util.AppLogger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class DownloadTab extends Tab {
    private final PeerDao peerDao;
    private final FileTransferClient client;
    private final TransferService transferService;
    private final Runnable transferFinished;
    private final ComboBox<Peer> selector = new ComboBox<>();
    private final ObservableList<Peer> peers = FXCollections.observableArrayList();
    private final ObservableList<SharedFile> files = FXCollections.observableArrayList();
    private final TableView<SharedFile> table = new TableView<>();
    private final Button reload = ViewParts.button("Refresh files", "secondary-button");
    private final Button getFile = ViewParts.button("Download file", "primary-button");
    private final ProgressBar progress = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
    private final Label feedback = ViewParts.label("", "secondary-text");
    private final Label guidance = ViewParts.label("", "secondary-text");
    private Peer listedPeer;
    private boolean busy;
    private boolean refreshingPeers;

    public DownloadTab(PeerDao peerDao, FileTransferClient client, TransferService transferService,
                       Path downloadDirectory, Runnable addPeer, Runnable transferFinished) {
        super("Download");
        this.peerDao = peerDao;
        this.client = client;
        this.transferService = transferService;
        this.transferFinished = transferFinished;

        selector.setItems(peers);
        selector.setPromptText("Choose a saved peer");
        selector.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(selector, Priority.ALWAYS);
        selector.setConverter(new StringConverter<>() {
            @Override
            public String toString(Peer peer) {
                return peer == null ? "" : peer.name() + "  (" + peer.ipAddress() + ":" + peer.port() + ")";
            }

            @Override
            public Peer fromString(String text) {
                return null;
            }
        });
        selector.valueProperty().addListener((obs, oldPeer, peer) -> {
            if (!refreshingPeers && !Objects.equals(oldPeer, peer)) {
                if (peer == null) {
                    clearFiles();
                    updateActions();
                } else {
                    listFiles(peer);
                }
            }
        });

        reload.setOnAction(event -> listFiles(selector.getValue()));
        getFile.setOnAction(event -> downloadFile());
        table.getSelectionModel().selectedItemProperty().addListener((obs, previous, selected) -> updateActions());
        table.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !getFile.isDisabled()) {
                getFile.fire();
                event.consume();
            }
        });
        table.setRowFactory(view -> {
            TableRow<SharedFile> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty()) {
                    getFile.fire();
                }
            });
            return row;
        });

        SharedFilesTab.setUpFileTable(table);
        TextField search = ViewParts.searchable(table, files, "Search files", "", SharedFile::fileName);
        Label empty = (Label) table.getPlaceholder();
        empty.textProperty().unbind();
        empty.textProperty().bind(guidance.textProperty());

        Button editPeers = ViewParts.button("Manage peers", "secondary-button");
        editPeers.setOnAction(event -> addPeer.run());
        VBox source = new VBox(8, ViewParts.fieldLabel("Download from", selector), ViewParts.row(selector, editPeers));
        source.getStyleClass().add("form-panel");

        Button openFolder = ViewParts.folderButton("Open downloads", downloadDirectory, feedback);
        progress.setPrefWidth(95);
        progress.managedProperty().bind(progress.visibleProperty());
        feedback.setWrapText(true);
        setContent(ViewParts.page(source, ViewParts.row(search, ViewParts.spacer(), reload), table,
                ViewParts.row(getFile, openFolder, ViewParts.spacer(), progress), feedback));
        updateActions();
    }

    public void refreshPeerSelector() {
        if (busy) {
            return;
        }
        Peer previous = selector.getValue();
        try {
            List<Peer> saved = peerDao.findAll();
            if (peers.equals(saved)) {
                return;
            }
            Peer current = saved.stream().filter(peer -> previous != null && peer.id() == previous.id())
                    .findFirst().orElse(null);
            refreshingPeers = true;
            try {
                peers.setAll(saved);
                selector.setValue(current);
            } finally {
                refreshingPeers = false;
            }
            if (!Objects.equals(previous, current)) {
                clearFiles();
                if (current != null) {
                    listFiles(current);
                }
            }
        } catch (SQLException ex) {
            AppLogger.create().log(Level.WARNING, "Could not load peers", ex);
            showFeedback("Couldn’t load saved peers. Try opening Download again.", true);
        }
        updateActions();
    }

    public void browse(Peer peer) {
        if (busy || peer == null || !peers.contains(peer)) {
            return;
        }
        if (peer.equals(selector.getValue())) {
            listFiles(peer);
        } else {
            selector.setValue(peer);
        }
    }

    private void listFiles(Peer peer) {
        clearFiles();
        runTask("Looking for files on " + peer.name() + "…", "Couldn’t reach " + peer.name()
                        + ". Check that PeerShare is open there and that the saved address and port are correct.",
                () -> client.listRemoteFiles(peer), listed -> {
                    listedPeer = peer;
                    files.setAll(listed);
                    showFeedback(files.size() == 1 ? "1 file available" : files.size() + " files available", false);
                }, false);
    }

    private void downloadFile() {
        Peer peer = listedPeer;
        SharedFile file = table.getSelectionModel().getSelectedItem();
        runTask("Downloading " + file.fileName() + "…", "Couldn’t download " + file.fileName() + ". ",
                () -> transferService.download(peer, file.fileName(), file.fileSize()),
                path -> showFeedback("Saved " + path.getFileName() + " to downloads.", false), true);
    }

    private <T> void runTask(String activity, String failure, Callable<T> action,
                             Consumer<T> success, boolean download) {
        busy = true;
        showFeedback(activity, false);
        updateActions();
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return action.call();
            }
        };
        task.setOnSucceeded(event -> {
            busy = false;
            success.accept(task.getValue());
            updateActions();
            if (download) {
                transferFinished.run();
            }
        });
        task.setOnFailed(event -> {
            busy = false;
            Throwable error = task.getException();
            AppLogger.create().log(Level.WARNING, failure, error);
            showFeedback(download && error.getMessage() != null ? failure + error.getMessage() : failure.strip(), true);
            updateActions();
            if (download) {
                transferFinished.run();
            }
        });
        Thread worker = new Thread(task, download ? "DownloadTab-Transfer" : "DownloadTab-ListFiles");
        // Transfers finish writing their file and history before the app exits.
        worker.setDaemon(!download);
        worker.start();
    }

    private void clearFiles() {
        listedPeer = null;
        files.clear();
        showFeedback("", false);
    }

    private void updateActions() {
        boolean selected = selector.getValue() != null;
        selector.setDisable(busy || peers.isEmpty());
        reload.setDisable(busy || !selected);
        getFile.setDisable(busy || listedPeer == null || table.getSelectionModel().getSelectedItem() == null);
        progress.setVisible(busy);
        if (!files.isEmpty()) {
            guidance.setText("No matches. Try a different search.");
        } else if (listedPeer != null) {
            guidance.setText("This peer has no files available to download.");
        } else if (busy) {
            guidance.setText("Waiting for the file list…");
        } else if (selected) {
            guidance.setText("Use Refresh files to try again.");
        } else {
            guidance.setText(peers.isEmpty() ? "Add a computer under Manage peers to get started."
                    : "Choose a saved peer to see its files.");
        }
    }

    private void showFeedback(String text, boolean error) {
        feedback.getStyleClass().remove("error-text");
        if (error) {
            feedback.getStyleClass().add("error-text");
        }
        feedback.setText(text);
    }
}
