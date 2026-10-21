package client.auth.java;

import client.commun.*;
import java.io.IOException;
import java.util.Arrays;

public class LoginService {

    private static final int DEFAULT_TIMEOUT_MS = 5000;

    private final Connection connection;
    private final int timeoutMs;
    private String sessionToken;          // null = déconnecté
    private int nbEssaie = 0;

    public LoginService(Connection connection) {
        this(connection, DEFAULT_TIMEOUT_MS);
    }

    public LoginService(Connection connection, int timeoutMs) {
        this.connection = connection;
        this.timeoutMs = timeoutMs;
    }

    public String login(String login, char[] password) throws LoginException {
        // Validation basique des entrées (logique xd)
        if (login == null || login.isBlank() || password == null || password.length == 0) {
            throw new LoginException(LoginError.CHAMPS_VIDES);
        }

        String passwordStr = new String(password); // Comme ça c'ets mieux pour la comparaison

        try {
            Message m = new Message(MessageType.LOGIN);
            m.add("login", login);
            m.add("password", passwordStr);
            connection.send(m);

            Message response = connection.receive(timeoutMs);
            if (response == null) {
                throw new LoginException(LoginError.TIMEOUT);
            }
            return interpreterReponse(response);

        } catch (IOException e) {
            throw new LoginException(LoginError.ERREUR_RESEAU, e);
        } finally {
            // Nettoyage immédiat du tableau d'origine
            Arrays.fill(password, '\0');
            passwordStr = null;  // Efface :)
        }
    }

    /**
     * Analyse la réponse du serveur et met à jour l'état interne (token, compteurs).
     */
    private String interpreterReponse(Message r) throws LoginException {
        switch (r.getType()) {
            case LOGIN_OK:
                nbEssaie = 0; // Réinitialise les échecs en cas de succès
                sessionToken = r.get("token");
                return sessionToken;
            case ERROR:
                String code = r.get("code");
                if ("BAD_CREDENTIALS".equals(code)) {
                    nbEssaie++;
                    throw new LoginException(LoginError.IDENTIFIANT_OU_MDP_INCORRECT);
                }
                if ("TOO_MANY_ATTEMPTS".equals(code)) {
                    throw new LoginException(LoginError.COMPTE_BLOQUE);
                }
                throw new LoginException(LoginError.ERREUR_SERVEUR);
            default:
                throw new LoginException(LoginError.REPONSE_INATTENDUE);
        }
    }

    public String getToken(){ 
        return sessionToken; 
    }

    public int getNbEssaie(){
        return nbEssaie;
    }

    public boolean isLoggedIn(){
        return sessionToken != null;
    }

    public void logout(){
        sessionToken = null;
    }
}