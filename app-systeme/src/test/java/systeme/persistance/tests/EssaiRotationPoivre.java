package systeme.persistance.tests;

import systeme.persistance.AuthentificationDAO;
import systeme.persistance.BaseDeDonnees;
import systeme.persistance.MotDePasse;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Essai du changement de poivre sur la vraie base (a lancer a la main, ce
 * n'est pas un test JUnit). Il cree son propre compte et l'efface a la fin.
 *
 * Il n'utilise PAS SAE_POIVRE : il passe lui-meme deux poivres d'essai a
 * AuthentificationDAO.connecter, pour jouer tout le scenario sans relancer
 * le programme. Il a seulement besoin de ORACLE_URL, ORACLE_USER et
 * ORACLE_PASSWORD.
 */
public class EssaiRotationPoivre {

    private static final String ID = "T905";
    private static final String LOGIN = "test_rotation";
    private static final String MDP = "Rotation123!";
    private static final String ANCIEN = "ancien-poivre-essai";
    private static final String NOUVEAU = "nouveau-poivre-essai";

    private static int ok = 0;
    private static int ko = 0;

    public static void main(String[] args) throws SQLException {
        nettoyer();
        creerCompteAvecAncienPoivre();
        String empreinteDepart = lireEmpreinte();

        // 1. Le serveur ne connait que le nouveau poivre : le compte est perdu
        AuthentificationDAO.Resultat r = AuthentificationDAO.connecter(LOGIN, MDP, NOUVEAU, null);
        verifier("1. Nouveau poivre seul : connexion refusée", !r.succes);
        verifier("   ... et l'empreinte n'a pas bougé", empreinteDepart.equals(lireEmpreinte()));

        // 2. Mauvais mot de passe pendant la transition : refus, rien ne change
        r = AuthentificationDAO.connecter(LOGIN, "MauvaisMdp", NOUVEAU, ANCIEN);
        verifier("2. Mauvais mdp avec les deux poivres : refusé", !r.succes);
        verifier("   ... et l'empreinte n'a pas bougé", empreinteDepart.equals(lireEmpreinte()));

        // 3. Transition (nouveau + ancien) : connexion acceptée et empreinte refaite
        r = AuthentificationDAO.connecter(LOGIN, MDP, NOUVEAU, ANCIEN);
        String empreinteMigree = lireEmpreinte();
        verifier("3. Bon mdp avec les deux poivres : accepté (" + r.message + ")", r.succes);
        verifier("   ... l'empreinte a changé", !empreinteDepart.equals(empreinteMigree));
        verifier("   ... la nouvelle empreinte marche avec le nouveau poivre",
                MotDePasse.verifier(MDP, empreinteMigree, NOUVEAU));
        verifier("   ... et plus avec l'ancien",
                !MotDePasse.verifier(MDP, empreinteMigree, ANCIEN));

        // 4. Fin de la transition : l'ancien poivre est retiré, le compte marche toujours
        r = AuthentificationDAO.connecter(LOGIN, MDP, NOUVEAU, null);
        verifier("4. Après migration, nouveau poivre seul : accepté", r.succes);
        verifier("   ... sans re-hacher une 2e fois", empreinteMigree.equals(lireEmpreinte()));

        nettoyer();
        System.out.println("\nRésultat : " + ok + " OK, " + ko + " KO");
    }

    /** Compte créé directement avec une empreinte faite avec l'ANCIEN poivre. */
    private static void creerCompteAvecAncienPoivre() throws SQLException {
        try (Connection cx = BaseDeDonnees.ouvrir();
             CallableStatement proc = cx.prepareCall("{ call ajouterElecteur(?, ?, ?, ?, ?, ?) }")) {
            proc.setString(1, ID);
            proc.setString(2, LOGIN);
            proc.setString(3, MotDePasse.hacher(MDP, ANCIEN));
            proc.setString(4, "Rotation");
            proc.setString(5, "Test");
            proc.registerOutParameter(6, Types.VARCHAR);
            proc.execute();
            System.out.println("Création du compte d'essai : " + proc.getString(6));
        }
    }

    private static String lireEmpreinte() throws SQLException {
        try (Connection cx = BaseDeDonnees.ouvrir();
             PreparedStatement req = cx.prepareStatement(
                     "SELECT mdpHashUtilisateur FROM Utilisateurs WHERE idUtilisateur = ?")) {
            req.setString(1, ID);
            try (ResultSet rs = req.executeQuery()) {
                return rs.next() ? rs.getString(1) : "";
            }
        }
    }

    private static void nettoyer() throws SQLException {
        AuthentificationDAO.supprimerElecteur(ID);
    }

    private static void verifier(String nom, boolean condition) {
        if (condition) {
            ok++;
            System.out.println("OK  " + nom);
        } else {
            ko++;
            System.out.println("KO  " + nom);
        }
    }
}
