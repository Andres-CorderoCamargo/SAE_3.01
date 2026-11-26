package systeme.reseaux;

import com.g3.vote.ConfigTLS;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;


public class ServeurTLS {



    private ServeurTLS() {
    }

    /** Ouvre une socket serveur TLS sur le port donne. */
    public static SSLServerSocket ouvrir(int port) throws IOException {
        String chemin = ConfigTLS.chemin("TLS_KEYSTORE", "serveur.p12");
        String mdp = ConfigTLS.motDePasse("TLS_KEYSTORE_PASSWORD");



        try {
            KeyStore ks = ConfigTLS.charger(chemin, mdp);


            KeyManagerFactory kmf =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, mdp.toCharArray());

            SSLContext contexte = SSLContext.getInstance("TLS");
            contexte.init(kmf.getKeyManagers(), null, null);

            SSLServerSocket socket =
                    (SSLServerSocket) contexte.getServerSocketFactory().createServerSocket(port);
            socket.setEnabledProtocols(ConfigTLS.protocoles(socket.getSupportedProtocols()));
            return socket;

        } catch (GeneralSecurityException e) {
            // keystore illisible, mauvais mot de passe, etc.
            throw new IOException("Initialisation TLS impossible : " + e.getMessage(), e);
        }
    }
}