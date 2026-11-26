package systeme.reseaux;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;


public class ServeurTLS {



    private static final String[] PROTOCOLES = {"TLSv1.3", "TLSv1.2"};

    private ServeurTLS() {
    }

    /** Ouvre une socket serveur TLS sur le port donne. */
    public static SSLServerSocket ouvrir(int port) throws IOException {
        Path certs = trouverDossierCerts();

        String chemin = System.getenv("TLS_KEYSTORE");

        if (chemin == null && certs != null) {
            chemin = certs.resolve("serveur.p12").toString();
        }

        String mdp = lire("TLS_KEYSTORE_PASSWORD",
                certs == null ? null : certs.resolve("tls.properties"), "password");


        if (chemin == null || mdp == null) {
            throw new IOException("Keystore introuvable : lancez certs/Generer_Certs.sh "
                    + "ou définissez TLS_KEYSTORE et TLS_KEYSTORE_PASSWORD");
        }

        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (InputStream in = new FileInputStream(chemin)) {
                ks.load(in, mdp.toCharArray());
            }

            KeyManagerFactory kmf =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, mdp.toCharArray());

            SSLContext contexte = SSLContext.getInstance("TLS");
            contexte.init(kmf.getKeyManagers(), null, null);

            SSLServerSocket socket =
                    (SSLServerSocket) contexte.getServerSocketFactory().createServerSocket(port);
            socket.setEnabledProtocols(protocolesDisponibles(socket.getSupportedProtocols()));
            return socket;

        } catch (GeneralSecurityException e) {
            // keystore illisible, mauvais mot de passe, etc.
            throw new IOException("Initialisation TLS impossible : " + e.getMessage(), e);
        }
    }

    /** Garde TLS 1.3 et 1.2 s'ils existent sur cette version de Java. */
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