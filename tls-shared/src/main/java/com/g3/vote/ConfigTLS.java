package com.g3.vote;

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

/**
 * Ce que le serveur et le client ont en commun pour le TLS.
 *
 * Les fichiers sont cherches dans le dossier certs/ du projet (cree par
 * certs/Generer_Certs.sh). Les variables d'environnement, si elles existent,
 * ont priorite sur ce dossier.
 */
public final class ConfigTLS {

    private static final String[] PROTOCOLES = {"TLSv1.3", "TLSv1.2"};

    private ConfigTLS() {
    }

    /** Chemin d'un fichier : la variable d'environnement, sinon certs/nomFichier. */
    public static String chemin(String variable, String nomFichier) throws IOException {
        String valeur = System.getenv(variable);
        if (valeur != null) {
            return valeur;
        }
        Path certs = trouverDossierCerts();
        if (certs == null) {
            throw new IOException("Dossier certs introuvable : lancez certs/Generer_Certs.sh "
                    + "ou définissez " + variable);
        }
        return certs.resolve(nomFichier).toString();
    }

    /** Mot de passe : la variable d'environnement, sinon certs/tls.properties. */
    public static String motDePasse(String variable) throws IOException {
        String valeur = System.getenv(variable);
        if (valeur != null) {
            return valeur;
        }
        Path certs = trouverDossierCerts();
        if (certs != null) {
            Path fichier = certs.resolve("tls.properties");
            if (Files.isRegularFile(fichier)) {
                Properties p = new Properties();
                try (InputStream in = Files.newInputStream(fichier)) {
                    p.load(in);
                }
                String mdp = p.getProperty("password");
                if (mdp != null) {
                    return mdp;
                }
            }
        }
        throw new IOException("Mot de passe TLS introuvable : lancez certs/Generer_Certs.sh "
                + "ou définissez " + variable);
    }

    /** Charge un fichier PKCS12 (keystore ou truststore). */
    public static KeyStore charger(String chemin, String mdp)
            throws IOException, GeneralSecurityException {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = new FileInputStream(chemin)) {
            ks.load(in, mdp.toCharArray());
        }
        return ks;
    }

    /** Garde TLS 1.3 et 1.2 s'ils existent sur cette version de Java. */
    public static String[] protocoles(String[] supportes) {
        List<String> liste = new ArrayList<>();
        for (String p : PROTOCOLES) {
            if (Arrays.asList(supportes).contains(p)) {
                liste.add(p);
            }
        }
        return liste.toArray(new String[0]);
    }

    /** Remonte les dossiers parents jusqu'a trouver un dossier "certs". */
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
}