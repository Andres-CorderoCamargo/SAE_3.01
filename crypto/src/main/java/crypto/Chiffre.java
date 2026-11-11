package crypto;

import java.math.BigInteger;

/** Un chiffré c = (u, v). */
public class Chiffre {
    public final BigInteger u, v;

    public Chiffre(BigInteger u, BigInteger v) {
        this.u = u;
        this.v = v;
    }

    /** Pour remplir les champs bytes du proto. */
    public byte[] octetsU() { return u.toByteArray(); }
    public byte[] octetsV() { return v.toByteArray(); }

    public static Chiffre depuisOctets(byte[] u, byte[] v) {
        return new Chiffre(new BigInteger(u), new BigInteger(v));
    }
}