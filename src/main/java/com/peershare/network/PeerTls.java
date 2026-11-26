package com.peershare.network;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;

/** Loads the local identity and the certificates explicitly trusted for LAN sharing. */
public final class PeerTls {
    private PeerTls() {
    }

    public static SSLContext load(Path keyStore, Path trustStore, char[] password) throws IOException {
        try {
            KeyStore identity = loadStore(keyStore, password);
            KeyStore trustedPeers = loadStore(trustStore, password);
            KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keys.init(identity, password);
            TrustManagerFactory trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trust.init(trustedPeers);
            SSLContext context = SSLContext.getInstance("TLSv1.3");
            context.init(keys.getKeyManagers(), trust.getTrustManagers(), null);
            return context;
        } catch (GeneralSecurityException | IOException ex) {
            throw new IOException("Couldn’t load the LAN identity and trusted peer certificates. Check the files and password.", ex);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static KeyStore loadStore(Path path, char[] password) throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(path)) {
            store.load(input, password);
        }
        return store;
    }
}
