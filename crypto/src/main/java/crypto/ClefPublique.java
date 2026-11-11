package crypto;

import java.math.BigInteger;

/** Clé publique (p, g, h), avec p = 2p' + 1. */
public class ClefPublique {
    public final BigInteger p, g, h;

    public ClefPublique(BigInteger p, BigInteger g, BigInteger h) {
        this.p = p;
        this.g = g;
        this.h = h;
    }

    /** p' = (p - 1) / 2. */
    public BigInteger pPrime() {
        return p.subtract(BigInteger.ONE).shiftRight(1);
    }

    /** Format de transport : p:g:h en hexadécimal. */
    public String serialiser() {
        return p.toString(16) + ":" + g.toString(16) + ":" + h.toString(16);
    }

    public static ClefPublique deserialiser(String s) {
        String[] t = s.split(":");
        if (t.length != 3) {
            throw new IllegalArgumentException("Clé publique invalide");
        }
        return new ClefPublique(new BigInteger(t[0], 16),
                new BigInteger(t[1], 16),
                new BigInteger(t[2], 16));
    }
}