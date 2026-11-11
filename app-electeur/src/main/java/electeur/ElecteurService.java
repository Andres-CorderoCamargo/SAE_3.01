package electeur;

import com.g3.vote.proto.*;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.net.Socket;

public class ElecteurService {

    // Jeton recu du serveur a la connexion. Servira a prouver qui on est.
    private static String jetonSession = "";

    public static String getJetonSession() {
        return jetonSession;
    }

    /**
     * Encapsule la logique d'inscription d'un électeur.
     *
     * @_socket         La socket connectée au serveur (l'urne)
     * @param electeurId L'identifiant unique de l'électeur
     * @param login      Le login choisi
     * @param mdpClair   Le mot de passe en clair
     * @param nom        le nom de l'électeur
     * @param prenom     le prénom de l'électeur
     * @return true si l'inscription a réussi, false sinon
     */
    public static boolean inscrireElecteur(Socket socket, String electeurId, String login, String mdpClair,String nom,String prenom) {
        try {
            byte[] mdpOctets  = preparerMotDePasse(mdpClair);

            RegisterRequest registerRequest = RegisterRequest.newBuilder()
                    .setElecteurId(electeurId)
                    .setLogin(login)
                    .setPasswordChiffre(ByteString.copyFrom(mdpOctets))
                    .setNom(nom)
                    .setPrenom(prenom)
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

    /**
     * Envoie un login et un mot de passe au serveur et recupere la reponse.
     *
     * @param socket     la socket connectee au serveur
     * @param login      le login saisi
     * @param motDePasse le mot de passe saisi, en clair
     * @return le message a afficher a l'utilisateur
     */
    public static String seConnecter(Socket socket, String login, String motDePasse) {
        try {
            LoginRequest loginRequest = LoginRequest.newBuilder()
                    .setLogin(login)
                    .setMotDePasse(motDePasse)
                    .build();

            ClientMessage clientMsg = ClientMessage.newBuilder()
                    .setLoginRequest(loginRequest)
                    .build();

            clientMsg.writeDelimitedTo(socket.getOutputStream());
            socket.getOutputStream().flush();

            LoginResponse response = LoginResponse.parseDelimitedFrom(socket.getInputStream());

            if (response == null) {
                return "Réponse vide du serveur";
            }

            if (!response.getSuccess()) {
                return response.getMessage();
            }

            jetonSession = response.getJetonSession();

            String resultat = "Connecté en tant que " + response.getRole();
            if (response.getDoitChangerMdp()) {
                resultat = resultat + " — mot de passe à changer";
            }
            return resultat;

        } catch (IOException e) {
            System.err.println("Erreur réseau/flux : " + e.getMessage());
            return "Erreur réseau";
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

    public static String modifierElecteur(Socket socket, String electeurId, String login,
                                          String nom, String prenom) {
        try {
            UpdateElecteurRequest request = UpdateElecteurRequest.newBuilder()
                    .setElecteurId(electeurId)
                    .setLogin(login)
                    .setNom(nom)
                    .setPrenom(prenom)
                    .build();

            ClientMessage clientMsg = ClientMessage.newBuilder()
                    .setUpdateElecteurRequest(request)
                    .build();

            clientMsg.writeDelimitedTo(socket.getOutputStream());
            socket.getOutputStream().flush();

            UpdateElecteurResponse response =
                    UpdateElecteurResponse.parseDelimitedFrom(socket.getInputStream());

            return (response != null) ? response.getMessage() : "Réponse vide du serveur";

        } catch (IOException e) {
            System.err.println("Erreur réseau/flux : " + e.getMessage());
            return "Erreur réseau";
        }
    }

    public static VoteResponse voter(Socket socket, String electeurId, String referendumId,
                                     byte[] bulletinU, byte[] bulletinV) {
        try {
            // Construction de message de vote :
            VoteRequest request = VoteRequest.newBuilder()
                    .setElecteurId(electeurId)
                    .setReferendumId(referendumId)
                    .setBulletinU(ByteString.copyFrom(bulletinU))
                    .setBulletinV(ByteString.copyFrom(bulletinV))
                    .build();

            // Puis vote placé dans ClientMesage
            ClientMessage clientMsg = ClientMessage.newBuilder()
                    .setVoteRequest(request)
                    .build();

            // Envoie socket
            clientMsg.writeDelimitedTo(socket.getOutputStream());
            socket.getOutputStream().flush();

            VoteResponse response = VoteResponse.parseDelimitedFrom(socket.getInputStream());

            if (response == null) {
                return VoteResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Réponse vide du serveur")
                        .build();
            }
            return response;

        } catch (IOException e) {
            // Si jamais un erreur, le système renvoie une réponse locale..
            System.err.println("Erreur réseau/flux : " + e.getMessage());
            return VoteResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Erreur réseau")
                    .build();
        }
    }


    private static byte[] preparerMotDePasse(String mdpClair) {
        return mdpClair.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
