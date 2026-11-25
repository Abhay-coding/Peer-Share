// Source: src\main\java\com\peershare\network\Protocol.java (lines 35-66)
// Commit: 230bb5fb750812f7d1f19a3fc5586932f99e58ed
// Concept: Byte-by-Byte Protocol Framing

    public static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int b;
        int bytesRead = 0;
        while ((b = in.read()) != -1) {
            if (b == '\n') {
                break;
            }
            if (++bytesRead > MAX_LINE_BYTES) {
                throw new IOException("Protocol line is too long.");
            }
            if (b != '\r') {
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
