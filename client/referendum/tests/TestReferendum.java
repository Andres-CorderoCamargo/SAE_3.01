package client.referendum.tests;

import client.referendum.*;

public class TestReferendum {

    private static int ok = 0, ko = 0;

    // Valeurs valides réutilisées dans presque tous les cas (bien sûr, on va les effacer)
    private static final String TOKEN = "abc123";
    private static final String Q = "Faut-il passer à la semaine de 4 jours ?";

    public static void main(String[] args) {
        // Comme ça c'est plus facile de repérer
        testsCreation();
        testsModification();

        System.out.println("\nRésultat : " + ok + " OK, " + ko + " KO");
    }

    private static void testsCreation() {
        // Réponse "succès" de l'urne (l'id 42 vient du serveur, pas du client)
        Message created = new Message(MessageType.REFERENDUM_CREATED);
        created.add("id", "42");

        System.out.println("================ Début test création ================");

        // Cas qui doivent réussir (normalement)
        testerSucces("Création correcte", new fakeConnection(created),
                TOKEN, Q, "Oui", "Non", 10, "42");
        testerSucces("Durée minimale (1 min)", new fakeConnection(created),
                TOKEN, Q, "Oui", "Non", 1, "42");
        testerSucces("Durée maximale (1440 min)", new fakeConnection(created),
                TOKEN, Q, "Oui", "Non", 1440, "42");
        testerSucces("Question de 200 caractères (limite)", new fakeConnection(created),
                TOKEN, "x".repeat(200), "Oui", "Non", 10, "42");

        // Contenu du message envoyé à l'urne
        verifierMessageEnvoye(created);

        // Pas de token : on ne touche pas au réseau
        testerErrorSansReseau("Token null", null, Q, "Oui", "Non", 10,
                ReferendumError.NON_CONNECTE);

        // Champs invalides (validation locale, sans réseau)
        testerErrorSansReseau("Question null", TOKEN, null, "Oui", "Non", 10,
                ReferendumError.CHAMPS_INVALIDES);
        testerErrorSansReseau("Question vide", TOKEN, "", "Oui", "Non", 10,
                ReferendumError.CHAMPS_INVALIDES);
        testerErrorSansReseau("Question d'espaces", TOKEN, "   ", "Oui", "Non", 10,
                ReferendumError.CHAMPS_INVALIDES);
        testerErrorSansReseau("Question trop longue (201)", TOKEN, "x".repeat(201), "Oui", "Non", 10,
                ReferendumError.CHAMPS_INVALIDES);
        testerErrorSansReseau("Choix 1 vide", TOKEN, Q, "", "Non", 10,
                ReferendumError.CHAMPS_INVALIDES);
        testerErrorSansReseau("Choix 0 vide", TOKEN, Q, "Oui", " ", 10,
                ReferendumError.CHAMPS_INVALIDES);
        testerErrorSansReseau("Choix identiques", TOKEN, Q, "Oui", "Oui", 10,
                ReferendumError.CHAMPS_INVALIDES);
        testerErrorSansReseau("Choix identiques (casse différente)", TOKEN, Q, "Oui", " oui ", 10,
                ReferendumError.CHAMPS_INVALIDES);
        testerErrorSansReseau("Durée 0", TOKEN, Q, "Oui", "Non", 0,
                ReferendumError.CHAMPS_INVALIDES);
        testerErrorSansReseau("Durée négative", TOKEN, Q, "Oui", "Non", -5,
                ReferendumError.CHAMPS_INVALIDES);
        testerErrorSansReseau("Durée trop longue (1441)", TOKEN, Q, "Oui", "Non", 1441,
                ReferendumError.CHAMPS_INVALIDES);

        // Erreurs renvoyées par l'urne
        testerError("Serveur : INVALID_TOKEN", new fakeConnection(erreur("INVALID_TOKEN")),
                TOKEN, Q, "Oui", "Non", 10, ReferendumError.NON_CONNECTE);
        testerError("Serveur : NOT_AUTHORIZED", new fakeConnection(erreur("NOT_AUTHORIZED")),
                TOKEN, Q, "Oui", "Non", 10, ReferendumError.NON_AUTORISE);
        testerError("Serveur : ELECTION_ALREADY_RUNNING", new fakeConnection(erreur("ELECTION_ALREADY_RUNNING")),
                TOKEN, Q, "Oui", "Non", 10, ReferendumError.REFERENDUM_DEJA_EN_COURS);
        testerError("Serveur : BAD_FORMAT", new fakeConnection(erreur("BAD_FORMAT")),
                TOKEN, Q, "Oui", "Non", 10, ReferendumError.CHAMPS_INVALIDES);
        testerError("Serveur : code inconnu", new fakeConnection(erreur("QUELQUE_CHOSE")),
                TOKEN, Q, "Oui", "Non", 10, ReferendumError.ERREUR_SERVEUR);
        testerError("Serveur : ERROR sans code", new fakeConnection(erreur(null)),
                TOKEN, Q, "Oui", "Non", 10, ReferendumError.ERREUR_SERVEUR);

        // Problèmes de communication
        testerError("Timeout (receive renvoie null)", new fakeConnection(null),
                TOKEN, Q, "Oui", "Non", 10, ReferendumError.TIMEOUT);
        testerError("Panne réseau (IOException)", new fakeConnection(null, true),
                TOKEN, Q, "Oui", "Non", 10, ReferendumError.ERREUR_RESEAU);
        testerError("Réponse inattendue (LOGIN_OK)", new fakeConnection(new Message(MessageType.LOGIN_OK)),
                TOKEN, Q, "Oui", "Non", 10, ReferendumError.REPONSE_INATTENDUE);

        System.out.println("================ Fin test création ================");
    }

    // Tous les tests de la méthode modifier()
    private static void testsModification() {
        Message modifie = new Message(MessageType.REFERENDUM_MODIFIED);

        System.out.println("================ Début test modification ================");

        // Succès
        testerModifSucces("Modif correcte", new fakeConnection(modifie),
                TOKEN, "42", Q, "Pour", "Contre", 15);

        // Contenu du message envoyé
        verifierMessageModification(modifie);

        // Sans réseau
        testerModifSansReseau("Modif : token null", null, "42", Q, "Oui", "Non", 10,
                ReferendumError.NON_CONNECTE);
        testerModifSansReseau("Modif : id null", TOKEN, null, Q, "Oui", "Non", 10,
                ReferendumError.CHAMPS_INVALIDES);
        testerModifSansReseau("Modif : id vide", TOKEN, "  ", Q, "Oui", "Non", 10,
                ReferendumError.CHAMPS_INVALIDES);
        testerModifSansReseau("Modif : question vide", TOKEN, "42", "", "Oui", "Non", 10,
                ReferendumError.CHAMPS_INVALIDES);
        testerModifSansReseau("Modif : choix identiques", TOKEN, "42", Q, "Oui", "oui", 10,
                ReferendumError.CHAMPS_INVALIDES);
        testerModifSansReseau("Modif : durée 0", TOKEN, "42", Q, "Oui", "Non", 0,
                ReferendumError.CHAMPS_INVALIDES);
        testerModifSansReseau("Modif : durée 1441", TOKEN, "42", Q, "Oui", "Non", 1441,
                ReferendumError.CHAMPS_INVALIDES);

        // Erreurs renvoyées par l'urne
        testerModifError("Modif serveur : INVALID_TOKEN", new fakeConnection(erreur("INVALID_TOKEN")),
                TOKEN, "42", Q, "Oui", "Non", 10, ReferendumError.NON_CONNECTE);
        testerModifError("Modif serveur : NOT_AUTHORIZED", new fakeConnection(erreur("NOT_AUTHORIZED")),
                TOKEN, "42", Q, "Oui", "Non", 10, ReferendumError.NON_AUTORISE);
        testerModifError("Modif serveur : REFERENDUM_NOT_FOUND", new fakeConnection(erreur("REFERENDUM_NOT_FOUND")),
                TOKEN, "42", Q, "Oui", "Non", 10, ReferendumError.REFERENDUM_INTROUVABLE);
        testerModifError("Modif serveur : VOTES_ALREADY_RECEIVED", new fakeConnection(erreur("VOTES_ALREADY_RECEIVED")),
                TOKEN, "42", Q, "Oui", "Non", 10, ReferendumError.VOTES_DEJA_RECUS);
        testerModifError("Modif serveur : ELECTION_CLOSED", new fakeConnection(erreur("ELECTION_CLOSED")),
                TOKEN, "42", Q, "Oui", "Non", 10, ReferendumError.REFERENDUM_TERMINE);
        testerModifError("Modif serveur : BAD_FORMAT", new fakeConnection(erreur("BAD_FORMAT")),
                TOKEN, "42", Q, "Oui", "Non", 10, ReferendumError.CHAMPS_INVALIDES);
        testerModifError("Modif serveur : code inconnu", new fakeConnection(erreur("QUELQUE_CHOSE")),
                TOKEN, "42", Q, "Oui", "Non", 10, ReferendumError.ERREUR_SERVEUR);

        // Communication
        testerModifError("Modif : timeout", new fakeConnection(null),
                TOKEN, "42", Q, "Oui", "Non", 10, ReferendumError.TIMEOUT);
        testerModifError("Modif : panne réseau", new fakeConnection(null, true),
                TOKEN, "42", Q, "Oui", "Non", 10, ReferendumError.ERREUR_RESEAU);

        // Hors protocole
        Message creeParErreur = new Message(MessageType.REFERENDUM_CREATED);
        creeParErreur.add("id", "42");
        testerModifError("Modif : réponse REFERENDUM_CREATED", new fakeConnection(creeParErreur),
                TOKEN, "42", Q, "Oui", "Non", 10, ReferendumError.REPONSE_INATTENDUE);

        System.out.println("================ Fin test modification ================");
    }

    // Vérifie que le service construit bien le message CREATE_REFERENDUM
    // (types, champs, espaces retirés par trim, durée convertie en texte)
    private static void verifierMessageEnvoye(Message reponse) {
        fakeConnection c = new fakeConnection(reponse);
        try {
            new ReferendumService(c).creer(TOKEN, "  " + Q + "  ", " Oui ", " Non ", 10);
        } catch (ReferendumException e) {
            verifier("Message envoyé : exception inattendue " + e.getError(), false);
            return;
        }
        Message m = c.lastSent;
        verifier("Message envoyé : un message a été envoyé", m != null);
        if (m == null) return;
        verifier("Message envoyé : une seule fois", c.sendCount == 1);
        verifier("Message envoyé : type CREATE_REFERENDUM", m.getType() == MessageType.CREATE_REFERENDUM);
        verifier("Message envoyé : token", TOKEN.equals(m.get("token")));
        verifier("Message envoyé : question sans espaces autour", Q.equals(m.get("question")));
        verifier("Message envoyé : option1 sans espaces", "Oui".equals(m.get("option1")));
        verifier("Message envoyé : option0 sans espaces", "Non".equals(m.get("option0")));
        verifier("Message envoyé : durée en texte", "10".equals(m.get("duree")));
    }

    // Vérifie le message MODIFY_REFERENDUM construit par le service
    private static void verifierMessageModification(Message reponse) {
        fakeConnection c = new fakeConnection(reponse);
        try {
            new ReferendumService(c).modifier(TOKEN, "42", "  " + Q + "  ", " Pour ", " Contre ", 20);
        } catch (ReferendumException e) {
            verifier("Message modif : exception inattendue " + e.getError(), false);
            return;
        }
        Message m = c.lastSent;
        verifier("Message modif : un message a été envoyé", m != null);
        if (m == null) return;
        verifier("Message modif : une seule fois", c.sendCount == 1);
        verifier("Message modif : type MODIFY_REFERENDUM", m.getType() == MessageType.MODIFY_REFERENDUM);
        verifier("Message modif : token", TOKEN.equals(m.get("token")));
        verifier("Message modif : id", "42".equals(m.get("id")));
        verifier("Message modif : question sans espaces autour", Q.equals(m.get("question")));
        verifier("Message modif : option1 sans espaces", "Pour".equals(m.get("option1")));
        verifier("Message modif : option0 sans espaces", "Contre".equals(m.get("option0")));
        verifier("Message modif : durée en texte", "20".equals(m.get("duree")));
    }

    // Fabrique une réponse ERROR (code = null → ERROR sans champ code)
    private static Message erreur(String code) {
        Message m = new Message(MessageType.ERROR);
        if (code != null) m.add("code", code);
        return m;
    }

    private static void testerSucces(String nom, Connection c, String token,
                                     String q, String o1, String o0, int duree,
                                     String idAttendu) {
        try {
            String id = new ReferendumService(c).creer(token, q, o1, o0, duree);
            verifier(nom, idAttendu.equals(id));
        } catch (ReferendumException e) {
            verifier(nom + " (exception inattendue : " + e.getError() + ")", false);
        }
    }

    private static void testerError(String nom, Connection c, String token,
                                    String q, String o1, String o0, int duree,
                                    ReferendumError attendu) {
        try {
            new ReferendumService(c).creer(token, q, o1, o0, duree);
            verifier(nom + " (devait lancer une exception)", false);
        } catch (ReferendumException e) {
            verifier(nom + " -> " + e.getError(), e.getError() == attendu);
        }
    }

    // Comme testerError, mais vérifie aussi que send() n'a JAMAIS été appelé.
    // On donne une réponse « succès » : si la validation locale manquait,
    // l'appel réussirait et le test passerait en [KO].
    private static void testerErrorSansReseau(String nom, String token, String q,
                                              String o1, String o0, int duree,
                                              ReferendumError attendu) {
        Message created = new Message(MessageType.REFERENDUM_CREATED);
        created.add("id", "42");
        fakeConnection c = new fakeConnection(created);
        testerError(nom, c, token, q, o1, o0, duree, attendu);
        verifier(nom + " : réseau non utilisé", !c.sendCalled);
    }

    private static void testerModifSucces(String nom, Connection c, String token, String id,
                                          String q, String o1, String o0, int duree) {
        try {
            new ReferendumService(c).modifier(token, id, q, o1, o0, duree);
            verifier(nom, true);   // pas d'exception = succès
        } catch (ReferendumException e) {
            verifier(nom + " (exception inattendue : " + e.getError() + ")", false);
        }
    }

    private static void testerModifError(String nom, Connection c, String token, String id,
                                         String q, String o1, String o0, int duree,
                                         ReferendumError attendu) {
        try {
            new ReferendumService(c).modifier(token, id, q, o1, o0, duree);
            verifier(nom + " (devait lancer une exception)", false);
        } catch (ReferendumException e) {
            verifier(nom + " -> " + e.getError(), e.getError() == attendu);
        }
    }

    // Comme testerModifError, mais vérifie aussi que send() n'a jamais été appelé
    private static void testerModifSansReseau(String nom, String token, String id,
                                              String q, String o1, String o0, int duree,
                                              ReferendumError attendu) {
        fakeConnection c = new fakeConnection(new Message(MessageType.REFERENDUM_MODIFIED));
        testerModifError(nom, c, token, id, q, o1, o0, duree, attendu);
        verifier(nom + " : réseau non utilisé", !c.sendCalled);
    }

    private static void verifier(String nom, boolean condition) {
        if (condition) {
            ok++;
            System.out.println("[OK] " + nom);
        }
        else {
            ko++;
            System.out.println("[KO] " + nom);
        }
    }
}