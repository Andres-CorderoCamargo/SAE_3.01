package systeme;

public class InfosStockageElecteur {
    private final String login;
    private final String mdpHache;
    private final String electeurId;

    /** @param mdpHache l'empreinte produite par MotDePasse.hacher(), jamais le mot de passe en clair */
    public InfosStockageElecteur(String electeurId, String login, String mdpHache) {
        this.electeurId = electeurId;
        this.login = login;
        this.mdpHache = mdpHache;
    }

    public String getLogin() { return login; }
    public String getMdpHache() { return mdpHache; }
    public String getElecteurId() { return electeurId; }

    @Override
    public String toString() {
        return "{ID=" + electeurId + ", Login=" + login + ", MdpHache=" + mdpHache + "}";
    }
}
