package electeur;

import java.io.IOException;
import java.net.Socket;

public class electeur {

    public static void main(String[] args) {
        String serveurHost = "127.0.0.1";
        int serveurPort = 8080;

        System.out.println("[CLIENT] Connexion en cours vers le serveur de vote...");

        try (Socket socket = new Socket(serveurHost, serveurPort)) {
            System.out.println("[CLIENT] Connecté avec succès au serveur !");

            boolean continuer = true;

            while (continuer) {
                int choix = SaisieElecteur.afficherMenuEtChoisir();

                switch (choix) {
                    case 1:
                        // Inscription
                        SaisieElecteur.InfosInscription infos = SaisieElecteur.demanderInformationsInscription();
                        boolean reussi = ElecteurService.inscrireElecteur(
                                socket,
                                infos.getElecteurId(),
                                infos.getLogin(),
                                infos.getMdpClair(),
                                infos.getNom(),
                                infos.getPrenom()
                        );
                        if (reussi) {
                            System.out.println("[CLIENT] Inscription confirmée par le serveur.");
                        } else {
                            System.out.println("[CLIENT] Échec de l'inscription.");
                        }
                        break;

                    case 2:
                        // Connexion (exemple de future fonctionnalité)
                        System.out.println("[INFO] La fonctionnalité de connexion est à implémenter dans ElecteurService.");
                        break;

                    case 3:
                        // Vote
                        System.out.println("[INFO] La fonctionnalité de vote est à implémenter dans ElecteurService.");
                        break;

                    case 4:
                        String idASupprimer = SaisieElecteur.demanderIdSuppression();
                        if (idASupprimer.isEmpty()) {
                            System.out.println("[CLIENT] Identifiant vide, suppression annulée.");
                        } else if (SaisieElecteur.demanderConfirmationSuppression(idASupprimer)) {
                            String resultat = ElecteurService.supprimerElecteur(socket, idASupprimer);
                            System.out.println("[CLIENT] " + resultat);
                        } else {
                            System.out.println("[CLIENT] Suppression annulée.");
                        }
                        break;

                    case 5:
                        SaisieElecteur.InfosModification modif = SaisieElecteur.demanderInformationsModification();
                        String resultatModif = ElecteurService.modifierElecteur(
                                socket,
                                modif.getElecteurId(),
                                modif.getLogin(),
                                modif.getNom(),
                                modif.getPrenom()
                        );
                        System.out.println("[CLIENT] " + resultatModif);
                        break;

                    case 0:
                        System.out.println("Fermeture de la session électeur. Au revoir !");
                        continuer = false;
                        break;

                    default:
                        System.out.println("[ERREUR] Option invalide. Veuillez rechoisir.");
                        break;
                }
            }

        } catch (IOException e) {
            System.err.println("[CLIENT] Impossible de se connecter au serveur (est-il démarré ?) : " + e.getMessage());
        }
    }
}