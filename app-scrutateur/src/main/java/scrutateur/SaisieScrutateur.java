package scrutateur;

import java.util.Scanner;

import java.io.Console;

/** Tout ce qui est saisi au clavier par le scrutateur. */
public class SaisieScrutateur {

    private static final Scanner scanner = new Scanner(System.in);

    public static int afficherMenuEtChoisir() {
        System.out.println("\n=== MENU SCRUTATEUR ===");
        System.out.println("1. Se connecter");
        System.out.println("2. Générer mes clés (à venir)");
        System.out.println("3. Envoyer ma clé publique (à venir)");
        System.out.println("0. Quitter");
        System.out.print("Faites votre choix : ");
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Les deux champs de la connexion.
     */
    public static class InfosConnexion {
        private final String login;
        private final String motDePasse;

        public InfosConnexion(String login, String motDePasse) {
            this.login = login;
            this.motDePasse = motDePasse;
        }

        public String getLogin() {
            return login;
        }

        public String getMotDePasse() {
            return motDePasse;
        }
    }

    public static InfosConnexion demanderInformationsConnexion() {
        System.out.println("\n--- CONNEXION ---");
        System.out.print("Login : ");
        String login = scanner.nextLine().trim();
        String mdp = lireMotDePasse("Mot de passe : ");
        return new InfosConnexion(login, mdp);
    }

    /**
     * Lit un mot de passe sans l'afficher a l'ecran.
     */
    private static String lireMotDePasse(String invite) {
        Console console = System.console();
        if (console == null) {
            System.out.println("(attention : saisie visible, pas de console disponible)");
            System.out.print(invite);
            return scanner.nextLine().trim();
        }
        return new String(console.readPassword(invite)).trim();
    }
}
