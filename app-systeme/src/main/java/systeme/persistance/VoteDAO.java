package systeme.persistance;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

public class VoteDAO {
    private VoteDAO() {
    }

    /**
     * @return le code renvoye par la procedure : OK, REFERENDUM_INCONNU,
     *         REFERENDUM_FERME, NON_INSCRIT, DEJA_VOTE ou ERREUR
     */
    public static String enregistrer(String idReferendum, String idElecteur,
                                     byte[] bulletinU, byte[] bulletinV)
            throws SQLException {

        Connection cx = BaseDeDonnees.ouvrir();
        try {
            CallableStatement proc = cx.prepareCall("{ Call enregistrerVote(?, ?, ?, ?, ?) }");
            proc.setString(1, idReferendum);
            proc.setString(2, idElecteur);
            proc.setBytes(3, bulletinU);
            proc.setBytes(4, bulletinV);
            proc.registerOutParameter(5, Types.VARCHAR);
            proc.execute();

            String code = proc.getString(5);
            proc.close();
            return code;
        } finally {
            cx.close();
        }
    }
}
