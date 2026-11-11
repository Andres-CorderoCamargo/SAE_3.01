package systeme.persistance;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Comptes et connexion.
 *
 * Le mot de passe est hache dans le serveur, jamais dans le client.
 * Le hachage et la verification passent par la classe MotDePasse, qui est
 * la seule a connaitre bcrypt et le poivre (variable d'environnement
 * SAE_POIVRE, a definir avant de lancer le serveur ; SAE_POIVRE_ANCIEN en
 * plus pendant un changement de poivre).
 */
public class AuthentificationDAO {

    /** Ce que renvoie une tentative de connexion. */
    public static class Resultat {
        public boolean succes;
        public String idUtilisateur;
        public String role;            // ELECTEUR, ORGANISATEUR ou SCRUTATEUR
        public boolean doitChangerMdp;
        public String message;
    }

    /**
     * Connecte un utilisateur, avec le poivre actuel (SAE_POIVRE) et, pendant
     * un changement de poivre, l'ancien (SAE_POIVRE_ANCIEN).
     *
     * @param identifiant     le login saisi
     * @param motDePasse      le mot de passe saisi, en clair
     * @throws IllegalStateException si SAE_POIVRE n'est pas definie
     */
    public static Resultat connecter(String identifiant, String motDePasse)
            throws SQLException {
        return connecter(identifiant, motDePasse,
                System.getenv(MotDePasse.VARIABLE_POIVRE),
                System.getenv(MotDePasse.VARIABLE_ANCIEN_POIVRE));
    }

    /**
     * Comme connecter(String, String), avec les poivres donnes (utile pour
     * tester un changement de poivre sans relancer le programme).
     *
     * Si le mot de passe est bon mais que l'empreinte a ete faite avec
     * l'ancien poivre, on la remplace par une empreinte faite avec le poivre
     * actuel : c'est la migration au fil des connexions.
     *
     * @param ancienPoivre null s'il n'y a pas de changement de poivre en cours
     */
    public static Resultat connecter(String identifiant, String motDePasse,
                                     String poivre, String ancienPoivre)
            throws SQLException {

        Resultat resultat = new Resultat();
        resultat.succes = false;

        Connection cx = BaseDeDonnees.ouvrir();
        try {
            // 1. On cherche le compte
            String idUtilisateur = null;
            String empreinte = null;
            String role = null;
            int actif = 0;

            PreparedStatement req = cx.prepareStatement(
                "SELECT idUtilisateur, mdpHashUtilisateur, roleUtilisateur, actifUtilisateur "
              + "FROM Utilisateurs WHERE identifiantUtilisateur = ?");
            req.setString(1, identifiant);

            ResultSet rs = req.executeQuery();
            if (rs.next()) {
                idUtilisateur = rs.getString("idUtilisateur");
                empreinte = rs.getString("mdpHashUtilisateur");
                role = rs.getString("roleUtilisateur");
                actif = rs.getInt("actifUtilisateur");
            }
            rs.close();
            req.close();

            // 2. On verifie le mot de passe
            MotDePasse.Verification verif = MotDePasse.Verification.INVALIDE;
            if (empreinte != null) {
                verif = MotDePasse.verifierAvecRotation(motDePasse, empreinte, poivre, ancienPoivre);
            }
            boolean mdpBon = verif != MotDePasse.Verification.INVALIDE;

            // 3. Compte inconnu, desactive ou mauvais mot de passe :
            //    meme reponse dans les trois cas, pour ne pas dire
            //    a un curieux quels identifiants existent.
            if (idUtilisateur == null || actif == 0 || !mdpBon) {
                resultat.message = appelerEchecConnexion(cx, identifiant);
                return resultat;
            }

            // 3 bis. Changement de poivre en cours : l'empreinte a ete faite
            //        avec l'ancien poivre. On a le mot de passe en clair, c'est
            //        le moment de la refaire avec le poivre actuel.
            if (verif == MotDePasse.Verification.VALIDE_A_REHACHER) {
                rehacher(cx, idUtilisateur, empreinte, MotDePasse.hacher(motDePasse, poivre));
            }

            // 4. Mot de passe bon : la procedure remet le compteur a zero
            //    et dit si le compte est bloque ou s'il faut changer le mdp
            String message = appelerSuccesConnexion(cx, identifiant);
            resultat.message = message;

            if (message.equals("OK") || message.equals("CHANGER_MDP")) {
                resultat.succes = true;
                resultat.idUtilisateur = idUtilisateur;
                resultat.role = role;
                resultat.doitChangerMdp = message.equals("CHANGER_MDP");
            }
            return resultat;

        } finally {
            cx.close();
        }
    }

    /**
     * Remplace l'empreinte d'un compte (changement de poivre). La condition sur
     * l'ancienne empreinte evite d'ecraser un mot de passe change entre temps.
     * Un echec ici n'empeche pas la connexion : la mise a jour sera retentee
     * a la prochaine connexion.
     */
    private static void rehacher(Connection cx, String idUtilisateur,
                                 String ancienneEmpreinte, String nouvelleEmpreinte) {
        try (PreparedStatement maj = cx.prepareStatement(
                "UPDATE Utilisateurs SET mdpHashUtilisateur = ? "
              + "WHERE idUtilisateur = ? AND mdpHashUtilisateur = ?")) {
            maj.setString(1, nouvelleEmpreinte);
            maj.setString(2, idUtilisateur);
            maj.setString(3, ancienneEmpreinte);
            int n = maj.executeUpdate();
            if (!cx.getAutoCommit()) {
                cx.commit();
            }
            if (n == 1) {
                System.out.println("[SERVEUR] Empreinte de " + idUtilisateur
                        + " refaite avec le nouveau poivre");
            }
        } catch (SQLException e) {
            System.err.println("[SERVEUR] Re-hachage impossible pour " + idUtilisateur
                    + " : " + e.getMessage());
        }
    }

    /**
     * Ajoute un electeur. Seul l'organisateur doit pouvoir appeler cette
     * methode : c'est a l'appelant d'avoir verifie le role.
     *
     * @param idUtilisateur  5 caracteres maximum
     * @param motDePasse     mot de passe provisoire, en clair
     * @return "OK", ou le message qui explique le refus
     */
    public static String ajouterElecteur(String idUtilisateur, String identifiant,
                                         String motDePasse, String nom, String prenom)
            throws SQLException {

        if (idUtilisateur == null || idUtilisateur.length() > 5) {
            return "Identifiant electeur invalide : 5 caracteres maximum";
        }

        // On refuse proprement avant de hacher : hacher() leverait une
        // exception sur un mot de passe vide (trouve par Zakaria).
        if (motDePasse == null || motDePasse.isEmpty()) {
            return "Mot de passe provisoire obligatoire";
        }

        String empreinte = MotDePasse.hacher(motDePasse);

        Connection cx = BaseDeDonnees.ouvrir();
        try {
            CallableStatement proc = cx.prepareCall(
                "{ call ajouterElecteur(?, ?, ?, ?, ?, ?) }");
            proc.setString(1, idUtilisateur);
            proc.setString(2, identifiant);
            proc.setString(3, empreinte);
            proc.setString(4, nom);
            proc.setString(5, prenom);
            proc.registerOutParameter(6, Types.VARCHAR);
            proc.execute();

            String message = proc.getString(6);
            proc.close();
            return message;

        } finally {
            cx.close();
        }
    }


    /**
     * Supprime un electeur. Seul l'organisateur doit pouvoir appeler cette
     * methode : c'est a l'appelant d'avoir verifie le role.
     *
     * Si l'electeur a deja vote, la procedure ne le supprime pas : elle
     * desactive son compte (pour garder la trace "un electeur, une voix").
     *
     * @param idUtilisateur  identifiant de l'electeur (5 caracteres maximum)
     * @return "OK", ou le message qui explique le refus ou la desactivation
     */
    public static String supprimerElecteur(String idUtilisateur) throws SQLException {

        if (idUtilisateur == null || idUtilisateur.trim().isEmpty() || idUtilisateur.length() > 5) {
            return "Identifiant electeur invalide : 5 caracteres maximum";
        }

        Connection cx = BaseDeDonnees.ouvrir();
        try {
            CallableStatement proc = cx.prepareCall("{ call supprimerElecteur(?, ?) }");
            proc.setString(1, idUtilisateur);
            proc.registerOutParameter(2, Types.VARCHAR);
            proc.execute();

            String message = proc.getString(2);
            proc.close();
            return message;

        } finally {
            cx.close();
        }
    }

    private static String appelerSuccesConnexion(Connection cx, String identifiant)
            throws SQLException {
        CallableStatement proc = cx.prepareCall("{ call succesConnexion(?, ?) }");
        proc.setString(1, identifiant);
        proc.registerOutParameter(2, Types.VARCHAR);
        proc.execute();
        String message = proc.getString(2);
        proc.close();
        return message;
    }

    private static String appelerEchecConnexion(Connection cx, String identifiant)
            throws SQLException {
        CallableStatement proc = cx.prepareCall("{ call echecConnexion(?, ?) }");
        proc.setString(1, identifiant);
        proc.registerOutParameter(2, Types.VARCHAR);
        proc.execute();
        String message = proc.getString(2);
        proc.close();
        return message;
    }


    public static String modifierElecteur(String idUtilisateur, String identifiant,
                                          String nom, String prenom)
            throws SQLException {

        if (idUtilisateur == null || idUtilisateur.trim().isEmpty() || idUtilisateur.length() > 5) {
            return "Identifiant electeur invalide : 5 caracteres maximum";
        }
        if (identifiant == null || identifiant.trim().isEmpty() || identifiant.length() > 25) {
            return "Login invalide : 25 caracteres maximum";
        }
        if (nom == null || nom.trim().isEmpty() || nom.length() > 25
                || prenom == null || prenom.trim().isEmpty() || prenom.length() > 25) {
            return "Nom et prenom obligatoires : 25 caracteres maximum";
        }

        Connection cx = BaseDeDonnees.ouvrir();
        try {
            CallableStatement proc = cx.prepareCall("{ call modifierElecteur(?, ?, ?, ?, ?) }");
            proc.setString(1, idUtilisateur);
            proc.setString(2, identifiant);
            proc.setString(3, nom);
            proc.setString(4, prenom);
            proc.registerOutParameter(5, Types.VARCHAR);
            proc.execute();

            String message = proc.getString(5);
            proc.close();
            return message;

        } finally {
            cx.close();
        }
    }
    public static String inscrireElecteurReferendum(String idReferendum, String idUtilisateur)
            throws SQLException {

        if (idReferendum == null || idReferendum.trim().isEmpty() || idReferendum.length() > 5) {
            return "Identifiant referendum invalide : 5 caracteres maximum";
        }
        if (idUtilisateur == null || idUtilisateur.trim().isEmpty() || idUtilisateur.length() > 5) {
            return "Identifiant electeur invalide : 5 caracteres maximum";
        }

        Connection cx = BaseDeDonnees.ouvrir();
        try {
            CallableStatement proc = cx.prepareCall(
                "{ call inscrireElecteurReferendum(?, ?, ?) }");
            proc.setString(1, idReferendum);
            proc.setString(2, idUtilisateur);
            proc.registerOutParameter(3, Types.VARCHAR);
            proc.execute();

            String message = proc.getString(3);
            proc.close();
            return message;

        } finally {
            cx.close();
        }
    }
}
