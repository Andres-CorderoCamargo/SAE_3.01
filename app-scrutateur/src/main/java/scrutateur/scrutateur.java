package scrutateur;

import java.io.IOException;
import java.net.Socket;

public class scrutateur {

    public static void main(String[] args) {
        String serveurHost = "127.0.0.1";
        int serveurPort = 8080;

        System.out.println("[SCRUTATEUR] Connexion au serveur de vote...");

        try (Socket socket = new Socket(serveurHost, serveurPort)) {
            System.out.println("[SCRUTATEUR] Connecté au serveur.");

            boolean continuer = true;

            while (continuer) {
                int choix = SaisieScrutateur.afficherMenuEtChoisir();

                switch (choix) {
                    case 1:
                        SaisieScrutateur.InfosConnexion id =
                                SaisieScrutateur.demanderInformationsConnexion();
                        String resultat = ScrutateurService.seConnecter(
                                socket, id.getLogin(), id.getMotDePasse());
                        System.out.println("[SCRUTATEUR] " + resultat);
                        break;

                    case 2:
                        System.out.println("[SCRUTATEUR] Génération des clés : à venir.");
                        break;

                    case 3:
                        System.out.println("[SCRUTATEUR] Envoi de la clé publique : à venir.");
                        break;

                    case 0:
                        continuer = false;
                        System.out.println("[SCRUTATEUR] Au revoir.");
                        break;

                    default:
                        System.out.println("[SCRUTATEUR] Choix invalide.");
                }
            }

        } catch (IOException e) {
            System.err.println("[SCRUTATEUR] Impossible de joindre le serveur : " + e.getMessage());
        }
    }
}
