package client.commun;

public enum MessageType {
    LOGIN,  // Connection avec l'urne (?)
    LOGIN_OK,  // Connection réussie
    CREATE_REFERENDUM, // Demande de création
    REFERENDUM_CREATED, // Pour la confirmation (que je n'ai pas trop compris comment montrer, mais ok)
    ERROR, // Evident xd
    MODIFY_REFERENDUM,  //
    REFERENDUM_MODIFIED,  //
}
