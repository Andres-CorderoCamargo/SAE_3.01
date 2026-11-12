package systeme;

import crypto.Agregation;
import crypto.Chiffre;
import crypto.ClefPublique;

/** Urne en mémoire : garde un seul chiffré, le total courant des votes. */
public class Urne {
    private final ClefPublique pk;
    private Chiffre total = Agregation.neutre();

    public Urne(ClefPublique pk) {
        this.pk = pk;
    }

    public synchronized void ajouter(Chiffre vote) {
        total = Agregation.agrege(total, vote, pk);
    }

    public synchronized Chiffre getTotal() {
        return total;
    }
}