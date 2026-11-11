package crypto;

import java.math.BigInteger;

/** Encrypt : c = (g^r mod p, g^m * h^r mod p). */
public class Chiffrement {

    private Chiffrement() {
    }

    /**
     * @param m  message en clair, dans [0, B] (pour un vote : 0 ou 1)
     * @param B  borne sur les messages
     * @param pk clé publique
     */
    public static Chiffre encrypt(BigInteger m, BigInteger B, ClefPublique pk) {
        BigInteger r = Aleatoire.dans(pk.pPrime());      // r dans [0, p'-1]
        return encryptAvecR(m, B, pk, r);
    }

    /** Même calcul avec r imposé : réservé aux tests (visible du package seulement). */
    static Chiffre encryptAvecR(BigInteger m, BigInteger B, ClefPublique pk, BigInteger r) {
        if (m.signum() < 0 || m.compareTo(B) > 0) {
            throw new IllegalArgumentException("m doit etre dans [0, B]");
        }
        BigInteger u = pk.g.modPow(r, pk.p);
        BigInteger v = pk.g.modPow(m, pk.p)
                .multiply(pk.h.modPow(r, pk.p))
                .mod(pk.p);
        return new Chiffre(u, v);
    }
}