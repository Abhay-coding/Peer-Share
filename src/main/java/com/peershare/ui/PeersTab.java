package com.peershare.ui;

import com.peershare.dao.PeerDao;
import com.peershare.model.Peer;
import com.peershare.util.AppLogger;
import com.peershare.util.InputValidator;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class PeersTab extends Tab {
    private final PeerDao peerDao;
    private final ObservableList<Peer> peers = FXCollections.observableArrayList();
    private final TableView<Peer> table = new TableView<>();
    private final TextField name = new TextField();
    private final TextField address = new TextField();
    private final TextField port = new TextField();
    private final Label formTitle = ViewParts.label("Add a peer", "section-title");
    private final Label feedback = ViewParts.label("", "secondary-text");
    private final Button save = ViewParts.button("Add peer", "primary-button");
    private final Button clear = ViewParts.button("Clear", "secondary-button");
    private final Button remove = ViewParts.button("Remove", "danger-button");
    private final Button browse = ViewParts.button("Browse files", "secondary-button");
    private boolean busy;

    public PeersTab(PeerDao peerDao, Consumer<Peer> browsePeer) {
        super("Peers");
        this.peerDao = peerDao;
        setUpTable();

        name.setPromptText("Studio PC");
        address.setPromptText("192.168.1.10");
        port.setPromptText("5000");
        VBox nameInput = new VBox(5, ViewParts.fieldLabel("Name", name), name);
        VBox addressInput = new VBox(5, ViewParts.fieldLabel("IP address or hostname", address), address);
        VBox portInput = new VBox(5, ViewParts.fieldLabel("Port", port), port);
        HBox.setHgrow(nameInput, Priority.ALWAYS);
        HBox.setHgrow(addressInput, Priority.ALWAYS);
        portInput.setPrefWidth(90);
        portInput.setMaxWidth(110);

        save.setOnAction(event -> savePeer());
        port.setOnAction(event -> save.fire());
        clear.setOnAction(event -> {
            clearForm();
            name.requestFocus();
        });
        browse.setOnAction(event -> browsePeer.accept(selectedPeer()));
        table.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !browse.isDisabled()) {
                browse.fire();
                event.consume();
            }
        });
        table.setRowFactory(view -> {
            TableRow<Peer> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty()) {
                    browse.fire();
                }
            });
            return row;
        });
        remove.setOnAction(event -> removePeer());
        feedback.setWrapText(true);
        VBox form = new VBox(10, formTitle, ViewParts.row(nameInput, addressInput, portInput),
                ViewParts.row(save, clear, feedback));
        form.getStyleClass().add("form-panel");

        TextField search = ViewParts.searchable(table, peers, "Find a peer",
                "No saved peers yet. Add one above to browse its files.",
                peer -> peer.name() + " " + peer.ipAddress() + " " + peer.port());
        setContent(ViewParts.page(form, ViewParts.row(search, ViewParts.spacer(), browse, remove), table));
        updateActions();
    }

    private void setUpTable() {
        TableColumn<Peer, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
        nameColumn.setPrefWidth(245);
        TableColumn<Peer, String> addressColumn = new TableColumn<>("Address");
        addressColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().ipAddress()));
        addressColumn.setPrefWidth(240);
        TableColumn<Peer, Number> portColumn = new TableColumn<>("Port");
        portColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().port()));
        portColumn.setPrefWidth(90);
        table.getColumns().setAll(List.of(nameColumn, addressColumn, portColumn));
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldPeer, peer) -> {
            name.setText(peer == null ? "" : peer.name());
            address.setText(peer == null ? "" : peer.ipAddress());
            port.setText(peer == null ? "" : String.valueOf(peer.port()));
            showFeedback("", false);
            updateActions();
        });
    }

    private Peer selectedPeer() {
        return table.getSelectionModel().getSelectedItem();
    }

    private void updateActions() {
        boolean selected = selectedPeer() != null;
        formTitle.setText(selected ? "Edit peer" : "Add a peer");
        save.setText(selected ? "Save changes" : "Add peer");
        browse.setDisable(busy || !selected);
        remove.setDisable(busy || !selected);
        save.setDisable(busy);
        clear.setDisable(busy);
        table.setDisable(busy);
        name.setDisable(busy);
        address.setDisable(busy);
        port.setDisable(busy);
    }

    private void savePeer() {
        int portNumber;
        try {
            InputValidator.validatePeer(name.getText(), address.getText(), port.getText());
            portNumber = InputValidator.parsePort(port.getText());
        } catch (IllegalArgumentException ex) {
            showFeedback(ex.getMessage(), true);
            TextField invalid = name.getText().isBlank() ? name : address.getText().isBlank() ? address : port;
            invalid.requestFocus();
            return;
        }

        Peer selected = selectedPeer();
        Peer peer = new Peer(selected == null ? 0 : selected.id(), name.getText().strip(), address.getText().strip(), portNumber);
        runChange(() -> {
            if (selected == null) {
                return peerDao.create(peer);
            }
            peerDao.update(peer);
            return peer;
        }, saved -> {
            if (selected == null) {
                peers.add(saved);
            } else {
                peers.set(peers.indexOf(selected), saved);
            }
            clearForm();
            showFeedback("Peer saved.", false);
        }, "Couldn’t save this peer.");
    }

    private void removePeer() {
        Peer selected = selectedPeer();
        if (!ViewParts.confirm(table, "Remove " + selected.name() + "?", "You can add this peer again later.", "Remove peer")) {
            return;
        }
        runChange(() -> {
            peerDao.deleteById(selected.id());
            return selected;
        }, removed -> {
            peers.remove(removed);
            clearForm();
            showFeedback("Peer removed.", false);
        }, "Couldn’t remove this peer.");
    }

    public void refreshPeers() {
        if (busy) {
            return;
        }
        runChange(peerDao::findAll, loaded -> {
            if (!peers.equals(loaded)) {
                peers.setAll(loaded);
            }
        }, "Couldn’t load saved peers.");
    }

    private <T> void runChange(Callable<T> action, Consumer<T> success, String failure) {
        busy = true;
        updateActions();
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return action.call();
            }
        };
        task.setOnSucceeded(event -> {
            busy = false;
            updateActions();
            success.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            busy = false;
            updateActions();
            Throwable error = task.getException();
            AppLogger.create().log(Level.WARNING, failure, error);
            showFeedback(failure, true);
        });
        new Thread(task, "PeersTab-DatabaseTask").start();
    }

    private void clearForm() {
        table.getSelectionModel().clearSelection();
        name.clear();
        address.clear();
        port.clear();
        showFeedback("", false);
    }

    private void showFeedback(String text, boolean error) {
        feedback.getStyleClass().remove("error-text");
        if (error) {
            feedback.getStyleClass().add("error-text");
        }
        feedback.setText(text);
    }
}
