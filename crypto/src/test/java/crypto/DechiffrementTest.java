package crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [Déchiffrement] [Tester les primitives cryptographiques] Tests de
 * Dechiffrement.decrypt, avec les chiffrés produits par Chiffrement.encrypt.
 *
 * Pas besoin de réseau ni de base de données. Les clés de test font 64 bits :
 * c'est instantané, et bien trop petit pour un vrai scrutin.
 */
@DisplayName("[Déchiffrement] Dechiffrement.decrypt")
class DechiffrementTest {

    private static final ClefPrivee SK = OutilsCles.genererCles(64);
    private static final ClefPublique PK = SK.clefPublique();
    private static final BigInteger UN = BigInteger.ONE;

    private static BigInteger b(long n) {
        return BigInteger.valueOf(n);
    }

    @Nested
    @DisplayName("avec des chiffrés calculés à la main")
    class AlaMain {

        // Petit nombre premier sûr : p = 23 = 2 * 11 + 1, g = 4 (ordre 11),
        // x = 6, donc h = 4^6 mod 23 = 2.
        // Avec r = 3 : u = 4^3 mod 23 = 18, et h^r = 2^3 = 8, donc
        //   m = 0 : v = 8      m = 1 : v = 4 * 8 mod 23 = 9      m = 2 : v = 16 * 8 mod 23 = 13
        private final ClefPrivee sk = new ClefPrivee(b(23), b(4), b(6));
        private final ClefPublique pk = sk.clefPublique();

        @Test
        @DisplayName("la clé publique calculée vaut bien h = 2")
        void clefPublique() {
            assertEquals(b(2), pk.h);
        }

        @Test
        @DisplayName("l'encrypt de Roman donne les valeurs calculées à la main, et decrypt les retrouve")
        void accordAvecEncrypt() {
            long[] attendus = {8, 9, 13};
            for (int m = 0; m <= 2; m++) {
                Chiffre c = Chiffrement.encryptAvecR(b(m), b(2), pk, b(3));
                assertEquals(b(18), c.u, "u pour m = " + m);
                assertEquals(b(attendus[m]), c.v, "v pour m = " + m);
                assertEquals(b(m), Dechiffrement.decrypt(c, b(5), sk));
            }
        }

        @Test
        @DisplayName("la borne B est incluse")
        void borneIncluse() {
            Chiffre c = new Chiffre(b(18), b(13));      // m = 2
            assertEquals(b(2), Dechiffrement.decrypt(c, b(2), sk));
            assertThrows(IllegalArgumentException.class, () -> Dechiffrement.decrypt(c, b(1), sk));
        }

        @Test
        @DisplayName("accepte un chiffré non réduit modulo p")
        void chiffreNonReduit() {
            Chiffre c = new Chiffre(b(18 + 23 * 7), b(9 + 23 * 3));   // m = 1
            assertEquals(UN, Dechiffrement.decrypt(c, b(5), sk));
        }

        @Test
        @DisplayName("refuse un chiffré dont u est un multiple de p (non inversible)")
        void uNonInversible() {
            assertThrows(IllegalArgumentException.class,
                    () -> Dechiffrement.decrypt(new Chiffre(BigInteger.ZERO, b(9)), b(5), sk));
            assertThrows(IllegalArgumentException.class,
                    () -> Dechiffrement.decrypt(new Chiffre(b(23), b(9)), b(5), sk));
        }

        @Test
        @DisplayName("refuse une borne B négative")
        void borneNegative() {
            assertThrows(IllegalArgumentException.class,
                    () -> Dechiffrement.decrypt(new Chiffre(b(18), b(8)), b(-1), sk));
        }

        @Test
        @DisplayName("refuse un argument null")
        void argumentsNull() {
            Chiffre c = new Chiffre(b(18), b(8));
            assertThrows(NullPointerException.class, () -> Dechiffrement.decrypt(null, b(5), sk));
            assertThrows(NullPointerException.class, () -> Dechiffrement.decrypt(c, null, sk));
            assertThrows(NullPointerException.class, () -> Dechiffrement.decrypt(c, b(5), null));
            assertThrows(NullPointerException.class,
                    () -> Dechiffrement.decrypt(new Chiffre(null, b(8)), b(5), sk));
        }

        @Test
        @DisplayName("la clé privée n'affiche pas x dans toString()")
        void toStringMasqueX() {
            assertFalse(new ClefPrivee(b(23), b(4), b(987654)).toString().contains("987654"));
        }
    }

    @Nested
    @DisplayName("avec des bulletins chiffrés par Chiffrement.encrypt")
    class AvecEncrypt {

        @Test
        @DisplayName("un bulletin seul : Non donne 0, Oui donne 1")
        void bulletinSeul() {
            assertEquals(BigInteger.ZERO, Dechiffrement.decrypt(Chiffrement.encrypt(b(0), UN, PK), UN, SK));
            assertEquals(UN, Dechiffrement.decrypt(Chiffrement.encrypt(b(1), UN, PK), UN, SK));
        }

        @Test
        @DisplayName("deux bulletins agrégés donnent v1 + v2, pour toutes les paires de votes")
        void sommeDeDeuxVotes() {
            for (int v1 = 0; v1 <= 1; v1++) {
                for (int v2 = 0; v2 <= 1; v2++) {
                    Chiffre somme = OutilsCles.agreger(
                            Chiffrement.encrypt(b(v1), UN, PK), Chiffrement.encrypt(b(v2), UN, PK), PK.p);
                    assertEquals(b(v1 + v2), Dechiffrement.decrypt(somme, b(2), SK),
                            "v1 = " + v1 + ", v2 = " + v2);
                }
            }
        }

        @Test
        @DisplayName("200 bulletins : le total déchiffré est le nombre de Oui")
        void nombreuxBulletins() {
            Random hasard = new Random(42);
            int[] votes = new int[200];
            int oui = 0;
            for (int i = 0; i < votes.length; i++) {
                votes[i] = hasard.nextInt(2);
                oui += votes[i];
            }
            assertEquals(b(oui), Dechiffrement.decrypt(OutilsCles.urne(SK, votes), b(votes.length), SK));
        }

        @Test
        @DisplayName("tous Non donne 0, tous Oui donne le nombre d'inscrits")
        void casLimites() {
            int[] tousNon = new int[50];
            int[] tousOui = new int[50];
            Arrays.fill(tousOui, 1);
            assertEquals(b(0), Dechiffrement.decrypt(OutilsCles.urne(SK, tousNon), b(50), SK));
            assertEquals(b(50), Dechiffrement.decrypt(OutilsCles.urne(SK, tousOui), b(50), SK));
        }

        @Test
        @DisplayName("même total, que l'agrégation réduise modulo p ou non")
        void reduitOuNon() {
            Chiffre reduit = Chiffrement.encrypt(b(0), UN, PK);
            Chiffre exact = reduit;
            for (int i = 0; i < 30; i++) {
                Chiffre c = Chiffrement.encrypt(b(i % 2), UN, PK);
                reduit = OutilsCles.agreger(reduit, c, PK.p);
                exact = OutilsCles.agregerSansReduire(exact, c);
            }
            assertEquals(b(15), Dechiffrement.decrypt(reduit, b(30), SK));
            assertEquals(b(15), Dechiffrement.decrypt(exact, b(30), SK));
        }

        @Test
        @DisplayName("refuse un total plus grand que B, au lieu de rendre une valeur fausse")
        void totalAuDessusDeLaBorne() {
            Chiffre c = OutilsCles.urne(SK, 1, 1, 1, 1, 1);
            assertEquals(b(5), Dechiffrement.decrypt(c, b(5), SK));
            assertThrows(IllegalArgumentException.class, () -> Dechiffrement.decrypt(c, b(4), SK));
        }

        @Test
        @DisplayName("refuse de déchiffrer avec la mauvaise clé privée")
        void mauvaiseCle() {
            Chiffre c = OutilsCles.urne(SK, 1, 0, 1);
            ClefPrivee autre = OutilsCles.genererCles(64);
            assertThrows(IllegalArgumentException.class, () -> Dechiffrement.decrypt(c, b(10), autre));
        }

        @Test
        @DisplayName("marche quel que soit le générateur g de la clé (pas seulement g = 4)")
        void plusieursGenerateurs() {
            for (int i = 0; i < 5; i++) {
                ClefPrivee sk = OutilsCles.genererCles(64);
                Chiffre c = OutilsCles.urne(sk, 1, 1, 0, 1);
                assertEquals(b(3), Dechiffrement.decrypt(c, b(4), sk), "g = " + sk.g);
            }
        }
    }
}
