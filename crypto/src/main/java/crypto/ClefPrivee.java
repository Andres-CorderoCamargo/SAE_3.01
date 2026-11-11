package crypto;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Clé privée ElGamal (p, g, x), associée à la clé publique (p, g, h) avec
 * h = g^x mod p. Elle reste chez le scrutateur, qui seul déchiffre le total.
 *
 * g est gardé dans la clé privée : Decrypt en a besoin pour retrouver m à
 * partir de g^m, et il n'est pas fixé à l'avance (il fait partie de la clé).
 */
public class ClefPrivee {
    public final BigInteger p, g, x;

    public ClefPrivee(BigInteger p, BigInteger g, BigInteger x) {
        this.p = Objects.requireNonNull(p, "p");
        this.g = Objects.requireNonNull(g, "g");
        this.x = Objects.requireNonNull(x, "x");
    }

    /** La clé publique correspondante : (p, g, g^x mod p). */
    public ClefPublique clefPublique() {
        return new ClefPublique(p, g, g.modPow(x, p));
    }

    /** x est un secret : il ne doit pas finir dans un log par accident. */
    @Override
    public String toString() {
        return "ClefPrivee[p=" + p + ", g=" + g + ", x=masqué]";
    }
}
