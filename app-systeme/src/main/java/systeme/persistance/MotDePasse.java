package systeme.persistance;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Hachage et verification des mots de passe.
 *
 * Code ecrit par Zakaria dans ElecteurService, deplace ici parce que ces
 * deux operations se font cote serveur : BCrypt.checkpw a besoin du mot de
 * passe en clair, donc le client ne peut pas hacher avant d'envoyer.
 *
 * C'est la seule classe du projet qui connait BCrypt.
 */
public class MotDePasse {

    private MotDePasse() {
    }

    /**
     * Hache un mot de passe avant de le ranger en base.
     * BCrypt tire un sel au hasard, donc deux appels sur le meme mot de
     * passe donnent deux empreintes differentes. C'est normal.
     */
    public static String hacher(String mdpClair) {
        // BCrypt hacherait le texte "null" sans rien dire, et le compte
        // s'ouvrirait avec "null" comme mot de passe (trouve par Zakaria).
        if (mdpClair == null || mdpClair.isEmpty()) {
            throw new IllegalArgumentException("Mot de passe vide");
        }
        return BCrypt.hashpw(mdpClair, BCrypt.gensalt(12));
    }

    /**
     * Verifie si un mot de passe saisi correspond a l'empreinte enregistree.
     */
    public static boolean verifier(String mdpSaisi, String hashEnBDD) {
        // mdpSaisi == null : jbcrypt le transformerait en texte "null"
        if (mdpSaisi == null || hashEnBDD == null || !hashEnBDD.startsWith("$2")) {
            return false;
        }
        try {
            return BCrypt.checkpw(mdpSaisi, hashEnBDD);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            // empreinte tronquee ou abimee : on refuse au lieu de planter
            return false;
        }
    }
}
