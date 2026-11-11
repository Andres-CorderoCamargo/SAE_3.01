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
                    .setJetonSession(jetonSession)
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
                    .setJetonSession(jetonSession)
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
                    .setJetonSession(jetonSession)
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


    private static byte[] preparerMotDePasse(String mdpClair) {
        return mdpClair.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
        private static void traiterLancement(LancerReferendumRequest request, Socket socket) throws IOException {
        System.out.println("[SERVEUR] Lancement demandé pour : " + request.getReferendumId());

        String idReferendum = request.getReferendumId().trim();
        boolean success;
        String message;

        if (!estOrganisateur(request.getJetonSession())) {
            success = false;
            message = REFUS;
            System.out.println("[SERVEUR] Lancement refusé : pas organisateur");

        } else if (idReferendum.isEmpty()) {
            success = false;
            message = "Échec : identifiant du référendum obligatoire.";
        } else {
            try {
                message = AuthentificationDAO.lancerReferendum(idReferendum);
                success = message.equals("OK");
            } catch (SQLException e) {
                success = false;
                message = "Erreur base de données";
                System.err.println("[SERVEUR] " + e.getMessage());
            }
        }

        LancerReferendumResponse response = LancerReferendumResponse.newBuilder()
                .setSuccess(success)
                .setMessage(message)
                .build();

        response.writeDelimitedTo(socket.getOutputStream());
        socket.getOutputStream().flush();
    }

    private static void traiterCloture(CloturerReferendumRequest request, Socket socket) throws IOException {
        System.out.println("[SERVEUR] Clôture demandée pour : " + request.getReferendumId());

        String idReferendum = request.getReferendumId().trim();
        boolean success;
        String message;

        if (!estOrganisateur(request.getJetonSession())) {
            success = false;
            message = REFUS;
            System.out.println("[SERVEUR] Clôture refusée : pas organisateur");

        } else if (idReferendum.isEmpty()) {
            success = false;
            message = "Échec : identifiant du référendum obligatoire.";
        } else {
            try {
                message = AuthentificationDAO.cloturerReferendum(idReferendum);
                success = message.equals("OK");
            } catch (SQLException e) {
                success = false;
                message = "Erreur base de données";
                System.err.println("[SERVEUR] " + e.getMessage());
            }
        }

        CloturerReferendumResponse response = CloturerReferendumResponse.newBuilder()
                .setSuccess(success)
                .setMessage(message)
                .build();

        response.writeDelimitedTo(socket.getOutputStream());
        socket.getOutputStream().flush();
    }
        public static String inscrireElecteurReferendum(Socket socket, String idReferendum, String idElecteur) {
        try {
            InscrireElecteurRequest request = InscrireElecteurRequest.newBuilder()
                    .setReferendumId(idReferendum)
                    .setElecteurId(idElecteur)
                    .setJetonSession(jetonSession)
                    .build();

            ClientMessage clientMsg = ClientMessage.newBuilder()
                    .setInscrireElecteurRequest(request)
                    .build();

            clientMsg.writeDelimitedTo(socket.getOutputStream());
            socket.getOutputStream().flush();

            InscrireElecteurResponse response =
                    InscrireElecteurResponse.parseDelimitedFrom(socket.getInputStream());

            return (response != null) ? response.getMessage() : "Réponse vide du serveur";

        } catch (IOException e) {
            System.err.println("Erreur réseau/flux : " + e.getMessage());
            return "Erreur réseau";
        }
    }
}
