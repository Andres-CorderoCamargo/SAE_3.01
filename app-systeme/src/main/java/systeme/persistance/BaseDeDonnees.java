package systeme.persistance;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Connexion a la base Oracle de l'IUT.
 *
 * L'URL, le login et le mot de passe ne doivent JAMAIS etre ecrits en dur
 * dans le code ni pousses sur GitHub. On les lit dans des variables
 * d'environnement :
 *
 *   export ORACLE_URL="jdbc:oracle:thin:@orainfo.iutmontp.univ-montp2.fr:1521/IUT"
 *   export ORACLE_USER="votre_login_iut"
 *   export ORACLE_PASSWORD="votre_mot_de_passe"
 *
 * Attention : le port 1521 n'est pas le 5560 de la page iSQL*Plus. Le 5560
 * sert au site web, le 1521 au listener Oracle, c'est celui que JDBC appelle.
 * Le nom du service est IUT : c'est ce qui suit le @ quand iSQL*Plus affiche
 * la connexion.
 */
public class BaseDeDonnees {

    private BaseDeDonnees() {
    }

    public static Connection ouvrir() throws SQLException {
        String url = System.getenv("ORACLE_URL");
        String utilisateur = System.getenv("ORACLE_USER");
        String motDePasse = System.getenv("ORACLE_PASSWORD");

        if (url == null || utilisateur == null || motDePasse == null) {
            throw new SQLException(
                "Variables ORACLE_URL, ORACLE_USER et ORACLE_PASSWORD non definies");
        }

        return DriverManager.getConnection(url, utilisateur, motDePasse);
    }
}
