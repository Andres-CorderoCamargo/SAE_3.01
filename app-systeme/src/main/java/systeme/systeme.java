package systeme;

import com.g3.vote.proto.*;
import systeme.persistance.AuthentificationDAO;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

public class systeme {

    private static final ConcurrentHashMap<String, InfosStockageElecteur> baseElecteursTemporaire = new ConcurrentHashMap<>();
    public static void main(String[] args) {


        int port = 8080;
        System.out.println("[SERVEUR] Démarrage de l'urne sur le port " + port + "...");



        try (ServerSocket serverSocket = new ServerSocket(port)) {

            while (true) {
                // Attente d'un nouvel électeur
                Socket socket = serverSocket.accept();
                System.out.println("[SERVEUR] Nouvel électeur connecté : " + socket.getRemoteSocketAddress());

                // Lancement d'un thread dédié pour ce client
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("[SERVEUR] Erreur sur le serveur : " + e.getMessage());
        }
    }

    /**
     * Classe interne pour gérer chaque client dans un thread séparé
     */
    private static class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (Socket s = socket) {
                ClientMessage clientMsg;

                // Boucle pour écouter tant que le client envoie des messages sur la même socket
                while ((clientMsg = ClientMessage.parseDelimitedFrom(s.getInputStream())) != null) {

                    // Routage selon le type de requête reçue grâce au oneof
                    if (clientMsg.hasRegisterRequest()) {
                        traiterInscription(clientMsg.getRegisterRequest(), s);

                    } else if (clientMsg.hasVoteRequest()) {
                        traiterVote(clientMsg.getVoteRequest(), s);

                    } else if (clientMsg.hasPublicKeyRequest()) {
                        traiterPublicKey(clientMsg.getPublicKeyRequest(), s);

                    } else if (clientMsg.hasDeleteElecteurRequest()) {
                        traiterSuppression(clientMsg.getDeleteElecteurRequest(), s);

                    } else {
                        System.out.println("[SERVEUR] Type de message inconnu ou non géré.");
                    }
                }

                System.out.println("[SERVEUR] L'électeur s'est déconnecté proprement : " + s.getRemoteSocketAddress());

            } catch (IOException e) {
                System.err.println("[SERVEUR] Erreur réseau ou déconnexion brusque : " + e.getMessage());
            }
        }
    }

    private static void traiterInscription(RegisterRequest request, Socket socket) throws IOException {
        System.out.println("[SERVEUR] Traitement inscription pour : " + request.getLogin());

        String electeurId = request.getElecteurId().trim();
        String login = request.getLogin().trim();
        String mdpClair = recupererMotDePasse(request.getPasswordChiffre());
        String nom = request.getNom().trim();
        String prenom = request.getPrenom().trim();

        boolean success;
        String message;

        // Vérifications de base
        if (electeurId.isEmpty() || login.isEmpty() || mdpClair.isEmpty()
                || nom.isEmpty() || prenom.isEmpty()) {
            success = false;
            message = "Échec : tous les champs sont obligatoires.";
        } else {
            try {
                // Écriture en base : le DAO hache le mot de passe puis appelle la procédure
                message = AuthentificationDAO.ajouterElecteur(electeurId, login, mdpClair, nom, prenom);
                success = message.equals("OK");

                if (success) {
                    // Debug uniquement : copie en mémoire, sans mot de passe ni hash
                    baseElecteursTemporaire.put(electeurId,
                            new InfosStockageElecteur(electeurId, login, "(en base)"));
                    System.out.println("[SERVEUR - DEBUG] Électeurs en mémoire : "
                            + baseElecteursTemporaire.size());
                    message = "Inscription réussie.";
                }
            } catch (SQLException e) {
                success = false;
                message = "Erreur base de données";
                System.err.println("[SERVEUR] " + e.getMessage());
            }
        }

        System.out.println("[SERVEUR] Message renvoyé : " + message);

        RegisterResponse response = RegisterResponse.newBuilder()
                .setSuccess(success)
                .setMessage(message)
                .build();

        response.writeDelimitedTo(socket.getOutputStream());
        socket.getOutputStream().flush();
    }

    private static void traiterVote(VoteRequest request, Socket socket) throws IOException {
        System.out.println("[SERVEUR] Réception d'un bulletin de vote pour l'électeur : " + request.getElecteurId());

        // TODO: Ajouter la logique de traitement et de réponse du vote
    }

    private static void traiterPublicKey(PublicKeyRequest request, Socket socket) throws IOException {
        System.out.println("[SERveur] Demande de clé publique reçue.");

        // TODO: Envoyer la PublicKeyResponse
    }

    private static void traiterSuppression(DeleteElecteurRequest request, Socket socket) throws IOException {
        System.out.println("[SERVEUR] Suppression demandée pour : " + request.getElecteurId());

        boolean success;
        String message;

        try {
            // TODO: vérifier ici que l'utilisateur connecté est ORGANISATEUR
            message = AuthentificationDAO.supprimerElecteur(request.getElecteurId());
            success = message.equals("OK");
        } catch (SQLException e) {
            success = false;
            message = "Erreur base de données";
            System.err.println("[SERVEUR] " + e.getMessage());
        }

        DeleteElecteurResponse response = DeleteElecteurResponse.newBuilder()
                .setSuccess(success)
                .setMessage(message)
                .build();

        response.writeDelimitedTo(socket.getOutputStream());
        socket.getOutputStream().flush();
    }

    private static String recupererMotDePasse(com.google.protobuf.ByteString octets) {
        return octets.toStringUtf8();
    }
}