// Source: src\main\java\com\peershare\ui\ViewParts.java (lines 69-92)
// Commit: 230bb5fb750812f7d1f19a3fc5586932f99e58ed
// Concept: Generic FilteredList/SortedList Table Search

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
