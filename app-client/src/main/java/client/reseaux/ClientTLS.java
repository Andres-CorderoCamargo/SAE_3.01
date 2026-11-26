package client.reseaux;


import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ClientTLS {

    private static final String[] PROTOCOLES = {"TLSv1.3", "TLSv1.2"};

    private ClientTLS() {
    }

    /** Ouvre une connexion TLS vers le serveur et verifie son certificat. */
    public static Socket connecter(String hote, int port) throws IOException {
        Path certs = trouverDossierCerts();

        String chemin = System.getenv("TLS_TRUSTSTORE");
        if (chemin == null && certs != null) {
            chemin = certs.resolve("client-truststore.p12").toString();
        }

        String mdp = lire("TLS_TRUSTSTORE_PASSWORD",
                certs == null ? null : certs.resolve("tls.properties"), "password");

        if (chemin == null || mdp == null) {
            throw new IOException("Truststore introuvable : lancez certs/Generer_Certs.sh "
                    + "ou définissez TLS_TRUSTSTORE et TLS_TRUSTSTORE_PASSWORD");
        }

        try {
            KeyStore ts = KeyStore.getInstance("PKCS12");
            try (InputStream in = new FileInputStream(chemin)) {
                ts.load(in, mdp.toCharArray());
            }

            TrustManagerFactory tmf =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);

            SSLContext contexte = SSLContext.getInstance("TLS");
            contexte.init(null, tmf.getTrustManagers(), null);

            SSLSocket socket = (SSLSocket) contexte.getSocketFactory().createSocket(hote, port);

            SSLParameters params = socket.getSSLParameters();
            params.setEndpointIdentificationAlgorithm("HTTPS");
            socket.setSSLParameters(params);

            socket.startHandshake();
            return socket;

        } catch (GeneralSecurityException e) {
            throw new IOException("Initialisation TLS impossible : " + e.getMessage(), e);
        }
    }

    private static String[] protocolesDisponibles(String[] supportes) {
        List<String> liste = new ArrayList<>();
        for (String p : PROTOCOLES) {
            if (Arrays.asList(supportes).contains(p)) {
                liste.add(p);
            }
        }
        return liste.toArray(new String[0]);
    }


    private static Path trouverDossierCerts() {
        Path dossier = Paths.get("").toAbsolutePath();
        while (dossier != null) {
            Path certs = dossier.resolve("certs");
            if (Files.isDirectory(certs)) {
                return certs;
            }
            dossier = dossier.getParent();
        }
        return null;
    }


    private static String lire(String variable, Path fichierPropre, String cleFichier) {
        String valeur = System.getenv(variable);
        if (valeur != null) {
            return valeur;
        }
        if (fichierPropre != null && Files.isRegularFile(fichierPropre)) {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(fichierPropre)) {
                p.load(in);
                return p.getProperty(cleFichier);
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

}