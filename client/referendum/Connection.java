package client.referendum;

import java.io.IOException;

/**
 * Représente une connexion réseau (Gere l'envoi et la réception de messages).
 */
public interface Connection extends AutoCloseable {

    /**
     * Envoie un message via la connexion. (évident xd)
     */
    void send(Message m) throws IOException;

    /**
     * Reçoit un message en attendant au maximum pendant le délai imparti.
     */
    Message receive(int timeoutMs) throws IOException;

    /**
     * Ferme la connexion et libère les ressources associées.
     */
    @Override
    void close() throws IOException;
}