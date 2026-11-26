package client.reseaux;


import com.g3.vote.ConfigTLS;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

public class ClientTLS {

    private ClientTLS() {
    }

    /** Ouvre une connexion TLS vers le serveur et verifie son certificat. */
    public static Socket connecter(String hote, int port) throws IOException {
        String chemin = ConfigTLS.chemin("TLS_TRUSTSTORE", "client-truststore.p12");
        String mdp = ConfigTLS.motDePasse("TLS_TRUSTSTORE_PASSWORD");

        try {
            KeyStore ts = ConfigTLS.charger(chemin, mdp);

            TrustManagerFactory tmf =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);

            SSLContext contexte = SSLContext.getInstance("TLS");
            contexte.init(null, tmf.getTrustManagers(), null);

            SSLSocket socket = (SSLSocket) contexte.getSocketFactory().createSocket(hote, port);

            SSLParameters params = socket.getSSLParameters();
            params.setEndpointIdentificationAlgorithm("HTTPS");
            params.setProtocols(ConfigTLS.protocoles(socket.getSupportedProtocols()));
            socket.setSSLParameters(params);

            socket.startHandshake();
            return socket;

        } catch (GeneralSecurityException e) {
            throw new IOException("Initialisation TLS impossible : " + e.getMessage(), e);
        }
    }
}