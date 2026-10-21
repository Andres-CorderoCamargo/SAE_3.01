package systeme.persistance.tests;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [S'authentifier] Tests sur l'implementation du hachage du mot de passe
 * (MotDePasse.hacher et MotDePasse.verifier).
 *
 * Tests unitaires : pas besoin de la base Oracle, contrairement a
 * TestAuthentification.
 */
@DisplayName("[S'authentifier] Hachage du mot de passe")
class MotDePasseTest {

    private static final String MDP = "MonMotDePasse123";

    /** Empreinte rapide (cout 4) pour les tests qui ne portent pas sur le cout. */
    private static String hashRapide(String mdp) {
        return BCrypt.hashpw(mdp, BCrypt.gensalt(4));
    }

    /** "a".repeat(n) n'existe qu'a partir de Java 11 : le module compile en Java 8. */
    private static String repeter(char c, int n) {
        char[] t = new char[n];
        Arrays.fill(t, c);
        return new String(t);
    }

    @Nested
    @DisplayName("hacher()")
    class Hacher {

        @Test
        @DisplayName("produit une empreinte BCrypt $2a$ au coût 12")
        void formatEtCout() {
            String hash = MotDePasse.hacher(MDP);
            assertTrue(hash.startsWith("$2a$12$"), "empreinte obtenue : " + hash);
        }

        @Test
        @DisplayName("l'empreinte fait 60 caractères et tient dans VARCHAR2(100)")
        void tientDansLaColonne() {
            String hash = MotDePasse.hacher(MDP);
            assertEquals(60, hash.length());
            assertTrue(hash.getBytes(StandardCharsets.UTF_8).length <= 100);
        }

        @Test
        @DisplayName("ne contient jamais le mot de passe en clair")
        void pasDeClair() {
            assertFalse(MotDePasse.hacher(MDP).contains(MDP));
        }

        @Test
        @DisplayName("deux hachages du même mot de passe sont différents (sel aléatoire)")
        void selAleatoire() {
            String h1 = MotDePasse.hacher(MDP);
            String h2 = MotDePasse.hacher(MDP);
            assertNotEquals(h1, h2);
            assertTrue(MotDePasse.verifier(MDP, h1));
            assertTrue(MotDePasse.verifier(MDP, h2));
        }

        @Test
        @DisplayName("gère les accents et caractères spéciaux")
        void caracteresSpeciaux() {
            String mdp = "Élève-Ç@fé_€ 2026!";
            assertTrue(MotDePasse.verifier(mdp, MotDePasse.hacher(mdp)));
        }

        @Test
        @DisplayName("refuse un mot de passe null")
        void refuseNull() {
            assertThrows(IllegalArgumentException.class, () -> MotDePasse.hacher(null));
        }

        @Test
        @DisplayName("refuse un mot de passe vide")
        void refuseVide() {
            assertThrows(IllegalArgumentException.class, () -> MotDePasse.hacher(""));
        }
    }

    @Nested
    @DisplayName("verifier()")
    class Verifier {

        @Test
        @DisplayName("accepte le bon mot de passe")
        void bonMotDePasse() {
            assertTrue(MotDePasse.verifier(MDP, MotDePasse.hacher(MDP)));
        }

        @Test
        @DisplayName("refuse un mauvais mot de passe")
        void mauvaisMotDePasse() {
            assertFalse(MotDePasse.verifier("autreChose", hashRapide(MDP)));
        }

        @Test
        @DisplayName("est sensible à la casse")
        void sensibleALaCasse() {
            assertFalse(MotDePasse.verifier(MDP.toUpperCase(), hashRapide(MDP)));
        }

        @Test
        @DisplayName("refuse un espace en trop")
        void espaceEnTrop() {
            assertFalse(MotDePasse.verifier(MDP + " ", hashRapide(MDP)));
        }

        @Test
        @DisplayName("refuse un mot de passe saisi vide")
        void saisieVide() {
            assertFalse(MotDePasse.verifier("", hashRapide(MDP)));
        }

        @Test
        @DisplayName("refuse un mot de passe saisi null, même face à l'empreinte de \"null\"")
        void saisieNull() {
            // Sans la garde, jbcrypt transforme null en "null" et dirait true ici.
            assertFalse(MotDePasse.verifier(null, hashRapide("null")));
        }

        @Test
        @DisplayName("refuse une empreinte null (compte sans mot de passe)")
        void empreinteNull() {
            assertFalse(MotDePasse.verifier(MDP, null));
        }

        @Test
        @DisplayName("refuse 'hash_a_remplacer' des données d'essai sans lever d'exception")
        void empreinteDonneesEssai() {
            assertDoesNotThrow(() -> MotDePasse.verifier(MDP, "hash_a_remplacer"));
            assertFalse(MotDePasse.verifier("hash_a_remplacer", "hash_a_remplacer"));
        }

        @Test
        @DisplayName("refuse un mot de passe stocké en clair (comparaison directe interdite)")
        void motDePasseEnClairEnBase() {
            assertFalse(MotDePasse.verifier(MDP, MDP));
        }

        @Test
        @DisplayName("refuse sans planter une empreinte $2b$ ou $2y$ (non gérée par jbcrypt 0.4)")
        void autresVariantes() {
            String hash = hashRapide(MDP);
            for (String variante : new String[]{"$2b$", "$2y$"}) {
                String autre = hash.replace("$2a$", variante);
                assertDoesNotThrow(() -> MotDePasse.verifier(MDP, autre), variante);
                assertFalse(MotDePasse.verifier(MDP, autre), variante);
            }
        }

        @Test
        @DisplayName("refuse une empreinte tronquée ou corrompue sans lever d'exception")
        void empreinteCorrompue() {
            String hash = hashRapide(MDP);
            String[] corrompues = {
                "$2",
                "$2a$",
                "$2a$12$",
                "$2a$xx$abcdefghijklmnopqrstuv",
                hash.substring(0, 20),
            };
            for (String c : corrompues) {
                assertDoesNotThrow(() -> MotDePasse.verifier(MDP, c), "empreinte : " + c);
                assertFalse(MotDePasse.verifier(MDP, c), "empreinte : " + c);
            }
        }

        @Test
        @DisplayName("refuse si un seul caractère de l'empreinte est modifié")
        void empreinteModifiee() {
            String hash = hashRapide(MDP);
            char dernier = hash.charAt(hash.length() - 1);
            String modifie = hash.substring(0, hash.length() - 1) + (dernier == 'a' ? 'b' : 'a');
            assertFalse(MotDePasse.verifier(MDP, modifie));
        }

        @Test
        @DisplayName("limite connue de BCrypt : seuls les 72 premiers octets comptent")
        void limiteDe72Octets() {
            String debut = repeter('a', 72);
            String hash = hashRapide(debut + "FIN_1");
            // Documente le comportement : au-delà de 72 octets, BCrypt ignore la suite.
            assertTrue(MotDePasse.verifier(debut + "FIN_2", hash));
        }
    }
}
