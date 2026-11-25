// Source: src\main\java\com\peershare\ui\DownloadTab.java (lines 189-245)
// Commit: 230bb5fb750812f7d1f19a3fc5586932f99e58ed
// Concept: JavaFX Background Task & AtomicBoolean Cancellation

    private void downloadFile() {
        Peer peer = listedPeer;
        SharedFile file = table.getSelectionModel().getSelectedItem();
        AtomicBoolean cancelled = new AtomicBoolean();
        activeDownload = cancelled;
        String failure = "Couldn’t download " + file.fileName() + ".";
        runTask("Downloading " + file.fileName() + "…",
                error -> error.getMessage() == null ? failure : failure + " " + error.getMessage(),
                () -> transferService.download(peer, file.fileName(), file.fileSize(), cancelled::get),
                path -> showFeedback("Saved " + path.getFileName() + " to downloads.", false), true);
    }

    private <T> void runTask(String activity, Function<Throwable, String> failure, Callable<T> action,
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
            activeDownload = null;
            success.accept(task.getValue());
            updateActions();
            if (download) {
                transferFinished.run();
            }
        });
        task.setOnFailed(event -> {
            busy = false;
            activeDownload = null;
            Throwable error = task.getException();
            if (error instanceof CancellationException) {
                showFeedback("Download cancelled.", false);
            } else if (error instanceof TransferService.HistoryWriteException saved) {
                AppLogger.create().log(Level.WARNING, "Could not save download history", saved);
                showFeedback("Saved " + saved.savedFile().getFileName()
                        + " to downloads. Couldn’t save its history entry.", false);
            } else {
                String message = failure.apply(error);
                AppLogger.create().log(Level.WARNING, message, error);
                showFeedback(message, true);
            }
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
