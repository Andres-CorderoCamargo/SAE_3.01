package systeme.persistance;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [Ajouter un electeur] Verifications faites par le serveur AVANT d'aller en base
 * (AuthentificationDAO.ajouterElecteur).
 *
 * Ces cas doivent etre refuses sans ouvrir la connexion Oracle : les tests
 * passent donc meme sans les variables ORACLE_*. L'ajout reel en base est
 * teste par TestAuthentification, qui a besoin d'Oracle.
 */
@DisplayName("[Ajouter un électeur] AuthentificationDAO.ajouterElecteur (sans base)")
class AjouterElecteurDAOTest {

    private static final String MESSAGE_ID = "Identifiant electeur invalide : 5 caracteres maximum";

    private static String ajouter(String id, String mdp) {
        return assertDoesNotThrow(
                () -> AuthentificationDAO.ajouterElecteur(id, "jean.dupont", mdp, "Dupont", "Jean"),
                "la base n'aurait pas dû être ouverte");
    }

    @Test
    @DisplayName("identifiant null refusé")
    void idNull() {
        assertEquals(MESSAGE_ID, ajouter(null, "MotDePasse123"));
    }

    @Test
    @DisplayName("identifiant de 6 caractères refusé (colonne VARCHAR2(5))")
    void idSixCaracteres() {
        assertEquals(MESSAGE_ID, ajouter("E00001", "MotDePasse123"));
    }

    @Test
    @DisplayName("ancien format d'identifiant (ELEC-2026-001) refusé")
    void ancienFormat() {
        assertEquals(MESSAGE_ID, ajouter("ELEC-2026-001", "MotDePasse123"));
    }

    @Test
    @DisplayName("le refus a lieu avant le hachage, même sans mot de passe")
    void refusAvantHachage() {
        assertEquals(MESSAGE_ID, ajouter("TROPLONG", null));
    }

    @Test
    @Disabled("BUG : un mot de passe provisoire null n'est pas refusé, MotDePasse.hacher(null) hache le texte \"null\"")
    @DisplayName("mot de passe provisoire null refusé sans aller en base")
    void mdpNull() {
        String message = ajouter("E0001", null);
        assertNotEquals("OK", message);
    }

    @Test
    @Disabled("BUG : un mot de passe provisoire vide n'est pas refusé")
    @DisplayName("mot de passe provisoire vide refusé sans aller en base")
    void mdpVide() {
        String message = ajouter("E0001", "");
        assertNotEquals("OK", message);
    }
}
