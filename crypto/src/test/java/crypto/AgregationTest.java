package crypto;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class AgregationTest {
    static BigInteger b(long x) { return BigInteger.valueOf(x); }

    static final ClefPrivee SK = new ClefPrivee(b(23), b(5), b(6));
    static final ClefPublique PK = SK.clefPublique();

    @Test
    void chiffresFabriquesALaMain() {
        // deux votes « 1 » : (10,7) et (2,21)
        Chiffre r = Agregation.agrege(
                new Chiffre(b(10), b(7)), new Chiffre(b(2), b(21)), PK);
        assertEquals(b(20), r.u);
        assertEquals(b(9), r.v);
    }

    @Test
    void additionneLesVotesEnClair() {
        Chiffre c1 = Chiffrement.encryptAvecR(b(1), b(1), PK, b(3));
        Chiffre c2 = Chiffrement.encryptAvecR(b(1), b(1), PK, b(2));
        Chiffre c3 = Chiffrement.encryptAvecR(b(0), b(1), PK, b(5));
        Chiffre total = Agregation.agrege(Agregation.agrege(c1, c2, PK), c3, PK);
        assertEquals(b(2), Dechiffrement.decrypt(total, b(3), SK));
    }

    @Test
    void neutreNeChangeRien() {
        Chiffre a = new Chiffre(b(10), b(7));
        Chiffre r = Agregation.agrege(a, Agregation.neutre(), PK);
        assertEquals(a.u, r.u);
        assertEquals(a.v, r.v);
    }

    @Test
    void commutative() {
        Chiffre a = new Chiffre(b(10), b(7)), c = new Chiffre(b(2), b(21));
        Chiffre ac = Agregation.agrege(a, c, PK), ca = Agregation.agrege(c, a, PK);
        assertEquals(ac.u, ca.u);
        assertEquals(ac.v, ca.v);
    }

    @Test
    void totalDeNombreuxVotesAleatoires() {
        int n = 50, oui = 0;
        Chiffre total = Agregation.neutre();
        for (int i = 0; i < n; i++) {
            int vote = (i % 3 == 0) ? 1 : 0;
            oui += vote;
            total = Agregation.agrege(total,
                    Chiffrement.encrypt(b(vote), b(1), PK), PK);
        }
        assertEquals(b(oui), Dechiffrement.decrypt(total, b(n), SK));
    }

    @Test
    void urneVideDechiffreEnZero() {
        assertEquals(b(0), Dechiffrement.decrypt(Agregation.neutre(), b(10), SK));
    }
}