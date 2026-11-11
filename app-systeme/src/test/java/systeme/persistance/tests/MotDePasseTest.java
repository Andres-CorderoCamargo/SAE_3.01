package systeme.persistance.tests;

import systeme.persistance.MotDePasse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [S'authentifier] Tests sur l'implementation du hachage du mot de passe
 * (MotDePasse.hacher et MotDePasse.verifier), avec la methode du TD8 :
 * HMAC-SHA256 avec un poivre, puis bcrypt.
 *
 * Tests unitaires : pas besoin de la base Oracle, contrairement a
 * TestAuthentification, ni de la variable d'environnement SAE_POIVRE : on
 * passe un poivre d'essai (qui n'est pas un secret) aux variantes de
 * hacher() et verifier() qui l'acceptent en paramètre.
 */
@DisplayName("[S'authentifier] Hachage du mot de passe")
class MotDePasseTest {

    private static final String MDP = "MonMotDePasse123";

    /** Poivre d'essai : surtout pas le vrai poivre du serveur. */
    private static final String POIVRE = "poivre-d-essai-pas-un-secret";
    private static final String AUTRE_POIVRE = "un-autre-poivre-d-essai";

    private static String hacher(String mdp) {
        return MotDePasse.hacher(mdp, POIVRE);
    }

    private static boolean verifier(String mdp, String hash) {
        return MotDePasse.verifier(mdp, hash, POIVRE);
    }

    /**
     * Empreinte rapide (cout 4) pour les tests qui ne portent pas sur le
     * cout : meme methode que hacher(), seul le cout change.
     */
    private static String hashRapide(String mdp) {
        return BCrypt.hashpw(MotDePasse.poivrer(mdp, POIVRE), BCrypt.gensalt(4));
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
            String hash = hacher(MDP);
            assertTrue(hash.startsWith("$2a$12$"), "empreinte obtenue : " + hash);
        }

        @Test
        @DisplayName("l'empreinte fait 60 caractères et tient dans VARCHAR2(100)")
        void tientDansLaColonne() {
            String hash = hacher(MDP);
            assertEquals(60, hash.length());
            assertTrue(hash.getBytes(StandardCharsets.UTF_8).length <= 100);
        }

        @Test
        @DisplayName("ne contient jamais le mot de passe en clair")
        void pasDeClair() {
            assertFalse(hacher(MDP).contains(MDP));
        }

        @Test
        @DisplayName("deux hachages du même mot de passe sont différents (sel aléatoire)")
        void selAleatoire() {
            String h1 = hacher(MDP);
            String h2 = hacher(MDP);
            assertNotEquals(h1, h2);
            assertTrue(verifier(MDP, h1));
            assertTrue(verifier(MDP, h2));
        }

        @Test
        @DisplayName("gère les accents et caractères spéciaux")
        void caracteresSpeciaux() {
            String mdp = "Élève-Ç@fé_€ 2026!";
            assertTrue(verifier(mdp, hacher(mdp)));
        }

        @Test
        @DisplayName("refuse un mot de passe null")
        void refuseNull() {
            assertThrows(IllegalArgumentException.class, () -> hacher(null));
        }

        @Test
        @DisplayName("refuse un mot de passe vide")
        void refuseVide() {
            assertThrows(IllegalArgumentException.class, () -> hacher(""));
        }
    }

    @Nested
    @DisplayName("poivrer() : étape HMAC-SHA256 du TD8")
    class Poivrer {

        @Test
        @DisplayName("donne le vecteur de test 2 de la RFC 4231 (donc la même valeur que hash_hmac de PHP)")
        void vecteurRfc4231() {
            assertEquals("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
                    MotDePasse.poivrer("what do ya want for nothing?", "Jefe"));
        }

        @Test
        @DisplayName("donne 64 caractères hexadécimaux en minuscules")
        void format() {
            String resultat = MotDePasse.poivrer(MDP, POIVRE);
            assertTrue(resultat.matches("[0-9a-f]{64}"), "résultat : " + resultat);
        }

        @Test
        @DisplayName("est déterministe, et dépend du poivre")
        void deterministeEtDependantDuPoivre() {
            assertEquals(MotDePasse.poivrer(MDP, POIVRE), MotDePasse.poivrer(MDP, POIVRE));
            assertNotEquals(MotDePasse.poivrer(MDP, POIVRE), MotDePasse.poivrer(MDP, AUTRE_POIVRE));
        }

        @Test
        @DisplayName("hacher() = bcrypt du mot de passe poivré, pas du mot de passe seul")
        void ordreDesEtapes() {
            String hash = hacher(MDP);
            assertTrue(BCrypt.checkpw(MotDePasse.poivrer(MDP, POIVRE), hash));
            // Une empreinte sans poivre (ancienne méthode) n'est plus reconnue
            assertFalse(BCrypt.checkpw(MDP, hash));
            assertFalse(verifier(MDP, BCrypt.hashpw(MDP, BCrypt.gensalt(4))));
        }

        @Test
        @DisplayName("refuse un poivre absent ou vide (erreur de configuration, pas un mauvais mot de passe)")
        void poivreAbsent() {
            assertThrows(IllegalStateException.class, () -> MotDePasse.poivrer(MDP, null));
            assertThrows(IllegalStateException.class, () -> MotDePasse.poivrer(MDP, ""));
            assertThrows(IllegalStateException.class, () -> MotDePasse.hacher(MDP, null));
            assertThrows(IllegalStateException.class, () -> MotDePasse.hacher(MDP, ""));
            String hash = hashRapide(MDP);
            assertThrows(IllegalStateException.class, () -> MotDePasse.verifier(MDP, hash, null));
            assertThrows(IllegalStateException.class, () -> MotDePasse.verifier(MDP, hash, ""));
        }
    }

    @Nested
    @DisplayName("verifier()")
    class Verifier {

        @Test
        @DisplayName("accepte le bon mot de passe")
        void bonMotDePasse() {
            assertTrue(verifier(MDP, hacher(MDP)));
        }

        @Test
        @DisplayName("refuse un mauvais mot de passe")
        void mauvaisMotDePasse() {
            assertFalse(verifier("autreChose", hashRapide(MDP)));
        }

        @Test
        @DisplayName("refuse le bon mot de passe avec un autre poivre")
        void autrePoivre() {
            assertFalse(MotDePasse.verifier(MDP, hashRapide(MDP), AUTRE_POIVRE));
        }

        @Test
        @DisplayName("est sensible à la casse")
        void sensibleALaCasse() {
            assertFalse(verifier(MDP.toUpperCase(), hashRapide(MDP)));
        }

        @Test
        @DisplayName("refuse un espace en trop")
        void espaceEnTrop() {
            assertFalse(verifier(MDP + " ", hashRapide(MDP)));
        }

        @Test
        @DisplayName("refuse un mot de passe saisi vide")
        void saisieVide() {
            assertFalse(verifier("", hashRapide(MDP)));
        }

        @Test
        @DisplayName("refuse un mot de passe saisi null, même face à l'empreinte de \"null\"")
        void saisieNull() {
            // La garde de verifier() répond false avant tout calcul : pas d'exception.
            assertFalse(verifier(null, hashRapide("null")));
        }

        @Test
        @DisplayName("refuse une empreinte null (compte sans mot de passe)")
        void empreinteNull() {
            assertFalse(verifier(MDP, null));
        }

        @Test
        @DisplayName("refuse 'hash_a_remplacer' des données d'essai sans lever d'exception")
        void empreinteDonneesEssai() {
            assertDoesNotThrow(() -> verifier(MDP, "hash_a_remplacer"));
            assertFalse(verifier("hash_a_remplacer", "hash_a_remplacer"));
        }

        @Test
        @DisplayName("refuse un mot de passe stocké en clair (comparaison directe interdite)")
        void motDePasseEnClairEnBase() {
            assertFalse(verifier(MDP, MDP));
        }

        @Test
        @DisplayName("refuse sans planter une empreinte $2b$ ou $2y$ (non gérée par jbcrypt 0.4)")
        void autresVariantes() {
            String hash = hashRapide(MDP);
            for (String variante : new String[]{"$2b$", "$2y$"}) {
                String autre = hash.replace("$2a$", variante);
                assertDoesNotThrow(() -> verifier(MDP, autre), variante);
                assertFalse(verifier(MDP, autre), variante);
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
                assertDoesNotThrow(() -> verifier(MDP, c), "empreinte : " + c);
                assertFalse(verifier(MDP, c), "empreinte : " + c);
            }
        }

        @Test
        @DisplayName("refuse si un seul caractère de l'empreinte est modifié")
        void empreinteModifiee() {
            String hash = hashRapide(MDP);
            char dernier = hash.charAt(hash.length() - 1);
            String modifie = hash.substring(0, hash.length() - 1) + (dernier == 'a' ? 'b' : 'a');
            assertFalse(verifier(MDP, modifie));
        }

        @Test
        @DisplayName("prend en compte les mots de passe de plus de 72 octets en entier")
        void plusDeLimiteDe72Octets() {
            // bcrypt seul ignore tout après 72 octets. Comme il reçoit ici un HMAC
            // de 64 caractères, la fin du mot de passe compte désormais.
            String debut = repeter('a', 72);
            String hash = hashRapide(debut + "FIN_1");
            assertTrue(verifier(debut + "FIN_1", hash));
            assertFalse(verifier(debut + "FIN_2", hash));
        }
    }

    @Nested
    @DisplayName("verifierAvecRotation() : changement de poivre")
    class Rotation {

        private static final String ANCIEN = "ancien-poivre-d-essai";
        private static final String NOUVEAU = "nouveau-poivre-d-essai";

        private String hashAvec(String mdp, String poivre) {
            return BCrypt.hashpw(MotDePasse.poivrer(mdp, poivre), BCrypt.gensalt(4));
        }

        private MotDePasse.Verification verif(String mdp, String hash, String poivre, String ancien) {
            return MotDePasse.verifierAvecRotation(mdp, hash, poivre, ancien);
        }

        @Test
        @DisplayName("empreinte faite avec le poivre actuel : VALIDE")
        void poivreActuel() {
            String hash = hashAvec(MDP, NOUVEAU);
            assertEquals(MotDePasse.Verification.VALIDE, verif(MDP, hash, NOUVEAU, ANCIEN));
            assertEquals(MotDePasse.Verification.VALIDE, verif(MDP, hash, NOUVEAU, null));
        }

        @Test
        @DisplayName("empreinte faite avec l'ancien poivre, ancien fourni : VALIDE_A_REHACHER")
        void ancienPoivreConnu() {
            String hash = hashAvec(MDP, ANCIEN);
            assertEquals(MotDePasse.Verification.VALIDE_A_REHACHER, verif(MDP, hash, NOUVEAU, ANCIEN));
        }

        @Test
        @DisplayName("empreinte faite avec l'ancien poivre, ancien oublié : INVALIDE")
        void ancienPoivreInconnu() {
            String hash = hashAvec(MDP, ANCIEN);
            assertEquals(MotDePasse.Verification.INVALIDE, verif(MDP, hash, NOUVEAU, null));
            assertEquals(MotDePasse.Verification.INVALIDE, verif(MDP, hash, NOUVEAU, ""));
        }

        @Test
        @DisplayName("mauvais mot de passe : INVALIDE, même avec les deux poivres")
        void mauvaisMotDePasse() {
            assertEquals(MotDePasse.Verification.INVALIDE,
                    verif("MauvaisMdp", hashAvec(MDP, NOUVEAU), NOUVEAU, ANCIEN));
            assertEquals(MotDePasse.Verification.INVALIDE,
                    verif("MauvaisMdp", hashAvec(MDP, ANCIEN), NOUVEAU, ANCIEN));
        }

        @Test
        @DisplayName("ancien poivre identique au nouveau : traité comme pas de changement")
        void ancienEgalNouveau() {
            assertEquals(MotDePasse.Verification.VALIDE,
                    verif(MDP, hashAvec(MDP, NOUVEAU), NOUVEAU, NOUVEAU));
            assertEquals(MotDePasse.Verification.INVALIDE,
                    verif(MDP, hashAvec(MDP, ANCIEN), NOUVEAU, NOUVEAU));
        }

        @Test
        @DisplayName("après re-hachage, le nouveau poivre seul suffit")
        void apresMigration() {
            String ancienHash = hashAvec(MDP, ANCIEN);
            assertEquals(MotDePasse.Verification.VALIDE_A_REHACHER, verif(MDP, ancienHash, NOUVEAU, ANCIEN));
            String nouveauHash = MotDePasse.hacher(MDP, NOUVEAU);   // ce que fait le DAO
            assertEquals(MotDePasse.Verification.VALIDE, verif(MDP, nouveauHash, NOUVEAU, null));
            assertFalse(MotDePasse.verifier(MDP, nouveauHash, ANCIEN), "l'ancien poivre ne doit plus marcher");
        }

        @Test
        @DisplayName("poivre actuel absent : erreur de configuration, même si l'ancien est là")
        void poivreActuelAbsent() {
            String hash = hashAvec(MDP, ANCIEN);
            assertThrows(IllegalStateException.class, () -> verif(MDP, hash, null, ANCIEN));
            assertThrows(IllegalStateException.class, () -> verif(MDP, hash, "", ANCIEN));
        }

        @Test
        @DisplayName("empreinte absente ou corrompue : INVALIDE sans exception")
        void empreinteAbsente() {
            assertEquals(MotDePasse.Verification.INVALIDE, verif(MDP, null, NOUVEAU, ANCIEN));
            assertEquals(MotDePasse.Verification.INVALIDE, verif(MDP, "hash_a_remplacer", NOUVEAU, ANCIEN));
        }
    }
}
