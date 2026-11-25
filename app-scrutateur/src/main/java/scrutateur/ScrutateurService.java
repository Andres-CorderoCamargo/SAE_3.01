package scrutateur;

import com.g3.vote.proto.ClientMessage;
import com.g3.vote.proto.LoginRequest;
import com.g3.vote.proto.LoginResponse;

import java.io.IOException;
import java.net.Socket;

/** Les echanges avec le serveur. Le scrutateur est une application
 *  independante : elle a son propre service, comme l'application electeur. */
public class ScrutateurService {

    private static String jetonSession = "";

    public static String getJetonSession() {
        return jetonSession;
    }

    public static String seConnecter(Socket socket, String login, String motDePasse) {
        try {
            LoginRequest request = LoginRequest.newBuilder()
                    .setLogin(login)
                    .setMotDePasse(motDePasse)
                    .build();

            ClientMessage clientMsg = ClientMessage.newBuilder()
                    .setLoginRequest(request)
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

            if (!response.getRole().equals("SCRUTATEUR")) {
                return "Ce compte n'est pas un compte scrutateur (rôle " + response.getRole() + ")";
            }

            jetonSession = response.getJetonSession();
            return "Connecté en tant que SCRUTATEUR";

        } catch (IOException e) {
            System.err.println("Erreur réseau/flux : " + e.getMessage());
            return "Erreur réseau";
        }
    }
}
