package client.auth.java;

public class LoginException extends Exception {

    // Stocke le type d'erreur précis (rendu immutable avec 'final')
    private final LoginError error;

    public LoginException(LoginError error) {
        super(error.name()); // Utilise le nom de l'énumération comme message de l'exception
        this.error = error;
    }

    /**
     * Crée une exception avec un code d'erreur et une cause sous-jacente (ex: IOException).
     */
    public LoginException(LoginError error, Throwable cause) {
        super(error.name(), cause);
        this.error = error;
    }

    public LoginError getError() {
        return error;
    }
}