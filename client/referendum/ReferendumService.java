package client.referendum;

import client.commun.*;
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
        // Validation locale des champs (sans réseau)
        validerChamps(question, option1, option0, dureeMinutes);

        // Construction du message, puis échange complet avec l'urne
        Message m = construireMessage(MessageType.CREATE_REFERENDUM, token,
                question, option1, option0, dureeMinutes);
        return echanger(m, MessageType.REFERENDUM_CREATED).get("id");
    }

    public void modifier(String token, String id, String question, String option1,
                         String option0, int dureeMinutes) throws ReferendumException {
        // Pas de réseau
        if (token == null) {
            throw new ReferendumException(ReferendumError.NON_CONNECTE);
        }
        // L'identifiant du référendum à modifier est obligatoire (sinon, ça n'a pas de sens).
        if (id == null || id.isBlank()) {
            throw new ReferendumException(ReferendumError.CHAMPS_INVALIDES);
        }
        // Mêmes règles de validation que pour la création
        validerChamps(question, option1, option0, dureeMinutes);

        Message m = construireMessage(MessageType.MODIFY_REFERENDUM, token,
                question, option1, option0, dureeMinutes);
        m.add("id", id);

        // On attend REFERENDUM_MODIFIED ; la réponse n'a pas de contenu utile
        echanger(m, MessageType.REFERENDUM_MODIFIED);
    }

    private Message construireMessage(MessageType type, String token, String question,
                                      String option1, String option0, int dureeMinutes) {
        Message m = new Message(type);
        m.add("token", token);
        m.add("question", question.trim());
        m.add("option1", option1.trim());
        m.add("option0", option0.trim());
        m.add("duree", String.valueOf(dureeMinutes));
        return m;
    }

    private Message echanger(Message m, MessageType succes) throws ReferendumException {
        try {
            connection.send(m);

            Message r = connection.receive(timeoutMs);
            if (r == null) {
                throw new ReferendumException(ReferendumError.TIMEOUT);
            }
            return interpreter(r, succes);

        } catch (IOException e) {
            // Panne réseau : on la transforme en erreur métier en gardant la cause
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

    private Message interpreter(Message r, MessageType succes) throws ReferendumException {
        if (r.getType() == succes) {
            return r;
        }
        // Ni succès ni ERROR (par exemple un LOGIN_OK) : réponse hors protocole
        if (r.getType() != MessageType.ERROR) {
            throw new ReferendumException(ReferendumError.REPONSE_INATTENDUE);
        }
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
        if ("REFERENDUM_NOT_FOUND".equals(code)) {
            throw new ReferendumException(ReferendumError.REFERENDUM_INTROUVABLE);
        }
        if ("VOTES_ALREADY_RECEIVED".equals(code)) {
            throw new ReferendumException(ReferendumError.VOTES_DEJA_RECUS);
        }
        if ("ELECTION_CLOSED".equals(code)) {
            throw new ReferendumException(ReferendumError.REFERENDUM_TERMINE);
        }
        if ("BAD_FORMAT".equals(code)) {
            throw new ReferendumException(ReferendumError.CHAMPS_INVALIDES);
        }
        throw new ReferendumException(ReferendumError.ERREUR_SERVEUR);
    }
}