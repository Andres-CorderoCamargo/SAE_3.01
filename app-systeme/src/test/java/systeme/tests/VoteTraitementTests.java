package systeme.tests;

import com.g3.vote.proto.VoteRequest;
import com.g3.vote.proto.VoteResponse;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import systeme.VoteTraitement;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("[Voter] VoteTraitement")
class VoteTraitementTest {

    private static VoteRequest requete(String electeur, String referendum, byte[] u, byte[] v) {
        return VoteRequest.newBuilder()
                .setElecteurId(electeur)
                .setReferendumId(referendum)
                .setBulletinU(ByteString.copyFrom(u))
                .setBulletinV(ByteString.copyFrom(v))
                .build();
    }

    private static VoteRequest requeteValide() {
        return requete("U3", "R1", new byte[]{1, 2, 3}, new byte[]{4, 5, 6});
    }

    private static class Faux implements VoteTraitement.Enregistreur {
        final String code;
        final AtomicInteger appels = new AtomicInteger();
        String idReferendum, idElecteur;
        byte[] u, v;

        Faux(String code) { this.code = code; }

        @Override
        public String enregistrer(String r, String e, byte[] u, byte[] v) {
            appels.incrementAndGet();
            this.idReferendum = r; this.idElecteur = e; this.u = u; this.v = v;
            return code;
        }
    }

    @Test
    @DisplayName("Bulletin chiffré valide : confirmation, et le bulletin arrive intact à l'enregistrement")
    void confirmation() {
        Faux faux = new Faux("OK");
        VoteResponse r = new VoteTraitement(faux).traiter(requeteValide());

        assertTrue(r.getSuccess());
        assertFalse(r.getIsLate());
        assertEquals(1, faux.appels.get());
        assertEquals("R1", faux.idReferendum);
        assertEquals("U3", faux.idElecteur);
        assertArrayEquals(new byte[]{1, 2, 3}, faux.u);
        assertArrayEquals(new byte[]{4, 5, 6}, faux.v);
    }

    @Test
    @DisplayName("Référendum clos : refus, is_late = true")
    void referendumClos() {
        VoteResponse r = new VoteTraitement(new Faux("REFERENDUM_FERME")).traiter(requeteValide());

        assertFalse(r.getSuccess());
        assertTrue(r.getIsLate());
        assertTrue(r.getMessage().contains("fermé"));
    }

    @Test
    @DisplayName("Electeur non inscrit : refus")
    void nonInscrit() {
        VoteResponse r = new VoteTraitement(new Faux("NON_INSCRIT")).traiter(requeteValide());

        assertFalse(r.getSuccess());
        assertFalse(r.getIsLate());
        assertTrue(r.getMessage().contains("non inscrit"));
    }

    @Test
    @DisplayName("Electeur ayant déjà voté : refus")
    void dejaVote() {
        VoteResponse r = new VoteTraitement(new Faux("DEJA_VOTE")).traiter(requeteValide());

        assertFalse(r.getSuccess());
        assertTrue(r.getMessage().contains("déjà voté"));
    }

    @Test
    @DisplayName("Référendum inconnu : refus")
    void referendumInconnu() {
        VoteResponse r = new VoteTraitement(new Faux("REFERENDUM_INCONNU")).traiter(requeteValide());

        assertFalse(r.getSuccess());
        assertTrue(r.getMessage().contains("inconnu"));
    }

    @Test
    @DisplayName("Code inattendu ou null : refus, jamais une confirmation")
    void codeInattendu() {
        assertFalse(new VoteTraitement(new Faux("BIZARRE")).traiter(requeteValide()).getSuccess());
        assertFalse(new VoteTraitement(new Faux(null)).traiter(requeteValide()).getSuccess());
        assertFalse(new VoteTraitement(new Faux("ERREUR")).traiter(requeteValide()).getSuccess());
    }

    @Test
    @DisplayName("Panne de base : refus, pas d'exception")
    void panneBase() {
        VoteTraitement t = new VoteTraitement((r, e, u, v) -> { throw new SQLException("Oracle HS"); });
        VoteResponse r = assertDoesNotThrow(() -> t.traiter(requeteValide()));

        assertFalse(r.getSuccess());
        assertTrue(r.getMessage().contains("base de données"));
    }

    @Test
    @DisplayName("Message mal formé : refusé sans toucher à la base")
    void messageMalForme() {
        byte[] ok = {1};
        VoteRequest[] mauvaises = {
                requete("", "R1", ok, ok),                        // électeur vide
                requete("   ", "R1", ok, ok),                     // électeur d'espaces
                requete("TROPLONG", "R1", ok, ok),                // électeur > 5 caractères
                requete("U3", "", ok, ok),                        // référendum vide
                requete("U3", "REF123", ok, ok),                  // référendum > 5 caractères
                requete("U3", "R1", new byte[0], ok),             // u vide
                requete("U3", "R1", ok, new byte[0]),             // v vide
                requete("U3", "R1", new byte[4097], ok),          // u trop gros
                requete("U3", "R1", ok, new byte[4097]),          // v trop gros
                VoteRequest.getDefaultInstance()                  // rien du tout
        };

        for (VoteRequest m : mauvaises) {
            Faux faux = new Faux("OK");
            VoteResponse r = new VoteTraitement(faux).traiter(m);
            assertFalse(r.getSuccess(), "devait être refusé : " + m);
            assertEquals(0, faux.appels.get(), "la base n'aurait pas dû être appelée : " + m);
        }
    }

    @Test
    @DisplayName("Bulletin de 4096 octets (limite) accepté")
    void limite() {
        Faux faux = new Faux("OK");
        VoteResponse r = new VoteTraitement(faux)
                .traiter(requete("U3", "R1", new byte[4096], new byte[4096]));
        assertTrue(r.getSuccess());
    }

    @Test
    @DisplayName("Les espaces autour des identifiants sont retirés")
    void trim() {
        Faux faux = new Faux("OK");
        new VoteTraitement(faux).traiter(requete(" U3 ", " R1 ", new byte[]{1}, new byte[]{1}));
        assertEquals("U3", faux.idElecteur);
        assertEquals("R1", faux.idReferendum);
    }
}
