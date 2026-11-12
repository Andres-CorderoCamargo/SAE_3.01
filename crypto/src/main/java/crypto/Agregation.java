package crypto;

import java.math.BigInteger;

/** Agrege : propriété homomorphe d'ElGamal, produit composante par composante mod p. */
public class Agregation {

    private Agregation() {
    }

    /** Chiffré neutre (1, 1) : chiffre « 0 vote ». */
    public static Chiffre neutre() {
        return new Chiffre(BigInteger.ONE, BigInteger.ONE);
    }

    /** (u1·u2 mod p, v1·v2 mod p). Ne déchiffre jamais. */
    public static Chiffre agrege(Chiffre a, Chiffre b, ClefPublique pk) {
        return new Chiffre(
                a.u.multiply(b.u).mod(pk.p),
                a.v.multiply(b.v).mod(pk.p));
    }
}