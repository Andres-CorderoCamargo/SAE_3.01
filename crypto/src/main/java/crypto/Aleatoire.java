package crypto;

import java.math.BigInteger;
import java.security.SecureRandom;

/** Tirages aléatoires cryptographiques, partagés par les classes du package. */
class Aleatoire {

    private static final SecureRandom RND = new SecureRandom();

    private Aleatoire() {
    }

    /** Tirage uniforme dans [0, n - 1], par rejet (sans biais). */
    static BigInteger dans(BigInteger n) {
        BigInteger r;
        do {
            r = new BigInteger(n.bitLength(), RND);
        } while (r.compareTo(n) >= 0);
        return r;
    }
}