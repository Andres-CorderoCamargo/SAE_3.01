package crypto;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Decrypt : retrouve m à partir de c = (u, v) = (g^r, g^m * h^r) mod p.
 *
 *   u^x = g^(r*x) = h^r,  donc  v / u^x = g^m mod p.
 *
 * ElGamal « exponentiel » ne donne que g^m, pas m : on cherche m en essayant
 * 0, 1, 2, ... jusqu'à la borne B. Pour un référendum, m est le nombre de Oui
 * et B le nombre d'inscrits, donc c'est rapide.
 *
 * Marche aussi sur un chiffré agrégé (produit de plusieurs chiffrés) : il
 * chiffre la somme des votes. Un produit non réduit modulo p convient aussi.
 *
 * Symétrique de Chiffrement.encrypt(m, B, pk).
 */
public class Dechiffrement {

    private Dechiffrement() {
    }

    /**
     * @param c  le chiffré (un bulletin, ou l'agrégat de tous les bulletins)
     * @param B  borne sur m : le total ne peut pas dépasser le nombre d'inscrits
     * @param sk clé privée du scrutateur
     * @return m, dans [0, B]
     * @throws IllegalArgumentException si aucun m dans [0, B] ne convient :
     *         mauvaise clé, chiffré corrompu ou B trop petit. On ne rend
     *         jamais un résultat faux.
     */
    public static BigInteger decrypt(Chiffre c, BigInteger B, ClefPrivee sk) {
        Objects.requireNonNull(c, "c");
        Objects.requireNonNull(c.u, "c.u");
        Objects.requireNonNull(c.v, "c.v");
        Objects.requireNonNull(B, "B");
        Objects.requireNonNull(sk, "sk");
        if (B.signum() < 0) {
            throw new IllegalArgumentException("B doit etre positif ou nul");
        }
        BigInteger p = sk.p;

        // u^x = h^r, puis g^m = v * (h^r)^-1 mod p
        BigInteger hr = c.u.mod(p).modPow(sk.x, p);
        BigInteger gm;
        try {
            gm = c.v.mod(p).multiply(hr.modInverse(p)).mod(p);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Chiffre invalide : u n'est pas inversible modulo p", e);
        }

        // On cherche m dans [0, B] tel que g^m = gm
        BigInteger courant = BigInteger.ONE;          // g^0
        for (BigInteger m = BigInteger.ZERO; m.compareTo(B) <= 0; m = m.add(BigInteger.ONE)) {
            if (courant.equals(gm)) {
                return m;
            }
            courant = courant.multiply(sk.g).mod(p);
        }
        throw new IllegalArgumentException(
            "Aucun m dans [0, B] ne correspond : mauvaise cle, chiffre corrompu ou B trop petit");
    }
}
