package electeur;

import java.util.Scanner;

public class SaisieElecteur {

    private static final Scanner scanner = new Scanner(System.in);
     public static class InfosConnexion {
        private final String login;
        private final String motDePasse;

 public InfosConnexion(String login, String motDePasse) {
            this.login = login;
            this.motDePasse = motDePasse;
        }

        public String getLogin() { return login; }
        public String getMotDePasse() { return motDePasse; }
    }

    public static InfosConnexion demanderInformationsConnexion() {
        System.out.println("\n--- CONNEXION ---");
        System.out.print("Login : ");
        String login = scanner.nextLine().trim();
        System.out.print("Mot de passe : ");
        String motDePasse = scanner.nextLine();
        return new InfosConnexion(login, motDePasse);
    }

 public static class InfosInscription {
     private final String electeurId;
     private final String login;
     private final String mdpClair;
     private final String nom;
     private final String prenom;


     public InfosInscription(String electeurId, String login, String mdpClair,
                             String nom, String prenom) {
         this.electeurId = electeurId;
         this.login = login;
         this.mdpClair = mdpClair;
         this.nom = nom;
         this.prenom = prenom;
     }

    public String getElecteurId() { return electeurId; }
    public String getLogin() { return login; }
    public String getMdpClair() { return mdpClair; }
     public String getNom() { return nom; }
     public String getPrenom() { return prenom; }
}

    public static int afficherMenuEtChoisir() {

        //uniquement l'orga doit pouvoir ajouter un electeur
        //donc on devra modifier l'affichage si la personne est connectee en admin
        System.out.println("\n=== MENU PRINCIPAL ÉLECTEUR ===");
        System.out.println("1. Inscrire un compte électeur");
        System.out.println("2. Se connecter");
        System.out.println("3. Voter (à venir)");
        System.out.println("4. Supprimer un électeur (organisateur)"); // TODO: réserver à l'organisateur
        System.out.println("5. Modifier un électeur (organisateur)");  // TODO: réserver à l'organisateur
        System.out.println("0. Quitter");
        System.out.print("Faites votre choix : ");

        try {
            int choix = Integer.parseInt(scanner.nextLine().trim());
            return choix;
        } catch (NumberFormatException e) {
            return -1; // Choix invalide
        }
    }

    public static InfosInscription demanderInformationsInscription() {
        System.out.println("\n--- INSCRIPTION D'UN ÉLECTEUR ---");

        System.out.print("Entrez l'identifiant électeur : ");
        String electeurId = scanner.nextLine();

        System.out.print("Entrez le login : ");
        String login = scanner.nextLine();

        System.out.print("Entrez le mot de passe : ");
        String mdpClair = scanner.nextLine();

        System.out.print("Entrez le nom : ");
        String nom = scanner.nextLine().trim();

        System.out.print("Entrez le prénom : ");
        String prenom = scanner.nextLine().trim();

        return new InfosInscription(electeurId, login, mdpClair, nom, prenom);
    }

    public static String demanderIdSuppression() {
        System.out.println("\n--- SUPPRESSION D'UN ÉLECTEUR ---");
        System.out.print("Entrez l'identifiant de l'électeur à supprimer : ");
        return scanner.nextLine().trim();
    }

    public static class InfosModification {
        private final String electeurId;
        private final String login;
        private final String nom;
        private final String prenom;

        public InfosModification(String electeurId, String login, String nom, String prenom) {
            this.electeurId = electeurId;
            this.login = login;
            this.nom = nom;
            this.prenom = prenom;
        }

        public String getElecteurId() { return electeurId; }
        public String getLogin() { return login; }
        public String getNom() { return nom; }
        public String getPrenom() { return prenom; }
    }

    public static class InfosVote {
        private final String electeurId;
        private final String referendumId;
        private final int vote;

        public InfosVote(String electeurId, String referendumId, int vote) {
            this.electeurId = electeurId;
            this.referendumId = referendumId;
            this.vote = vote;
        }

        public String getElecteurId() { return electeurId; }
        public String getReferendumId() { return referendumId; }
        public int getVote() { return vote; }
    }

    public static InfosVote demanderInformationsVote() {
        System.out.println("\n--- VOTER ----");

        System.out.print("Votre identifiant électeur : ");
        String electeurId = scanner.nextLine().trim();

        System.out.print("Identifiant du référendum : ");
        String referendumId = scanner.nextLine().trim();

        System.out.print("Votre vote (1 = oui, 0 = non) : ");

        int vote;
        try {
            vote = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            // On ne les compte pas
            vote = -1;
        }
        return new InfosVote(electeurId, referendumId, vote);
    }

    public static InfosModification demanderInformationsModification() {
        System.out.println("\n--- MODIFICATION D'UN ÉLECTEUR ---");

        System.out.print("Identifiant de l'électeur à modifier : ");
        String electeurId = scanner.nextLine().trim();

        System.out.print("Nouveau login : ");
        String login = scanner.nextLine().trim();

        System.out.print("Nouveau nom : ");
        String nom = scanner.nextLine().trim();

        System.out.print("Nouveau prénom : ");
        String prenom = scanner.nextLine().trim();

        return new InfosModification(electeurId, login, nom, prenom);
    }

    public static boolean demanderConfirmationSuppression(String electeurId) {
        System.out.println("Vous allez supprimer l'électeur " + electeurId + ".");
        System.out.println("(S'il a déjà voté, son compte sera seulement désactivé.)");
        System.out.print("Confirmer ? (o/N) : ");

        String reponse = scanner.nextLine().trim().toLowerCase();
        return reponse.equals("o") || reponse.equals("oui");
    }




}
