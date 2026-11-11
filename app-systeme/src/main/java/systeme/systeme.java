package systeme;

import com.g3.vote.proto.*;
import systeme.persistance.AuthentificationDAO;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class systeme {

    private static final ConcurrentHashMap<String, InfosStockageElecteur> baseElecteursTemporaire = new ConcurrentHashMap<>();

    // Pour retenir qui est connecté : jeton -> utilisateur. Vidé quand le serveur redémarre.
    private static final ConcurrentHashMap<String, Connecte> sessions = new ConcurrentHashMap<>();

    public static class Connecte {
        public String idUtilisateur;
        public String role;
    }

    /** Message unique de refus : on ne dit pas si c'est le jeton ou le rôle qui ne va pas. */
    private static final String REFUS = "Action réservée à l'organisateur. Connectez-vous d'abord.";

    /**
     * Dit si le jeton correspond a une session ouverte par un ORGANISATEUR.
     */
    private static boolean estOrganisateur(String jeton) {
        if (jeton == null || jeton.isEmpty()) {
            return false;
        }
        Connecte c = sessions.get(jeton);
        return c != null && c.role.equals("ORGANISATEUR");
    }

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
                    if (clientMsg.hasLoginRequest()) {
                        traiterConnexion(clientMsg.getLoginRequest(), s);

                    } else if (clientMsg.hasRegisterRequest()) {
                        traiterInscription(clientMsg.getRegisterRequest(), s);

                    } else if (clientMsg.hasVoteRequest()) {
                        traiterVote(clientMsg.getVoteRequest(), s);

                    } else if (clientMsg.hasPublicKeyRequest()) {
                        traiterPublicKey(clientMsg.getPublicKeyRequest(), s);

                    } else if (clientMsg.hasDeleteElecteurRequest()) {
                        traiterSuppression(clientMsg.getDeleteElecteurRequest(), s);

                    } else if (clientMsg.hasUpdateElecteurRequest()) {
                        traiterModification(clientMsg.getUpdateElecteurRequest(), s);
                        
                    } else if (clientMsg.hasInscrireElecteurRequest()) {
                        traiterInscriptionReferendum(clientMsg.getInscrireElecteurRequest(), s);
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

    private static void traiterConnexion(LoginRequest request, Socket socket) throws IOException {
        String login = request.getLogin().trim();
        String motDePasse = request.getMotDePasse();

        System.out.println("[SERVEUR] Tentative de connexion : " + login);

        boolean succes = false;
        String message = "";
        String jeton = "";
        String role = "";
        boolean doitChangerMdp = false;

        if (login.isEmpty() || motDePasse.isEmpty()) {
            message = "Identifiant et mot de passe obligatoires";
        } else {
            try {
                AuthentificationDAO.Resultat resultat = AuthentificationDAO.connecter(login, motDePasse);

                succes = resultat.succes;
                message = resultat.message;

                if (succes) {
                    role = resultat.role;
                    doitChangerMdp = resultat.doitChangerMdp;

                    // On fabrique un jeton au hasard et on retient à qui il appartient
                    jeton = UUID.randomUUID().toString();

                    Connecte c = new Connecte();
                    c.idUtilisateur = resultat.idUtilisateur;
                    c.role = resultat.role;
                    sessions.put(jeton, c);

                    System.out.println("[SERVEUR] " + login + " connecté en tant que " + role);
                }
            } catch (SQLException e) {
                message = "Erreur base de données";
                System.err.println("[SERVEUR] " + e.getMessage());
            }
        }

        LoginResponse response = LoginResponse.newBuilder()
                .setSuccess(succes)
                .setMessage(message)
                .setJetonSession(jeton)
                .setRole(role)
                .setDoitChangerMdp(doitChangerMdp)
                .build();

        response.writeDelimitedTo(socket.getOutputStream());
        socket.getOutputStream().flush();
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

        if (!estOrganisateur(request.getJetonSession())) {
            success = false;
            message = REFUS;
            System.out.println("[SERVEUR] Inscription refusée : pas organisateur");

        } else if (electeurId.isEmpty() || login.isEmpty() || mdpClair.isEmpty()
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
        System.out.println("[SERVEUR] Demande de clé publique reçue.");

        // TODO: Envoyer la PublicKeyResponse
    }

    private static void traiterSuppression(DeleteElecteurRequest request, Socket socket) throws IOException {
        System.out.println("[SERVEUR] Suppression demandée pour : " + request.getElecteurId());
        String electeurId = request.getElecteurId().trim();
        boolean success;
        String message;

        if (!estOrganisateur(request.getJetonSession())) {
            success = false;
            message = REFUS;
            System.out.println("[SERVEUR] Suppression refusée : pas organisateur");

        } else {
            try {
                message = AuthentificationDAO.supprimerElecteur(electeurId);
                success = message.equals("OK");
            } catch (SQLException e) {
                success = false;
                message = "Erreur base de données";
                System.err.println("[SERVEUR] " + e.getMessage());
            }
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

    private static void traiterModification(UpdateElecteurRequest request, Socket socket) throws IOException {
        System.out.println("[SERVEUR] Modification demandée pour : " + request.getElecteurId());

        String electeurId = request.getElecteurId().trim();
        String login = request.getLogin().trim();
        String nom = request.getNom().trim();
        String prenom = request.getPrenom().trim();

        boolean success;
        String message;

        if (!estOrganisateur(request.getJetonSession())) {
            success = false;
            message = REFUS;
            System.out.println("[SERVEUR] Modification refusée : pas organisateur");

        } else if (electeurId.isEmpty() || login.isEmpty() || nom.isEmpty() || prenom.isEmpty()) {
            success = false;
            message = "Échec : tous les champs sont obligatoires.";
        } else {
            try {
                message = AuthentificationDAO.modifierElecteur(electeurId, login, nom, prenom);
                success = message.equals("OK");

                if (success && baseElecteursTemporaire.containsKey(electeurId)) {
                    baseElecteursTemporaire.put(electeurId,
                            new InfosStockageElecteur(electeurId, login, "(en base)"));
                }
            } catch (SQLException e) {
                success = false;
                message = "Erreur base de données";
                System.err.println("[SERVEUR] " + e.getMessage());
            }
        }

        UpdateElecteurResponse response = UpdateElecteurResponse.newBuilder()
                .setSuccess(success)
                .setMessage(message)
                .build();

        response.writeDelimitedTo(socket.getOutputStream());
        socket.getOutputStream().flush();
    }
}
