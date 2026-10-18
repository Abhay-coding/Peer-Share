package com.peershare.ui;

import com.peershare.dao.PeerDao;
import com.peershare.model.Peer;
import com.peershare.util.AppLogger;
import com.peershare.util.InputValidator;
import javafx.beans.property.SimpleIntegerProperty;
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
 * PeersTab allows the user to manage saved peers (Create, Read, Update, Delete).
 */
public final class PeersTab extends Tab {
    private static final Logger LOGGER = AppLogger.create();

    private final PeerDao peerDao;
    private final ObservableList<Peer> peerList = FXCollections.observableArrayList();

    private final TableView<Peer> peerTable = new TableView<>();
    private final TextField nameField = new TextField();
    private final TextField ipField = new TextField();
    private final TextField portField = new TextField();

    private Peer selectedPeer = null;

    public PeersTab(PeerDao peerDao) {
        super("Peers");
        this.peerDao = peerDao;
        setClosable(false);

        setContent(createContent());
        refreshPeers();
    }

    private Pane createContent() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(15));

        // Form Section
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        formGrid.setAlignment(Pos.CENTER_LEFT);

        nameField.setPromptText("Peer Name (e.g. Rahul)");
        ipField.setPromptText("IP Address or Hostname (e.g. 127.0.0.1)");
        portField.setPromptText("Port (1 - 65535)");

        formGrid.add(new Label("Peer Name:"), 0, 0);
        formGrid.add(nameField, 1, 0);
        formGrid.add(new Label("IP Address:"), 0, 1);
        formGrid.add(ipField, 1, 1);
        formGrid.add(new Label("Port:"), 0, 2);
        formGrid.add(portField, 1, 2);

        // Buttons
        Button addButton = new Button("Add Peer");
        Button updateButton = new Button("Update Selected");
        Button deleteButton = new Button("Delete Selected");
        Button clearButton = new Button("Clear Form");

        addButton.setOnAction(e -> handleAddPeer());
        updateButton.setOnAction(e -> handleUpdatePeer());
        deleteButton.setOnAction(e -> handleDeletePeer());
        clearButton.setOnAction(e -> clearForm());

        HBox buttonBar = new HBox(10, addButton, updateButton, deleteButton, clearButton);
        buttonBar.setAlignment(Pos.CENTER_LEFT);

        // Table setup
        setupTable();

        VBox.setVgrow(peerTable, Priority.ALWAYS);
        root.getChildren().addAll(new Label("Manage Saved Peers:"), formGrid, buttonBar, peerTable);
        return root;
    }

    private void setupTable() {
        TableColumn<Peer, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().id()));
        idCol.setPrefWidth(60);

        TableColumn<Peer, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        nameCol.setPrefWidth(160);

        TableColumn<Peer, String> ipCol = new TableColumn<>("IP Address");
        ipCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().ipAddress()));
        ipCol.setPrefWidth(160);

        TableColumn<Peer, Number> portCol = new TableColumn<>("Port");
        portCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().port()));
        portCol.setPrefWidth(100);

        peerTable.getColumns().addAll(idCol, nameCol, ipCol, portCol);
        peerTable.setItems(peerList);
        peerTable.setPlaceholder(new Label("No peers saved. Add a peer above."));

        peerTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedPeer = newVal;
            if (newVal != null) {
                nameField.setText(newVal.name());
                ipField.setText(newVal.ipAddress());
                portField.setText(String.valueOf(newVal.port()));
            }
        });
    }

    private void handleAddPeer() {
        try {
            InputValidator.validatePeer(nameField.getText(), ipField.getText(), portField.getText());
            int port = InputValidator.parsePort(portField.getText());

            Peer peer = new Peer(0, nameField.getText().trim(), ipField.getText().trim(), port);
            peerDao.create(peer);
            refreshPeers();
            clearForm();
        } catch (IllegalArgumentException ex) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", ex.getMessage());
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to create peer", ex);
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not save peer: " + ex.getMessage());
        }
    }

    private void handleUpdatePeer() {
        if (selectedPeer == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a peer to update.");
            return;
        }

        try {
            InputValidator.validatePeer(nameField.getText(), ipField.getText(), portField.getText());
            int port = InputValidator.parsePort(portField.getText());

            Peer updated = new Peer(selectedPeer.id(), nameField.getText().trim(), ipField.getText().trim(), port);
            peerDao.update(updated);
            refreshPeers();
            clearForm();
        } catch (IllegalArgumentException ex) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", ex.getMessage());
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to update peer", ex);
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not update peer: " + ex.getMessage());
        }
    }

    private void handleDeletePeer() {
        if (selectedPeer == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a peer to delete.");
            return;
        }

        try {
            peerDao.deleteById(selectedPeer.id());
            refreshPeers();
            clearForm();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to delete peer", ex);
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not delete peer: " + ex.getMessage());
        }
    }

    public void refreshPeers() {
        try {
            peerList.setAll(peerDao.findAll());
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load peers", ex);
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load peers: " + ex.getMessage());
        }
    }

    private void clearForm() {
        selectedPeer = null;
        nameField.clear();
        ipField.clear();
        portField.clear();
        peerTable.getSelectionModel().clearSelection();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
