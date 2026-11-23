package com.peershare.network;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Closes a socket at a fixed deadline, including when a read or write is blocked. */
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
}
