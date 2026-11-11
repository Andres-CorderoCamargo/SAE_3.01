package electeur;

import java.math.BigInteger;

public class BulletinFactice {
    private BulletinFactice() {
    }

    /** @return {bulletinU, bulletinV} (en octets), comme dans VoteRequest */
    public static byte[][] fabriquer(int vote) {
        BigInteger u = BigInteger.valueOf(123456789L);
        BigInteger v = BigInteger.valueOf(987654321L + vote);
        return new byte[][] { u.toByteArray(), v.toByteArray() };
    }
}
