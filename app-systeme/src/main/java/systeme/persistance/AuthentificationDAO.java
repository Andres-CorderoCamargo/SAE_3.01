package systeme.persistance;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Comptes et connexion.
 *
 * Le mot de passe est hache ICI, dans le serveur, jamais dans le client.
 * BCrypt ajoute un sel different a chaque fois, donc deux hachages du
 * meme mot de passe ne se ressemblent pas. On ne compare donc jamais
 * deux empreintes avec equals : on utilise BCrypt.checkpw.
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
     * Connecte un utilisateur.
     *
     * @param identifiant     le login saisi
     * @param motDePasse      le mot de passe saisi, en clair
     */
    public static Resultat connecter(String identifiant, String motDePasse)
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
            boolean mdpBon = false;
            if (empreinte != null) {
                mdpBon = BCrypt.checkpw(motDePasse, empreinte);
            }

            // 3. Compte inconnu, desactive ou mauvais mot de passe :
            if (idUtilisateur == null || actif == 0 || !mdpBon) {
                resultat.message = appelerEchecConnexion(cx, identifiant);
                return resultat;
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

        String empreinte = BCrypt.hashpw(motDePasse, BCrypt.gensalt(12));

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
}
