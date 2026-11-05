package electeur;
import com.g3.vote.proto.*;
import com.google.protobuf.ByteString;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.net.Socket;

public class ElecteurService {

    /**
     * Encapsule la logique d'inscription d'un électeur.
     *
     * @_socket         La socket connectée au serveur (l'urne)
     * @param electeurId L'identifiant unique de l'électeur
     * @param login      Le login choisi
     * @param mdpClair   Le mot de passe en clair (qui sera chiffré)
     * @return true si l'inscription a réussi, false sinon
     */
    public static boolean inscrireElecteur(Socket socket, String electeurId, String login, String mdpClair) {
        try {
            // Chiffrement du mot de passe
            byte[] mdpChiffre = chiffrerMotDePasse(mdpClair);


            RegisterRequest registerRequest = RegisterRequest.newBuilder()
                    .setElecteurId(electeurId)
                    .setLogin(login)
                    .setPasswordChiffre(ByteString.copyFrom(mdpChiffre))
                    .build();

            ClientMessage clientMsg = ClientMessage.newBuilder()
                    .setRegisterRequest(registerRequest)
                    .build();

            clientMsg.writeDelimitedTo(socket.getOutputStream());
            socket.getOutputStream().flush();

            RegisterResponse response = RegisterResponse.parseDelimitedFrom(socket.getInputStream());

            if (response != null && response.getSuccess()) {
                System.out.println("[SUCCÈS] " + response.getMessage());
                return true;
            } else {
                String errorMsg = (response != null) ? response.getMessage() : "Réponse vide du serveur";
                System.out.println("[ÉCHEC] " + errorMsg);
                return false;
            }

        } catch (IOException e) {
            System.err.println("Erreur réseau/flux : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static String supprimerElecteur(Socket socket, String electeurId) {
        try {
            DeleteElecteurRequest request = DeleteElecteurRequest.newBuilder()
                    .setElecteurId(electeurId)
                    .build();

            ClientMessage clientMsg = ClientMessage.newBuilder()
                    .setDeleteElecteurRequest(request)
                    .build();

            clientMsg.writeDelimitedTo(socket.getOutputStream());
            socket.getOutputStream().flush();

            DeleteElecteurResponse response =
                    DeleteElecteurResponse.parseDelimitedFrom(socket.getInputStream());

            return (response != null) ? response.getMessage() : "Réponse vide du serveur";

        } catch (IOException e) {
            System.err.println("Erreur réseau/flux : " + e.getMessage());
            return "Erreur réseau";
        }
    }


    private static byte[] chiffrerMotDePasse(String mdp) {

        String mdpHache = BCrypt.hashpw(mdp, BCrypt.gensalt(12));
        return mdpHache.getBytes();
    }
}
