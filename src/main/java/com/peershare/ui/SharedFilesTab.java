package com.peershare.ui;

import com.peershare.dao.SharedFileDao;
import com.peershare.model.SharedFile;
import com.peershare.service.FileScanner;
import com.peershare.util.AppLogger;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SharedFilesTab displays the local files being shared and synchronizes them with SQLite.
 */
public final class SharedFilesTab extends Tab {
    private static final Logger LOGGER = AppLogger.create();

    private final FileScanner fileScanner;
    private final SharedFileDao sharedFileDao;

    private final ObservableList<SharedFile> fileList = FXCollections.observableArrayList();
    private final TableView<SharedFile> fileTable = new TableView<>();
    private final Label statusLabel = new Label("Ready");
    private final Button refreshButton = new Button("Refresh Files");
    private final ProgressIndicator progress = new ProgressIndicator();

    public SharedFilesTab(FileScanner fileScanner, SharedFileDao sharedFileDao) {
        super("Shared Files");
        this.fileScanner = fileScanner;
        this.sharedFileDao = sharedFileDao;
        setClosable(false);

        setContent(createContent());
        refreshFiles();
    }

    private Pane createContent() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(15));

        progress.setVisible(false);
        progress.setPrefSize(20, 20);

        refreshButton.setOnAction(e -> refreshFiles());

        Label folderInfo = new Label("Sharing folder: " + fileScanner.sharedDirectory().toAbsolutePath());
        folderInfo.setStyle("-fx-font-style: italic; -fx-text-fill: #555;");

        HBox topBar = new HBox(12, refreshButton, progress, statusLabel);
        topBar.setAlignment(Pos.CENTER_LEFT);

        setupTable();
        VBox.setVgrow(fileTable, Priority.ALWAYS);

        root.getChildren().addAll(folderInfo, topBar, fileTable);
        return root;
    }

    private void setupTable() {
        TableColumn<SharedFile, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().id()));
        idCol.setPrefWidth(60);

        TableColumn<SharedFile, String> nameCol = new TableColumn<>("File Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().fileName()));
        nameCol.setPrefWidth(260);

        TableColumn<SharedFile, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(data -> new SimpleStringProperty(formatBytes(data.getValue().fileSize())));
        sizeCol.setPrefWidth(120);

        TableColumn<SharedFile, String> dateCol = new TableColumn<>("Last Modified");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().lastModified().toString().replace('T', ' ')));
        dateCol.setPrefWidth(200);

        fileTable.getColumns().addAll(idCol, nameCol, sizeCol, dateCol);
        fileTable.setItems(fileList);
        fileTable.setPlaceholder(new Label("No files in shared_files. Place files there and click Refresh."));
    }

    public void refreshFiles() {
        refreshButton.setDisable(true);
        progress.setVisible(true);
        statusLabel.setText("Scanning shared folder...");

        Task<List<SharedFile>> task = new Task<>() {
            @Override
            protected List<SharedFile> call() throws Exception {
                return fileScanner.scanAndStore(sharedFileDao);
            }
        };

        task.setOnSucceeded(e -> {
            try {
                // Reload from SQLite so database IDs are displayed
                fileList.setAll(sharedFileDao.findAll());
                statusLabel.setText("Scanned " + fileList.size() + " file(s) successfully.");
            } catch (SQLException ex) {
                fileList.setAll(task.getValue());
                statusLabel.setText("Scanned " + fileList.size() + " file(s).");
            }
            refreshButton.setDisable(false);
            progress.setVisible(false);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            LOGGER.log(Level.SEVERE, "Failed scanning shared files", ex);
            statusLabel.setText("Scan failed: " + ex.getMessage());
            refreshButton.setDisable(false);
            progress.setVisible(false);

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Scan Error");
            alert.setHeaderText(null);
            alert.setContentText("Could not scan shared folder: " + ex.getMessage());
            alert.showAndWait();
        });

        Thread worker = new Thread(task, "SharedFiles-ScannerTask");
        worker.setDaemon(true);
        worker.start();
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
