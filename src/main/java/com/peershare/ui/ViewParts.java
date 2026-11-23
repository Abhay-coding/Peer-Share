package com.peershare.ui;

import com.peershare.util.AppLogger;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.function.Function;
import java.util.logging.Level;

final class ViewParts {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT);

    private ViewParts() {
    }

    static Label label(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        return label;
    }

    static Label fieldLabel(String text, Control field) {
        Label label = label(text, "field-label");
        label.setLabelFor(field);
        return label;
    }

    static VBox page(Node... children) {
        VBox page = new VBox(14, children);
        page.getStyleClass().add("page-content");
        return page;
    }

    static HBox row(Node... children) {
        HBox row = new HBox(10, children);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    static Button button(String text, String style) {
        Button button = new Button(text);
        button.getStyleClass().add(style);
        return button;
    }

    static <T> TextField searchable(TableView<T> table, ObservableList<T> items, String prompt,
                                    String empty, Function<T, String> keywords) {
        TextField filter = new TextField();
        filter.setPromptText(prompt);
        filter.setAccessibleText(prompt);
        filter.getStyleClass().add("search-field");

        FilteredList<T> filtered = new FilteredList<>(items);
        filter.textProperty().addListener((obs, oldValue, value) -> {
            String query = value.strip().toLowerCase(Locale.ROOT);
            filtered.setPredicate(item -> keywords.apply(item).toLowerCase(Locale.ROOT).contains(query));
        });
        SortedList<T> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        Label placeholder = label("", "secondary-text");
        placeholder.setWrapText(true);
        placeholder.textProperty().bind(Bindings.createStringBinding(
                () -> items.isEmpty() ? empty : "No matches. Try a different search.", items));
        table.setPlaceholder(placeholder);
        VBox.setVgrow(table, Priority.ALWAYS);
        return filter;
    }

    static Button folderButton(String text, Path directory, Label feedback) {
        Button button = button(text, "secondary-button");
        button.setOnAction(event -> {
            try {
                Files.createDirectories(directory);
                Desktop.getDesktop().open(directory.toFile());
            } catch (IOException | UnsupportedOperationException | SecurityException ex) {
                AppLogger.create().log(Level.WARNING, "Could not open folder " + directory, ex);
                feedback.setText("Open this folder in your file manager: " + directory.toAbsolutePath());
            }
        });
        return button;
    }

    static boolean confirm(Node owner, String question, String detail, String action) {
        ButtonType confirm = new ButtonType(action, ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, detail, ButtonType.CANCEL, confirm);
        alert.initOwner(owner.getScene().getWindow());
        alert.setTitle("PeerShare");
        alert.setHeaderText(question);
        ((Button) alert.getDialogPane().lookupButton(confirm)).setDefaultButton(false);
        ((Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setDefaultButton(true);
        return alert.showAndWait().filter(confirm::equals).isPresent();
    }

    static String date(LocalDateTime value) {
        return value == null ? "" : DATE.format(value);
    }

    static String bytes(long value) {
        if (value < 1024) {
            return value + " B";
        }
        if (value < 1024 * 1024) {
            return String.format("%.1f KB", value / 1024.0);
        }
        return String.format("%.1f MB", value / (1024.0 * 1024.0));
    }
}
