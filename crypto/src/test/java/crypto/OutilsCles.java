package crypto;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Aide pour les TESTS uniquement : génère des clés et agrège des chiffrés.
 *
 * Ce n'est PAS KeyGen ni Agrege : ces primitives appartiennent à d'autres
 * cartes et ne sont encore dans aucune branche. Cette classe est dans
 * src/test, elle n'est donc jamais livrée avec l'application. Le chiffrement,
 * lui, utilise le vrai Chiffrement.encrypt de Roman.
 */
public final class OutilsCles {

    private static final BigInteger UN = BigInteger.ONE;
    private static final SecureRandom ALEA = new SecureRandom();

    private OutilsCles() {
    }

    /**
     * p = 2p' + 1 premier sûr de tailleBits bits, g un carré différent de 1
     * (donc d'ordre p'), x au hasard dans [1, p' - 1].
     * La clé publique s'obtient avec clefPublique().
     */
    public static ClefPrivee genererCles(int tailleBits) {
        BigInteger pPrime;
        BigInteger p;
        do {
            pPrime = BigInteger.probablePrime(tailleBits - 1, ALEA);
            p = pPrime.shiftLeft(1).add(UN);
        } while (!p.isProbablePrime(64));

        BigInteger g;
        do {
            BigInteger a = new BigInteger(p.bitLength() + 64, ALEA).mod(p);
            g = a.multiply(a).mod(p);
        } while (g.compareTo(UN) <= 0);

        BigInteger x = new BigInteger(pPrime.bitLength() + 64, ALEA)
                .mod(pPrime.subtract(UN)).add(UN);
        return new ClefPrivee(p, g, x);
    }

    /** Agrégation : produit terme à terme, réduit modulo p. */
    public static Chiffre agreger(Chiffre a, Chiffre b, BigInteger p) {
        return new Chiffre(a.u.multiply(b.u).mod(p), a.v.multiply(b.v).mod(p));
    }

    /** Agrégation sans réduction modulo p (les nombres grossissent). */
    public static Chiffre agregerSansReduire(Chiffre a, Chiffre b) {
        return new Chiffre(a.u.multiply(b.u), a.v.multiply(b.v));
    }

    /** Chiffre chaque vote avec Chiffrement.encrypt, puis agrège le tout. */
    public static Chiffre urne(ClefPrivee sk, int... votes) {
        ClefPublique pk = sk.clefPublique();
        BigInteger B = BigInteger.ONE;
        Chiffre total = Chiffrement.encrypt(BigInteger.ZERO, B, pk);
        for (int v : votes) {
            total = agreger(total, Chiffrement.encrypt(BigInteger.valueOf(v), B, pk), pk.p);
        }
        return total;
    }
}
