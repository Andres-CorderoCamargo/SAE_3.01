package electeur;

import java.io.IOException;
import java.net.Socket;

public class electeur {

    public static void main(String[] args) {
        String serveurHost = "127.0.0.1";
        int serveurPort = 8080;

        System.out.println("[CLIENT] Connexion au serveur de vote...");

        try (Socket socket = new Socket(serveurHost, serveurPort)) {
            System.out.println("[CLIENT] Connecte.");

            // L'organisateur se connecte, puis ajoute un electeur.
            // Un electeur ne peut pas s'inscrire lui-meme : la liste
            // electorale est fermee, sinon on ne peut pas garantir
            // qu'un electeur ne vote qu'une seule fois.
            String token = ElecteurService.login(socket, "gasquet", "MonMotDePasse123");

            if (token == null) {
                System.out.println("[CLIENT] Connexion refusee, on s'arrete la.");
                return;
            }

            boolean ok = ElecteurService.ajouterElecteur(
                    socket, token,
                    "E0001",              // 5 caracteres maximum
                    "jean.dupont",
                    "MotDePasseProvisoire",
                    "Dupont", "Jean");

            System.out.println(ok
                    ? "[CLIENT] Test reussi : l'electeur est ajoute."
                    : "[CLIENT] Test echoue : l'ajout a ete refuse.");

        } catch (IOException e) {
            System.err.println("[CLIENT] Serveur injoignable (est-il demarre ?) : " + e.getMessage());
        }
    }
}
