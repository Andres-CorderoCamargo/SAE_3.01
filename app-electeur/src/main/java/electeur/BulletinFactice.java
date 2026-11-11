package electeur;

import java.math.BigInteger;

/**
 * Bulletin "fabriqué à la main" pour tester l'envoi du vote sans la moitié A
 * (le vrai chiffrement ElGamal de Roman).
 *
 * Ce n'est PAS un chiffrement : les valeurs sont fixes et ne cachent rien.
 * À supprimer quand le vrai Chiffre sera branché : seul l'appel dans
 * electeur.java change, ElecteurService.voter() ne bouge pas.
 */
public class BulletinFactice {

    private BulletinFactice() {
    }

    /** @return {bulletinU, bulletinV} en octets, comme dans VoteRequest */
    public static byte[][] fabriquer(int vote) {
        BigInteger u = BigInteger.valueOf(123456789L);
        BigInteger v = BigInteger.valueOf(987654321L + vote);
        return new byte[][] { u.toByteArray(), v.toByteArray() };
    }
}
