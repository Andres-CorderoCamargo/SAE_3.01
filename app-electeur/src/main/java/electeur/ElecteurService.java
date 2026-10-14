package electeur;

import com.g3.vote.proto.*;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.net.Socket;

public class ElecteurService {

    /**
     * Connecte un utilisateur et renvoie son token de session.
     * Renvoie null si l'authentification echoue.
     */
    public static String login(Socket socket, String login, String mdpClair) {
        try {
            LoginRequest req = LoginRequest.newBuilder()
                    .setLogin(login)
                    .setPasswordHash(ByteString.copyFrom(hacherMotDePasse(mdpClair)))
                    .build();

            ClientMessage msg = ClientMessage.newBuilder()
                    .setLoginRequest(req)
                    .build();

            msg.writeDelimitedTo(socket.getOutputStream());
            socket.getOutputStream().flush();

            LoginResponse rep = LoginResponse.parseDelimitedFrom(socket.getInputStream());

            if (rep != null && rep.getSuccess()) {
                System.out.println("[CLIENT] Connecte en tant que " + rep.getRole());
                if (rep.getDoitChangerMdp()) {
                    System.out.println("[CLIENT] Changement de mot de passe obligatoire.");
                }
                return rep.getToken();
            }

            System.out.println("[CLIENT] Echec : " + (rep != null ? rep.getMessage() : "pas de reponse"));
            return null;

        } catch (IOException e) {
            System.err.println("Erreur reseau : " + e.getMessage());
            return null;
        }
    }

    /**
     * Ajoute un electeur a la liste. Reserve a l'organisateur : le serveur
     * refuse la demande si le token n'est pas celui d'un organisateur.
     *
     * @param electeurId identifiant de 5 caracteres maximum
     */
    public static boolean ajouterElecteur(Socket socket, String token, String electeurId,
                                          String login, String mdpClair,
                                          String nom, String prenom) {
        if (electeurId == null || electeurId.length() > 5) {
            System.out.println("[CLIENT] Identifiant invalide : 5 caracteres maximum");
            return false;
        }

        try {
            AjouterElecteurRequest req = AjouterElecteurRequest.newBuilder()
                    .setSessionToken(token)
                    .setElecteurId(electeurId)
                    .setLogin(login)
                    .setPasswordHash(ByteString.copyFrom(hacherMotDePasse(mdpClair)))
                    .setNom(nom)
                    .setPrenom(prenom)
                    .build();

            ClientMessage msg = ClientMessage.newBuilder()
                    .setAjouterElecteurRequest(req)
                    .build();

            msg.writeDelimitedTo(socket.getOutputStream());
            socket.getOutputStream().flush();

            AjouterElecteurResponse rep = AjouterElecteurResponse.parseDelimitedFrom(socket.getInputStream());

            if (rep != null && rep.getSuccess()) {
                System.out.println("[SUCCES] " + rep.getMessage());
                return true;
            }

            System.out.println("[ECHEC] " + (rep != null ? rep.getMessage() : "pas de reponse"));
            return false;

        } catch (IOException e) {
            System.err.println("Erreur reseau : " + e.getMessage());
            return false;
        }
    }

    private static byte[] hacherMotDePasse(String mdp) {
        // TODO Zakaria : hacher (pas chiffrer) avec un sel, puis encoder.
        // Le resultat est range dans mdpHashUtilisateur, VARCHAR2(100).
        return mdp.getBytes();
    }
}
