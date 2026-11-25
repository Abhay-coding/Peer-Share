// Source: src\main\java\com\peershare\network\SocketDeadline.java (lines 13-68)
// Commit: 230bb5fb750812f7d1f19a3fc5586932f99e58ed
// Concept: ScheduledThreadPoolExecutor & AutoCloseable Deadline

final class SocketDeadline implements AutoCloseable {
    private static final ScheduledThreadPoolExecutor TIMER = new ScheduledThreadPoolExecutor(1, task -> {
        Thread thread = new Thread(task, "PeerShare-SocketDeadline");
        thread.setDaemon(true);
        return thread;
    });

    static {
        TIMER.setRemoveOnCancelPolicy(true);
    }

    private final BooleanSupplier cancelled;
    private final ScheduledFuture<?> timeout;
    private final ScheduledFuture<?> cancellation;
    private volatile boolean expired;

    SocketDeadline(Socket socket, int timeoutMs, BooleanSupplier cancelled) {
        this.cancelled = cancelled;
        timeout = TIMER.schedule(() -> {
            expired = true;
            closeSocket(socket);
        }, timeoutMs, TimeUnit.MILLISECONDS);
        cancellation = cancelled == null ? null : TIMER.scheduleWithFixedDelay(() -> {
            if (cancelled.getAsBoolean()) {
                closeSocket(socket);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    IOException failure(IOException cause) {
        if (cancelled != null && cancelled.getAsBoolean()) {
            throw new CancellationException("Download cancelled.");
        }
        if (expired) {
            SocketTimeoutException error = new SocketTimeoutException("Peer request timed out.");
            error.initCause(cause);
            return error;
        }
        return cause;
    }

    @Override
    public void close() {
        timeout.cancel(false);
        if (cancellation != null) {
            cancellation.cancel(false);
        }
    }

    private static void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // Request handling also owns the socket and closes it on exit.
        }
    }
