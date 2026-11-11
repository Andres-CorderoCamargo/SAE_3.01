package systeme;

import com.g3.vote.proto.VoteRequest;
import com.g3.vote.proto.VoteResponse;

import java.sql.SQLException;

public class VoteTraitement {

    // Taille maximale d'un bloc du bulletin, pour ne pas stocker n'importe quoi.
    static final int TAILLE_MAX_BLOC = 4096;

    @FunctionalInterface
    public interface Enregistreur {
        String enregistrer(String idReferendum, String idElecteur,
                           byte[] bulletinU, byte[] bulletinV) throws SQLException;
    }

    private final Enregistreur enregistreur;

    public VoteTraitement(Enregistreur enregistreur) {
        this.enregistreur = enregistreur;
    }

    public VoteResponse traiter(VoteRequest requete) {
        String electeurId = requete.getElecteurId().trim();
        String referendumId = requete.getReferendumId().trim();
        byte[] u = requete.getBulletinU().toByteArray();
        byte[] v = requete.getBulletinV().toByteArray();

        // Controles sans base : un message mal forme n'ouvre pas de connexion
        if (electeurId.isEmpty() || electeurId.length() > 5
                || referendumId.isEmpty() || referendumId.length() > 5) {
            return refus("Identifiant électeur ou référendum invalide.", false);
        }
        if (u.length == 0 || v.length == 0
                || u.length > TAILLE_MAX_BLOC || v.length > TAILLE_MAX_BLOC) {
            return refus("Bulletin invalide.", false);
        }

        // Enregistrement (la base décide : ouvert ? inscrit ? déjà voté ?)
        String code;
        try {
            code = enregistreur.enregistrer(referendumId, electeurId, u, v);
        } catch (SQLException e) {
            System.err.println("[SERVEUR] " + e.getMessage());
            return refus("Erreur base de données.", false);
        }

        // Traduction du code en réponse
        if (code == null) {
            return refus("Erreur serveur.", false);
        }
        switch (code) {
            case "OK":
                return VoteResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Vote enregistré.")
                        .setIsLate(false)
                        .build();
            case "REFERENDUM_FERME":
                return refus("Référendum fermé : le bulletin est refusé.", true);
            case "REFERENDUM_INCONNU":
                return refus("Référendum inconnu.", false);
            case "NON_INSCRIT":
                return refus("Électeur non inscrit à ce référendum.", false);
            case "DEJA_VOTE":
                return refus("Cet électeur a déjà voté.", false);
            default:
                return refus("Erreur serveur.", false);
        }
    }

    private static VoteResponse refus(String message, boolean enRetard) {
        return VoteResponse.newBuilder()
                .setSuccess(false)
                .setMessage(message)
                .setIsLate(enRetard)
                .build();
    }
}

