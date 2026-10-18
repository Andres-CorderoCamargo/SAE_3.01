package client.referendum;

import java.io.IOException;

public class ReferendumService {

    private static final int DEFAULT_TIMEOUT_MS = 5000;
    private static final int QUESTION_MAX = 200;
    private static final int DUREE_MAX_MIN = 1440;   // 24 h

    private final Connection connection;
    private final int timeoutMs;

    public ReferendumService(Connection connection) {
        this(connection, DEFAULT_TIMEOUT_MS);
    }

    // Pour tests :
    public ReferendumService(Connection connection, int timeoutMs) {
        this.connection = connection;
        this.timeoutMs = timeoutMs;
    }

    /** Crée le référendum et renvoie son identifiant. */
    public String creer(String token, String question, String option1,
                        String option0, int dureeMinutes) throws ReferendumException {
        if (token == null) { // Sans token donc l'utilisateur n'est pas connecté.
            throw new ReferendumException(ReferendumError.NON_CONNECTE);
        }
        // Validation des champs (aussi sans réseau)
        validerChamps(question, option1, option0, dureeMinutes);

        // Quand tout est ok (les tests(?) d'avant), on passe à construire le message CREATE_REFERENDUM avec tous ses champs
        try {
            Message m = new Message(MessageType.CREATE_REFERENDUM);
            m.add("token", token);
            m.add("question", question.trim());
            m.add("option1", option1.trim());
            m.add("option0", option0.trim());
            m.add("duree", String.valueOf(dureeMinutes));
            connection.send(m);

            // On attend la réponse de l'urne
            Message r = connection.receive(timeoutMs);
            if (r == null) {
                throw new ReferendumException(ReferendumError.TIMEOUT);
            }
            // Le système analyse le message reçu (fonction plus en bas)
            return interpreter(r);

        } catch (IOException e) { // Pour eviter problèmes et annoncer la raison.
            throw new ReferendumException(ReferendumError.ERREUR_RESEAU, e);
        }
    }

    private void validerChamps(String question, String o1, String o0, int duree)
            throws ReferendumException {
        boolean invalide = question == null
                    || question.isBlank()
                    || question.length() > QUESTION_MAX
                    // les deux libellés sont obligatoires (o1 et o2) mais je ne sais pas si faire comme ça ou dire directement 0 = non, 1 = oui...
                    || o1 == null
                    || o1.isBlank()
                    || o0 == null
                    || o0.isBlank()
                    // les deux choix doivent être différents, sinon le vote n'a pas de sens
                    || o1.trim().equalsIgnoreCase(o0.trim())
                    // durée entre 1 minute et 24 h
                    || duree < 1
                    || duree > DUREE_MAX_MIN;
        if (invalide) {
            throw new ReferendumException(ReferendumError.CHAMPS_INVALIDES);
        }
    }

    private String interpreter(Message r) throws ReferendumException {
        switch (r.getType()) {
            // l'urne renvoie l'identifiant du référendum créé
            case REFERENDUM_CREATED:
                return r.get("id");
            // Sinon... paw !
            case ERROR:
                String code = r.get("code");
                if ("INVALID_TOKEN".equals(code)) {
                    throw new ReferendumException(ReferendumError.NON_CONNECTE);
                }
                if ("NOT_AUTHORIZED".equals(code)) {
                    throw new ReferendumException(ReferendumError.NON_AUTORISE);
                }
                if ("ELECTION_ALREADY_RUNNING".equals(code)) {
                    throw new ReferendumException(ReferendumError.REFERENDUM_DEJA_EN_COURS);
                }
                if ("BAD_FORMAT".equals(code)) {
                    throw new ReferendumException(ReferendumError.CHAMPS_INVALIDES);
                }
                throw new ReferendumException(ReferendumError.ERREUR_SERVEUR);
            default: // Celui-là c'est si jamais on ne sais pas ce qui se passe :b
                throw new ReferendumException(ReferendumError.REPONSE_INATTENDUE);
        }
    }
}