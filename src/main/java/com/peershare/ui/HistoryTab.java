package com.peershare.ui;

import com.peershare.dao.DownloadHistoryDao;
import com.peershare.model.DownloadHistory;
import com.peershare.util.AppLogger;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HistoryTab displays past download history records and allows deleting entries.
 */
public final class HistoryTab extends Tab {
    private static final Logger LOGGER = AppLogger.create();

    private final DownloadHistoryDao historyDao;
    private final ObservableList<DownloadHistory> historyList = FXCollections.observableArrayList();
    private final TableView<DownloadHistory> historyTable = new TableView<>();

    private final Button refreshButton = new Button("Refresh History");
    private final Button deleteButton = new Button("Delete Selected");

    public HistoryTab(DownloadHistoryDao historyDao) {
        super("History");
        this.historyDao = historyDao;
        setClosable(false);

        setContent(createContent());
        refreshHistory();
    }

    private Pane createContent() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(15));

        refreshButton.setOnAction(e -> refreshHistory());
        deleteButton.setOnAction(e -> handleDeleteSelected());

        HBox buttonBar = new HBox(10, refreshButton, deleteButton);
        buttonBar.setAlignment(Pos.CENTER_LEFT);

        setupTable();
        VBox.setVgrow(historyTable, Priority.ALWAYS);

        root.getChildren().addAll(buttonBar, historyTable);
        return root;
    }

    private void setupTable() {
        TableColumn<DownloadHistory, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().id()));
        idCol.setPrefWidth(60);

        TableColumn<DownloadHistory, String> fileCol = new TableColumn<>("File Name");
        fileCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().fileName()));
        fileCol.setPrefWidth(240);

        TableColumn<DownloadHistory, String> peerCol = new TableColumn<>("Peer Name");
        peerCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().peerName()));
        peerCol.setPrefWidth(160);

        TableColumn<DownloadHistory, String> dateCol = new TableColumn<>("Downloaded At");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().downloadedAt() != null
                        ? data.getValue().downloadedAt().toString().replace('T', ' ')
                        : ""));
        dateCol.setPrefWidth(180);

        TableColumn<DownloadHistory, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status()));
        statusCol.setPrefWidth(100);

        // Custom cell to highlight SUCCESS in green and FAILED in red
        statusCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("SUCCESS".equalsIgnoreCase(status)) {
                        setStyle("-fx-text-fill: #1e7e34; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #bd2130; -fx-font-weight: bold;");
                    }
                }
            }
        });

        historyTable.getColumns().addAll(idCol, fileCol, peerCol, dateCol, statusCol);
        historyTable.setItems(historyList);
        historyTable.setPlaceholder(new Label("No download history recorded yet."));
    }

    public void refreshHistory() {
        try {
            historyList.setAll(historyDao.findAll());
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load download history", ex);
        }
    }

    private void handleDeleteSelected() {
        DownloadHistory selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Selection Required");
            alert.setHeaderText(null);
            alert.setContentText("Please select a history record to delete.");
            alert.showAndWait();
            return;
        }

        try {
            historyDao.deleteById(selected.id());
            refreshHistory();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to delete history record", ex);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText(null);
            alert.setContentText("Could not delete history record: " + ex.getMessage());
            alert.showAndWait();
        }
    }
}
