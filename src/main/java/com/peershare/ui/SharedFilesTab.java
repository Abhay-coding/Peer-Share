package com.peershare.ui;

import com.peershare.dao.SharedFileDao;
import com.peershare.model.SharedFile;
import com.peershare.service.FileScanner;
import com.peershare.util.AppLogger;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;

public final class SharedFilesTab extends Tab {
    private final FileScanner fileScanner;
    private final SharedFileDao sharedFileDao;
    private final ObservableList<SharedFile> files = FXCollections.observableArrayList();
    private final Label status = ViewParts.label("", "secondary-text");
    private final Button refresh = ViewParts.button("Refresh", "secondary-button");

    public SharedFilesTab(FileScanner fileScanner, SharedFileDao sharedFileDao) {
        super("Shared Files");
        this.fileScanner = fileScanner;
        this.sharedFileDao = sharedFileDao;

        TableView<SharedFile> table = new TableView<>();
        setUpFileTable(table);
        TextField search = ViewParts.searchable(table, files, "Search files",
                "No shared files yet. Open the folder, add files, then refresh.", SharedFile::fileName);

        Path folderPath = fileScanner.sharedDirectory().toAbsolutePath();
        Path project = Path.of("").toAbsolutePath();
        String displayPath = folderPath.startsWith(project) ? project.relativize(folderPath).toString() : folderPath.toString();
        Label folder = ViewParts.label(displayPath.isEmpty() ? "." : displayPath, "secondary-text");
        folder.setTooltip(new Tooltip(folderPath.toString()));
        status.setWrapText(true);
        Button open = ViewParts.folderButton("Open shared folder", fileScanner.sharedDirectory(), status);
        refresh.setOnAction(event -> refreshFiles());

        VBox directory = new VBox(4, ViewParts.label("Sharing from", "section-title"), folder);
        directory.getStyleClass().add("form-panel");
        setContent(ViewParts.page(directory, ViewParts.row(search, ViewParts.spacer(), refresh, open), table, status));
    }

    static void setUpFileTable(TableView<SharedFile> table) {
        TableColumn<SharedFile, String> name = new TableColumn<>("File name");
        name.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().fileName()));
        name.setPrefWidth(310);
        TableColumn<SharedFile, Number> size = new TableColumn<>("Size");
        size.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().fileSize()));
        size.setPrefWidth(100);
        size.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : ViewParts.bytes(value.longValue()));
            }
        });
        TableColumn<SharedFile, LocalDateTime> modified = new TableColumn<>("Last changed");
        modified.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().lastModified()));
        modified.setPrefWidth(190);
        modified.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : ViewParts.date(value));
            }
        });
        table.getColumns().setAll(List.of(name, size, modified));
    }

    public void refreshFiles() {
        if (refresh.isDisabled()) {
            return;
        }
        refresh.setDisable(true);
        status.setText("Checking the shared folder…");

        Task<List<SharedFile>> task = new Task<>() {
            @Override
            protected List<SharedFile> call() throws Exception {
                return fileScanner.scanAndStore(sharedFileDao);
            }
        };
        task.setOnSucceeded(event -> {
            files.setAll(task.getValue());
            status.setText(files.size() == 1 ? "1 file in the shared folder" : files.size() + " files in the shared folder");
            refresh.setDisable(false);
        });
        task.setOnFailed(event -> {
            AppLogger.create().log(Level.WARNING, "Could not scan shared folder", task.getException());
            status.setText("Couldn’t read the shared folder. Check that it’s available and try refreshing.");
            refresh.setDisable(false);
        });
        Thread worker = new Thread(task, "SharedFiles-ScannerTask");
        worker.setDaemon(true);
        worker.start();
    }
}
