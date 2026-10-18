package client.referendum.tests;

import client.referendum.*;

public class TestReferendum {

    private static int ok = 0, ko = 0;

    // Valeurs valides réutilisées dans presque tous les cas (bien sûr, on va les effacer)
    private static final String TOKEN = "abc123";
    private static final String Q = "Faut-il passer à la semaine de 4 jours ?";

    public static void main(String[] args) {

        // Réponse "succès" de l'urne (l'id 42 vient du serveur, pas du client)
        Message created = new Message(MessageType.REFERENDUM_CREATED);
        created.add("id", "42");

        // Cas qui doivent réussir (normalement)
        probarSucces("Création correcte", new fakeConnection(created),
                TOKEN, Q, "Oui", "Non", 10, "42");
        probarSucces("Durée minimale (1 min)", new fakeConnection(created),
                TOKEN, Q, "Oui", "Non", 1, "42");
        probarSucces("Durée maximale (1440 min)", new fakeConnection(created),
                TOKEN, Q, "Oui", "Non", 1440, "42");
        probarSucces("Question de 200 caractères (limite)", new fakeConnection(created),
                TOKEN, "x".repeat(200), "Oui", "Non", 10, "42");

        // Contenu du message envoyé à l'urne
        verifierMessageEnvoye(created);

        // Pas de token : on ne touche pas au réseau
        probarErrorSansReseau("Token null", null, Q, "Oui", "Non", 10,
                ReferendumError.NON_CONNECTE);

        // Champs invalides (validation locale, sans réseau)
        probarErrorSansReseau("Question null", TOKEN, null, "Oui", "Non", 10,
                ReferendumError.CHAMPS_INVALIDES);
        probarErrorSansReseau("Question vide", TOKEN, "", "Oui", "Non", 10,
                ReferendumError.CHAMPS_INVALIDES);
        probarErrorSansReseau("Question d'espaces", TOKEN, "   ", "Oui", "Non", 10,
                ReferendumError.CHAMPS_INVALIDES);
        probarErrorSansReseau("Question trop longue (201)", TOKEN, "x".repeat(201), "Oui", "Non", 10,
                ReferendumError.CHAMPS_INVALIDES);
        probarErrorSansReseau("Choix 1 vide", TOKEN, Q, "", "Non", 10,
                ReferendumError.CHAMPS_INVALIDES);
        probarErrorSansReseau("Choix 0 vide", TOKEN, Q, "Oui", " ", 10,
                ReferendumError.CHAMPS_INVALIDES);
        probarErrorSansReseau("Choix identiques", TOKEN, Q, "Oui", "Oui", 10,
                ReferendumError.CHAMPS_INVALIDES);
        probarErrorSansReseau("Choix identiques (casse différente)", TOKEN, Q, "Oui", " oui ", 10,
                ReferendumError.CHAMPS_INVALIDES);
        probarErrorSansReseau("Durée 0", TOKEN, Q, "Oui", "Non", 0,
                ReferendumError.CHAMPS_INVALIDES);
        probarErrorSansReseau("Durée négative", TOKEN, Q, "Oui", "Non", -5,
                ReferendumError.CHAMPS_INVALIDES);
        probarErrorSansReseau("Durée trop longue (1441)", TOKEN, Q, "Oui", "Non", 1441,
                ReferendumError.CHAMPS_INVALIDES);

        // Erreurs renvoyées par l'urne
        probarError("Serveur : INVALID_TOKEN", new fakeConnection(erreur("INVALID_TOKEN")),
                TOKEN, Q, "Oui", "Non", 10, ReferendumError.NON_CONNECTE);
        probarError("Serveur : NOT_AUTHORIZED", new fakeConnection(erreur("NOT_AUTHORIZED")),
                TOKEN, Q, "Oui", "Non", 10, ReferendumError.NON_AUTORISE);
        probarError("Serveur : ELECTION_ALREADY_RUNNING", new fakeConnection(erreur("ELECTION_ALREADY_RUNNING")),
                TOKEN, Q, "Oui", "Non", 10, ReferendumError.REFERENDUM_DEJA_EN_COURS);
        probarError("Serveur : BAD_FORMAT", new fakeConnection(erreur("BAD_FORMAT")),
                TOKEN, Q, "Oui", "Non", 10, ReferendumError.CHAMPS_INVALIDES);
        probarError("Serveur : code inconnu", new fakeConnection(erreur("QUELQUE_CHOSE")),
                TOKEN, Q, "Oui", "Non", 10, ReferendumError.ERREUR_SERVEUR);
        probarError("Serveur : ERROR sans code", new fakeConnection(erreur(null)),
                TOKEN, Q, "Oui", "Non", 10, ReferendumError.ERREUR_SERVEUR);

        // Problèmes de communication
        probarError("Timeout (receive renvoie null)", new fakeConnection(null),
                TOKEN, Q, "Oui", "Non", 10, ReferendumError.TIMEOUT);
        probarError("Panne réseau (IOException)", new fakeConnection(null, true),
                TOKEN, Q, "Oui", "Non", 10, ReferendumError.ERREUR_RESEAU);
        probarError("Réponse inattendue (LOGIN_OK)", new fakeConnection(new Message(MessageType.LOGIN_OK)),
                TOKEN, Q, "Oui", "Non", 10, ReferendumError.REPONSE_INATTENDUE);

        System.out.println("\nRésultat : " + ok + " OK, " + ko + " KO");
    }

    // Vérifie que le service construit bien le message CREATE_REFERENDUM
    // (types, champs, espaces retirés par trim, durée convertie en texte)
    private static void verifierMessageEnvoye(Message reponse) {
        fakeConnection c = new fakeConnection(reponse);
        try {
            new ReferendumService(c).creer(TOKEN, "  " + Q + "  ", " Oui ", " Non ", 10);
        } catch (ReferendumException e) {
            verificar("Message envoyé : exception inattendue " + e.getError(), false);
            return;
        }
        Message m = c.lastSent;
        verificar("Message envoyé : un message a été envoyé", m != null);
        if (m == null) return;
        verificar("Message envoyé : type CREATE_REFERENDUM", m.getType() == MessageType.CREATE_REFERENDUM);
        verificar("Message envoyé : token", TOKEN.equals(m.get("token")));
        verificar("Message envoyé : question sans espaces autour", Q.equals(m.get("question")));
        verificar("Message envoyé : option1 sans espaces", "Oui".equals(m.get("option1")));
        verificar("Message envoyé : option0 sans espaces", "Non".equals(m.get("option0")));
        verificar("Message envoyé : durée en texte", "10".equals(m.get("duree")));
    }

    // Fabrique une réponse ERROR (code = null → ERROR sans champ code)
    private static Message erreur(String code) {
        Message m = new Message(MessageType.ERROR);
        if (code != null) m.add("code", code);
        return m;
    }

    private static void probarSucces(String nombre, Connection c, String token,
                                     String q, String o1, String o0, int duree,
                                     String idEsperado) {
        try {
            String id = new ReferendumService(c).creer(token, q, o1, o0, duree);
            verificar(nombre, idEsperado.equals(id));
        } catch (ReferendumException e) {
            verificar(nombre + " (exception inattendue : " + e.getError() + ")", false);
        }
    }

    private static void probarError(String nombre, Connection c, String token,
                                    String q, String o1, String o0, int duree,
                                    ReferendumError esperado) {
        try {
            new ReferendumService(c).creer(token, q, o1, o0, duree);
            verificar(nombre + " (devait lancer une exception)", false);
        } catch (ReferendumException e) {
            verificar(nombre + " -> " + e.getError(), e.getError() == esperado);
        }
    }

    // Comme probarError, mais vérifie aussi que send() n'a JAMAIS été appelé.
    // On donne une réponse « succès » : si la validation locale manquait,
    // l'appel réussirait et le test passerait en [KO].
    private static void probarErrorSansReseau(String nombre, String token, String q,
                                              String o1, String o0, int duree,
                                              ReferendumError esperado) {
        Message created = new Message(MessageType.REFERENDUM_CREATED);
        created.add("id", "42");
        fakeConnection c = new fakeConnection(created);
        probarError(nombre, c, token, q, o1, o0, duree, esperado);
        verificar(nombre + " : réseau non utilisé", !c.sendCalled);
    }

    private static void verificar(String nombre, boolean condicion) {
        if (condicion) { ok++; System.out.println("[OK] " + nombre); }
        else           { ko++; System.out.println("[KO] " + nombre); }
    }
}
