package client.auth.java.test;

import client.auth.java.*;
import client.commun.*;

public class testAuthentification {

    private static int ok = 0, ko = 0;

    public static void main(String[] args) {
        // Désolé s'il y a des mots en espagnol, lol.
        // 1. Mdp correcte
        Message loginOk = new Message(MessageType.LOGIN_OK);
        loginOk.add("token", "abc123");
        testerSucces("Identification réussie", new fakeConnection(loginOk),
                "alice", "1234", "abc123");

        // 2. Mdp incorrecte
        Message badCred = new Message(MessageType.ERROR);
        badCred.add("code", "BAD_CREDENTIALS");
        testerEchec("Mdp incorrecte", new fakeConnection(badCred),
                "alice", "mal", LoginError.IDENTIFIANT_OU_MDP_INCORRECT);

        // 3. Compte bloquée
        Message bloque = new Message(MessageType.ERROR);
        bloque.add("code", "TOO_MANY_ATTEMPTS");
        testerEchec("Compte bloquée", new fakeConnection(bloque),
                "alice", "1234", LoginError.COMPTE_BLOQUE);

        // 4. Error de servidor desconocido
        Message otro = new Message(MessageType.ERROR);
        otro.add("code", "QUELQUE_CHOSE");
        testerEchec("Error de servidor", new fakeConnection(otro),
                "alice", "1234", LoginError.ERREUR_SERVEUR);

        // 5. Timeout (receive devuelve null)
        testerEchec("Timeout", new fakeConnection(null),
                "alice", "1234", LoginError.TIMEOUT);

        // 6. Champs vides (+ réseau)
        fakeConnection sinRed = new fakeConnection(null);
        testerEchec("Login vide", sinRed, "", "1234", LoginError.CHAMPS_VIDES);
        verifier("Login vide, pas de réseau", !sinRed.sendCalled);
        testerEchec("Mdp vide", new fakeConnection(null),
                "alice", "", LoginError.CHAMPS_VIDES);

        // 7. Erreur de réseau
        testerEchec("Erreur de réseau", new fakeConnection(null, true),
                "alice", "1234", LoginError.ERREUR_RESEAU);

        // 8. Réponse inattendue
        testerEchec("Réponse inattendue", new fakeConnection(new Message(MessageType.LOGIN)),
                "alice", "1234", LoginError.REPONSE_INATTENDUE);

        // 9. Trop d'essais
        LoginService s = new LoginService(new fakeConnection(badCred));
        for (int i = 0; i < 2; i++) {
            try { s.login("alice", "mal".toCharArray()); } catch (LoginException e) { }
        }
        verifier("nbEssaie == 2 après 2 échecs", s.getNbEssaie() == 2);

        LoginService s2 = new LoginService(new fakeConnection(loginOk));
        try { s2.login("alice", "1234".toCharArray()); } catch (LoginException e) { }
        verifier("isLoggedIn après login", s2.isLoggedIn());
        s2.logout();
        verifier("pas isLoggedIn après logout", !s2.isLoggedIn());

        // 10. Le mdp effacé de la mémoire
        char[] pwd = "secret".toCharArray();
        try { new LoginService(new fakeConnection(loginOk)).login("alice", pwd); } catch (LoginException e) { }
        verifier("Mdp effacée", pwd[0] == '\0');

        System.out.println("\nRésultat : " + ok + " OK, " + ko + " KO");
    }

    private static void testerSucces(String nom, Connection c,
                                     String login, String pwd, String tokenAttendu) {
        try {
            String token = new LoginService(c).login(login, pwd.toCharArray());
            verifier(nom, tokenAttendu.equals(token));
        } catch (LoginException e) {
            verifier(nom + " (Exception: " + e.getError() + ")", false);
        }
    }

    private static void testerEchec(String nom, Connection c,
                                    String login, String pwd, LoginError attendu) {
        try {
            new LoginService(c).login(login, pwd.toCharArray());
            verifier(nom + " (doit lancer exception)", false);
        } catch (LoginException e) {
            verifier(nom + " -> " + e.getError(), e.getError() == attendu);
        }
    }

    private static void verifier(String nom, boolean condition) {
        if (condition) { ok++; System.out.println("[OK] " + nom); }
        else           { ko++; System.out.println("[KO] " + nom); }
    }
}