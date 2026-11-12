package systeme;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import crypto.*;

class UrneTest {
    static BigInteger b(long x) { return BigInteger.valueOf(x); }

    static final ClefPrivee SK = new ClefPrivee(b(23), b(5), b(6));
    static final ClefPublique PK = SK.clefPublique();

    @Test
    void urneAccumuleLesVotes() {
        Urne urne = new Urne(PK);
        urne.ajouter(Chiffrement.encrypt(b(1), b(1), PK));
        urne.ajouter(Chiffrement.encrypt(b(1), b(1), PK));
        urne.ajouter(Chiffrement.encrypt(b(0), b(1), PK));
        assertEquals(b(2), Dechiffrement.decrypt(urne.getTotal(), b(3), SK));
    }

    @Test
    void urneVideDonneZero() {
        Urne urne = new Urne(PK);
        assertEquals(b(0), Dechiffrement.decrypt(urne.getTotal(), b(3), SK));
    }
}