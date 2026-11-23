package com.peershare.ui;

import com.peershare.dao.DownloadHistoryDao;
import com.peershare.model.DownloadHistory;
import com.peershare.util.AppLogger;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;

public final class HistoryTab extends Tab {
    private final DownloadHistoryDao historyDao;
    private final ObservableList<DownloadHistory> history = FXCollections.observableArrayList();
    private final TableView<DownloadHistory> table = new TableView<>();
    private final Label feedback = ViewParts.label("", "secondary-text");
    private final Button refresh = ViewParts.button("Refresh", "secondary-button");
    private final Button remove = ViewParts.button("Remove entry", "danger-button");
    private boolean reloadRequested;

    public HistoryTab(DownloadHistoryDao historyDao, Path downloadDirectory) {
        super("History");
        this.historyDao = historyDao;
        setUpTable();

        TextField search = ViewParts.searchable(table, history, "Search downloads",
                "Downloads will appear here after your first transfer.",
                entry -> entry.fileName() + " " + entry.peerName() + " " + displayedStatus(entry.status()));
        refresh.setOnAction(event -> refreshHistory());
        remove.disableProperty().bind(refresh.disableProperty().or(table.getSelectionModel().selectedItemProperty().isNull()));
        remove.setOnAction(event -> removeEntry());
        feedback.setWrapText(true);
        Button open = ViewParts.folderButton("Open downloads", downloadDirectory, feedback);
        setContent(ViewParts.page(ViewParts.row(search, ViewParts.spacer(), refresh, open), table,
                ViewParts.row(remove, feedback)));
    }

    private void setUpTable() {
        TableColumn<DownloadHistory, String> name = new TableColumn<>("File name");
        name.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().fileName()));
        name.setPrefWidth(240);
        TableColumn<DownloadHistory, String> peer = new TableColumn<>("From");
        peer.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().peerName()));
        peer.setPrefWidth(150);
        TableColumn<DownloadHistory, LocalDateTime> date = new TableColumn<>("Date");
        date.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().downloadedAt()));
        date.setPrefWidth(175);
        date.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : ViewParts.date(value));
            }
        });
        TableColumn<DownloadHistory, String> status = new TableColumn<>("Result");
        status.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status()));
        status.setPrefWidth(100);
        status.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                getStyleClass().removeAll("transfer-success", "transfer-failed");
                setText(empty ? null : displayedStatus(value));
                if (!empty && "SUCCESS".equalsIgnoreCase(value)) {
                    getStyleClass().add("transfer-success");
                } else if (!empty && "FAILED".equalsIgnoreCase(value)) {
                    getStyleClass().add("transfer-failed");
                }
            }
        });
        table.getColumns().setAll(List.of(name, peer, date, status));
    }

    private static String displayedStatus(String status) {
        if ("SUCCESS".equalsIgnoreCase(status)) {
            return "Saved";
        }
        return "FAILED".equalsIgnoreCase(status) ? "Failed" : status;
    }

    public void refreshHistory() {
        if (refresh.isDisabled()) {
            reloadRequested = true;
            return;
        }
        updateHistory(null);
    }

    private void removeEntry() {
        DownloadHistory selected = table.getSelectionModel().getSelectedItem();
        if (ViewParts.confirm(table, "Remove this history entry?",
                "The file stays in your downloads folder.", "Remove entry")) {
            updateHistory(selected);
        }
    }

    private void updateHistory(DownloadHistory toRemove) {
        refresh.setDisable(true);
        feedback.setText(toRemove == null ? "Checking download history…" : "Removing entry…");
        Task<List<DownloadHistory>> task = new Task<>() {
            @Override
            protected List<DownloadHistory> call() throws Exception {
                if (toRemove != null) {
                    historyDao.deleteById(toRemove.id());
                }
                return historyDao.findAll();
            }
        };
        task.setOnSucceeded(event -> {
            if (!history.equals(task.getValue())) {
                history.setAll(task.getValue());
            }
            feedback.setText(toRemove != null ? "Entry removed." : "");
            finishUpdate();
        });
        task.setOnFailed(event -> {
            String message = toRemove == null ? "Couldn’t load download history." : "Couldn’t update download history.";
            AppLogger.create().log(Level.WARNING, message, task.getException());
            feedback.setText(message + " Try refreshing.");
            finishUpdate();
        });
        new Thread(task, "HistoryTab-DatabaseTask").start();
    }

    private void finishUpdate() {
        refresh.setDisable(false);
        if (reloadRequested) {
            reloadRequested = false;
            refreshHistory();
        }
    }
}
