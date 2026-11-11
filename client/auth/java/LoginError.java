package client.auth.java;

public enum LoginError {
    CHAMPS_VIDES,
    TIMEOUT,
    IDENTIFIANT_OU_MDP_INCORRECT,
    COMPTE_BLOQUE,
    ERREUR_SERVEUR,
    REPONSE_INATTENDUE,
    ERREUR_RESEAU
}