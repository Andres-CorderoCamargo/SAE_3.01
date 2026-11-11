package systeme.persistance;

import org.mindrot.jbcrypt.BCrypt;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * Hachage et verification des mots de passe, avec la methode vue au TD8 de
 * R3.01 (poivrer, saler et hacher), que le client nous demande de reprendre :
 *
 *   1. poivre : HMAC-SHA256 du mot de passe, avec comme cle un secret propre
 *      au site (le poivre), qui n'est jamais range en base ;
 *   2. sel + hachage : bcrypt (cout 12) du resultat. Le sel est tire au
 *      hasard et fait partie de l'empreinte.
 *
 * C'est l'equivalent Java des deux lignes PHP du TD8 :
 *
 *   $mdpPoivre = hash_hmac("sha256", $mdpClair, $poivre);
 *   $mdpHache  = password_hash($mdpPoivre, PASSWORD_DEFAULT);
 *
 * Le poivre est un secret : il ne doit JAMAIS etre ecrit dans le code ni
 * pousse sur GitHub. Comme pour la base de donnees, on le lit dans une
 * variable d'environnement. Pour en generer un (une seule fois) :
 *
 *   openssl rand -base64 18
 *   export SAE_POIVRE="le_resultat_de_la_commande"
 *
 * Changer le poivre rend toutes les empreintes deja enregistrees illisibles,
 * sauf si on garde l'ancien pendant une periode de transition (variable
 * SAE_POIVRE_ANCIEN) : voir verifierAvecRotation(). On ne peut pas re-hacher
 * une empreinte sans le mot de passe en clair, donc chaque compte est mis a
 * jour au moment ou son proprietaire se connecte.
 *
 * Tout se passe cote SERVEUR : le client envoie le mot de passe saisi et le
 * serveur le hache. Hacher cote client ne protegerait rien, car l'empreinte
 * deviendrait elle-meme le mot de passe (cf. TD8, "Securite avancee").
 *
 * Cette classe est la seule du projet qui connait bcrypt et le poivre.
 */
public class MotDePasse {

    /** Nom de la variable d'environnement qui contient le poivre. */
    public static final String VARIABLE_POIVRE = "SAE_POIVRE";

    /**
     * Variable facultative : l'ancien poivre, a definir seulement pendant un
     * changement de poivre (voir verifierAvecRotation).
     */
    public static final String VARIABLE_ANCIEN_POIVRE = "SAE_POIVRE_ANCIEN";

    /** Resultat d'une verification qui tient compte d'un changement de poivre. */
    public enum Verification {
        /** Bon mot de passe, empreinte faite avec le poivre actuel. */
        VALIDE,
        /** Bon mot de passe, mais empreinte faite avec l'ancien poivre : a re-hacher. */
        VALIDE_A_REHACHER,
        /** Mauvais mot de passe (ou empreinte illisible avec les poivres connus). */
        INVALIDE
    }

    /** Cout bcrypt : 2^12 tours. Une empreinte fait toujours 60 caracteres. */
    public static final int COUT = 12;

    private static final String ALGO_HMAC = "HmacSHA256";
    private static final char[] HEXA = "0123456789abcdef".toCharArray();

    private MotDePasse() {
    }

    /**
     * Hache un mot de passe avant de le ranger en base, avec le poivre de la
     * variable d'environnement SAE_POIVRE.
     * bcrypt tire un sel au hasard, donc deux appels sur le meme mot de
     * passe donnent deux empreintes differentes. C'est normal.
     *
     * @throws IllegalArgumentException si le mot de passe est null ou vide
     * @throws IllegalStateException    si SAE_POIVRE n'est pas definie
     */
    public static String hacher(String mdpClair) {
        return hacher(mdpClair, lirePoivre());
    }

    /** Comme hacher(String), avec un poivre donne (utile pour les tests). */
    public static String hacher(String mdpClair, String poivre) {
        // Un mot de passe vide ou null ne doit jamais devenir un compte
        // utilisable (trouve par Zakaria).
        if (mdpClair == null || mdpClair.isEmpty()) {
            throw new IllegalArgumentException("Mot de passe vide");
        }
        return BCrypt.hashpw(poivrer(mdpClair, poivre), BCrypt.gensalt(COUT));
    }

    /**
     * Verifie si un mot de passe saisi correspond a l'empreinte enregistree,
     * avec le poivre de la variable d'environnement SAE_POIVRE.
     *
     * @throws IllegalStateException si SAE_POIVRE n'est pas definie : c'est
     *         une erreur de configuration du serveur, pas un mauvais mot de
     *         passe, donc on ne repond pas simplement "false".
     */
    public static boolean verifier(String mdpSaisi, String hashEnBDD) {
        return verifier(mdpSaisi, hashEnBDD, lirePoivre());
    }

    /** Comme verifier(String, String), avec un poivre donne (utile pour les tests). */
    public static boolean verifier(String mdpSaisi, String hashEnBDD, String poivre) {
        exigerPoivre(poivre);
        if (mdpSaisi == null || hashEnBDD == null || !hashEnBDD.startsWith("$2")) {
            return false;
        }
        try {
            return BCrypt.checkpw(poivrer(mdpSaisi, poivre), hashEnBDD);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            // empreinte tronquee ou abimee : on refuse au lieu de planter
            return false;
        }
    }

    /**
     * Verification utilisee a la connexion, qui gere le changement de poivre.
     * Poivre actuel : SAE_POIVRE (obligatoire). Ancien poivre :
     * SAE_POIVRE_ANCIEN (facultatif, seulement pendant la transition).
     *
     * @throws IllegalStateException si SAE_POIVRE n'est pas definie
     */
    public static Verification verifierAvecRotation(String mdpSaisi, String hashEnBDD) {
        return verifierAvecRotation(mdpSaisi, hashEnBDD,
                lirePoivre(), System.getenv(VARIABLE_ANCIEN_POIVRE));
    }

    /**
     * Comme verifierAvecRotation(String, String), avec les poivres donnes
     * (utile pour les tests).
     *
     * 1. On essaie le poivre actuel : si ca marche, VALIDE.
     * 2. Sinon, s'il y a un ancien poivre, on l'essaie : si ca marche, le mot
     *    de passe est bon mais l'empreinte est a refaire avec le poivre
     *    actuel (VALIDE_A_REHACHER). C'est le seul moment ou on peut le
     *    faire, car on a le mot de passe en clair.
     * 3. Sinon, INVALIDE.
     *
     * @param ancienPoivre null ou vide s'il n'y a pas de changement en cours
     * @throws IllegalStateException si le poivre actuel est absent
     */
    public static Verification verifierAvecRotation(String mdpSaisi, String hashEnBDD,
                                                    String poivre, String ancienPoivre) {
        exigerPoivre(poivre);
        if (verifier(mdpSaisi, hashEnBDD, poivre)) {
            return Verification.VALIDE;
        }
        boolean ancienDefini = ancienPoivre != null && !ancienPoivre.isEmpty()
                && !ancienPoivre.equals(poivre);
        if (ancienDefini && verifier(mdpSaisi, hashEnBDD, ancienPoivre)) {
            return Verification.VALIDE_A_REHACHER;
        }
        return Verification.INVALIDE;
    }

    /**
     * Etape 1 de la methode : HMAC-SHA256 du mot de passe avec le poivre
     * comme cle, ecrit en hexadecimal minuscule (64 caracteres), comme le
     * fait hash_hmac("sha256", ...) en PHP.
     *
     * Effet de bord utile : bcrypt ne lit que les 72 premiers octets de son
     * entree, mais ici l'entree fait toujours 64 caracteres. Un mot de
     * passe long est donc pris en compte en entier.
     *
     * Public pour pouvoir etre teste.
     */
    public static String poivrer(String mdpClair, String poivre) {
        exigerPoivre(poivre);
        try {
            Mac mac = Mac.getInstance(ALGO_HMAC);
            mac.init(new SecretKeySpec(poivre.getBytes(StandardCharsets.UTF_8), ALGO_HMAC));
            byte[] empreinte = mac.doFinal(mdpClair.getBytes(StandardCharsets.UTF_8));
            return enHexa(empreinte);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 indisponible", e);
        }
    }

    private static String lirePoivre() {
        return System.getenv(VARIABLE_POIVRE);
    }

    private static void exigerPoivre(String poivre) {
        if (poivre == null || poivre.isEmpty()) {
            throw new IllegalStateException(
                "Poivre non defini : exporter la variable d'environnement " + VARIABLE_POIVRE);
        }
    }

    private static String enHexa(byte[] octets) {
        char[] texte = new char[octets.length * 2];
        for (int i = 0; i < octets.length; i++) {
            int v = octets[i] & 0xFF;
            texte[2 * i] = HEXA[v >>> 4];
            texte[2 * i + 1] = HEXA[v & 0x0F];
        }
        return new String(texte);
    }
}
