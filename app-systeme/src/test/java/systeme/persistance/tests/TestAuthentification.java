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

// Tests de l'authentification. Ils écrivent dans la vraie base, donc ils
// créent leurs comptes et les effacent après (on peut les relancer).
public class TestAuthentification {

    private static int ok = 0, ko = 0;

    // Comptes d'essai, effacés à la fin
    private static final String ID1 = "T901";
    private static final String LOGIN1 = "test_auth_1";
    private static final String ID2 = "T902";
    private static final String LOGIN2 = "test_auth_2";
    private static final String MDP = "MotDePasse123";

    public static void main(String[] args) throws SQLException {
        nettoyer();   // au cas où un essai précédent se serait arrêté en plein milieu

        testsHachage();
        testsAjout();
        testsConnexion();
        testsBlocage();

        nettoyer();

        System.out.println("\nRésultat : " + ok + " OK, " + ko + " KO");
    }

    private static void testsHachage() {
        System.out.println("================ Début test hachage ================");

        String empreinte1 = MotDePasse.hacher(MDP);
        String empreinte2 = MotDePasse.hacher(MDP);

        verifier("Deux hachages du même mdp sont différents",
                !empreinte1.equals(empreinte2));
        verifier("L'empreinte ne contient pas le mot de passe",
                !empreinte1.contains(MDP));

        verifier("Bon mot de passe accepté",
                MotDePasse.verifier(MDP, empreinte1));
        verifier("Le 2e hachage marche aussi",
                MotDePasse.verifier(MDP, empreinte2));
        verifier("Mauvais mot de passe refusé",
                !MotDePasse.verifier("MotDePasse124", empreinte1));
        verifier("Mot de passe vide refusé",
                !MotDePasse.verifier("", empreinte1));

        // Doivent refuser au lieu de planter
        verifier("Mot de passe null refusé",
                !MotDePasse.verifier(null, empreinte1));
        verifier("Empreinte null refusée",
                !MotDePasse.verifier(MDP, null));
        verifier("Texte qui n'est pas une empreinte refusé",
                !MotDePasse.verifier(MDP, "pas une empreinte"));
        verifier("Empreinte coupée en deux refusée",
                !MotDePasse.verifier(MDP, empreinte1.substring(0, 20)));

        System.out.println("================ Fin test hachage ================");
    }

    private static void testsAjout() throws SQLException {
        System.out.println("\n================ Début test ajout ================");

        String message = AuthentificationDAO.ajouterElecteur(
                ID1, LOGIN1, MDP, "Martin", "Léa");
        verifier("Ajout correct -> " + message, message.equals("OK"));

        message = AuthentificationDAO.ajouterElecteur(
                "T903", LOGIN1, MDP, "Durand", "Paul");
        verifier("Identifiant déjà pris -> " + message,
                message.equals("Cet identifiant est deja utilise"));

        // La colonne fait 5 caractères, on refuse sans aller en base
        message = AuthentificationDAO.ajouterElecteur(
                "ELEC-2026-001", "paul", MDP, "Durand", "Paul");
        verifier("Identifiant de plus de 5 caractères refusé -> " + message,
                message.startsWith("Identifiant electeur invalide"));

        message = AuthentificationDAO.ajouterElecteur(
                null, "paul", MDP, "Durand", "Paul");
        verifier("Identifiant null refusé -> " + message,
                message.startsWith("Identifiant electeur invalide"));

        verifier("Le mot de passe est haché en base", mdpEstHacheEnBase(LOGIN1));

        System.out.println("================ Fin test ajout ================");
    }

    private static void testsConnexion() throws SQLException {
        System.out.println("\n================ Début test connexion ================");

        AuthentificationDAO.Resultat r =
                AuthentificationDAO.connecter(LOGIN1, MDP);
        verifier("Bon mot de passe -> connexion acceptée", r.succes);
        verifier("Le rôle est ELECTEUR", "ELECTEUR".equals(r.role));
        verifier("L'identifiant renvoyé est le bon", ID1.equals(r.idUtilisateur));
        verifier("1re connexion : il faut changer le mot de passe",
                r.doitChangerMdp);

        AuthentificationDAO.Resultat mauvais =
                AuthentificationDAO.connecter(LOGIN1, "MotDePasse124");
        verifier("Mauvais mot de passe -> connexion refusée", !mauvais.succes);
        verifier("Aucun rôle n'est renvoyé", mauvais.role == null);

        AuthentificationDAO.Resultat inconnu =
                AuthentificationDAO.connecter("compte_qui_nexiste_pas", MDP);
        verifier("Compte inexistant -> connexion refusée", !inconnu.succes);

        // Si les deux messages étaient différents, on saurait quels comptes existent
        verifier("Même message pour mauvais mdp et compte inconnu",
                mauvais.message.equals(inconnu.message));

        AuthentificationDAO.Resultat apres =
                AuthentificationDAO.connecter(LOGIN1, MDP);
        verifier("Connexion possible après des échecs", apres.succes);

        System.out.println("================ Fin test connexion ================");
    }

    // Scénario A2 du dossier d'analyse
    private static void testsBlocage() throws SQLException {
        System.out.println("\n================ Début test blocage ================");

        String message = AuthentificationDAO.ajouterElecteur(
                ID2, LOGIN2, MDP, "Dupont", "Marc");
        verifier("Compte d'essai créé -> " + message, message.equals("OK"));

        AuthentificationDAO.connecter(LOGIN2, "faux1");
        AuthentificationDAO.connecter(LOGIN2, "faux2");
        AuthentificationDAO.Resultat troisieme =
                AuthentificationDAO.connecter(LOGIN2, "faux3");
        verifier("3e échec -> compte bloqué : " + troisieme.message,
                troisieme.message.contains("bloque"));

        AuthentificationDAO.Resultat bonMdp =
                AuthentificationDAO.connecter(LOGIN2, MDP);
        verifier("Compte bloqué : le bon mdp est refusé aussi", !bonMdp.succes);
        verifier("Le message dit jusqu'à quand : " + bonMdp.message,
                bonMdp.message.startsWith("Compte bloque jusqu'a"));

        System.out.println("================ Fin test blocage ================");
    }

    private static boolean mdpEstHacheEnBase(String identifiant)
            throws SQLException {
        Connection cx = BaseDeDonnees.ouvrir();
        try {
            PreparedStatement req = cx.prepareStatement(
                "SELECT mdpHashUtilisateur FROM Utilisateurs "
                + "WHERE identifiantUtilisateur = ?");
            req.setString(1, identifiant);
            ResultSet rs = req.executeQuery();
            String empreinte = null;
            if (rs.next()) {
                empreinte = rs.getString("mdpHashUtilisateur");
            }
            rs.close();
            req.close();
            return empreinte != null
                && !empreinte.equals(MDP)
                && MotDePasse.verifier(MDP, empreinte);
        } finally {
            cx.close();
        }
    }

    // Ces électeurs n'ont voté nulle part, donc supprimerElecteur les efface
    // vraiment. S'ils n'existent pas elle le dit et on continue.
    private static void nettoyer() throws SQLException {
        Connection cx = BaseDeDonnees.ouvrir();
        try {
            supprimer(cx, ID1);
            supprimer(cx, ID2);
            supprimer(cx, "T903");
        } finally {
            cx.close();
        }
    }

    private static void supprimer(Connection cx, String idUtilisateur)
            throws SQLException {
        CallableStatement proc = cx.prepareCall(
            "{ call supprimerElecteur(?, ?) }");
        proc.setString(1, idUtilisateur);
        proc.registerOutParameter(2, Types.VARCHAR);
        proc.execute();
        proc.close();
    }

    private static void verifier(String nom, boolean condition) {
        if (condition) {
            ok++;
            System.out.println("[OK] " + nom);
        } else {
            ko++;
            System.out.println("[KO] " + nom);
        }
    }
}
