package com.peershare.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Protocol defines the commands, status codes, and line-reading utilities
 * used for communication between PeerShare peers over TCP.
 */
public final class Protocol {
    public static final int MAX_LINE_BYTES = 4096;
    public static final int MAX_LIST_FILES = 500;
    public static final long MAX_FILE_BYTES = 100L * 1024 * 1024;
    public static final String CMD_LIST = "LIST";
    public static final String CMD_DOWNLOAD = "DOWNLOAD";
    public static final String STATUS_OK = "OK";
    public static final String STATUS_ERROR = "ERROR";

    private Protocol() {
    }

    /**
     * Reads a line of text from the input stream up to the next newline character.
     * Unlike BufferedReader, this does not buffer ahead, which allows switching to
     * binary streaming immediately after reading the header.
     *
     * @param in the InputStream to read from
     * @return the line as a UTF-8 String, or null if the end of the stream is reached
     * @throws IOException if a network error occurs
     */
    public static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') {
                break;
            }
            if (b != '\r') {
                if (buffer.size() >= MAX_LINE_BYTES) {
                    throw new IOException("Protocol line is too long.");
                }
                buffer.write(b);
            }
        }
        if (b == -1 && buffer.size() == 0) {
            return null;
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    /**
     * Writes a line of text terminated by a newline character in UTF-8 format.
     *
     * @param out the OutputStream to write to
     * @param line the text line to send
     * @throws IOException if a network error occurs
     */
    public static void writeLine(OutputStream out, String line) throws IOException {
        out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
