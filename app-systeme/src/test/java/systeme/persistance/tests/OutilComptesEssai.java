package systeme.persistance.tests;

import systeme.persistance.AuthentificationDAO;
import systeme.persistance.BaseDeDonnees;
import systeme.persistance.MotDePasse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Outil pour la base LOCALE de test (pas un test JUnit, il se lance a la main).
 *
 * Le fichier BD/TEST/03_donnees_essai.sql met "hash_a_remplacer" comme mot de
 * passe. Cet outil remplace chaque "hash_a_remplacer" par le vrai hachage
 * (poivre + bcrypt, cf. MotDePasse) du mot de passe d'essai, puis verifie que
 * la connexion marche.
 *
 * Variables : ORACLE_URL, ORACLE_USER, ORACLE_PASSWORD, SAE_POIVRE.
 * Argument facultatif : le mot de passe d'essai (par defaut Test1234!).
 */
public class OutilComptesEssai {

    public static void main(String[] args) throws SQLException {
        String mdp = args.length > 0 ? args[0] : "Test1234!";

        List<String> ids = new ArrayList<String>();
        List<String> logins = new ArrayList<String>();
        try (Connection cx = BaseDeDonnees.ouvrir()) {
            try (PreparedStatement sel = cx.prepareStatement(
                    "SELECT idUtilisateur, identifiantUtilisateur FROM Utilisateurs "
                    + "WHERE mdpHashUtilisateur = 'hash_a_remplacer'");
                 ResultSet rs = sel.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getString(1));
                    logins.add(rs.getString(2));
                }
            }

            for (String id : ids) {
                try (PreparedStatement maj = cx.prepareStatement(
                        "UPDATE Utilisateurs SET mdpHashUtilisateur = ? WHERE idUtilisateur = ?")) {
                    maj.setString(1, MotDePasse.hacher(mdp));   // un sel different par compte
                    maj.setString(2, id);
                    maj.executeUpdate();
                }
            }
            if (!cx.getAutoCommit()) {
                cx.commit();
            }
        }
        System.out.println(ids.size() + " compte(s) mis a jour : " + logins);

        // Verification : on se connecte avec un compte d'essai
        if (!logins.isEmpty()) {
            // Mauvais mot de passe d'abord : la bonne connexion ensuite remet le compteur d'echecs a zero
            AuthentificationDAO.Resultat r2 = AuthentificationDAO.connecter(logins.get(0), mdp + "x");
            System.out.println("Connexion de '" + logins.get(0) + "' avec un mauvais mot de passe -> "
                    + (r2.succes ? "ACCEPTE (anormal !)" : "refuse (normal)") + " (" + r2.message + ")");
            AuthentificationDAO.Resultat r = AuthentificationDAO.connecter(logins.get(0), mdp);
            System.out.println("Connexion de '" + logins.get(0) + "' avec le bon mot de passe -> "
                    + (r.succes ? "OK" : "ECHEC") + " (" + r.message + ")");
        }
    }
}
