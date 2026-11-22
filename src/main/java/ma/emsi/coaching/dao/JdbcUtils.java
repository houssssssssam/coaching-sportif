package ma.emsi.coaching.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;

/**
 * Petites méthodes utilitaires pour gérer les valeurs NULL et les dates.
 */
final class JdbcUtils {

    private JdbcUtils() {
    }

    static Integer getInteger(ResultSet rs, String colonne) throws SQLException {
        int v = rs.getInt(colonne);
        return rs.wasNull() ? null : v;
    }

    static Double getDouble(ResultSet rs, String colonne) throws SQLException {
        double v = rs.getDouble(colonne);
        return rs.wasNull() ? null : v;
    }

    static LocalDate getDate(ResultSet rs, String colonne) throws SQLException {
        Date d = rs.getDate(colonne);
        return d == null ? null : d.toLocalDate();
    }

    static void setInteger(PreparedStatement ps, int index, Integer valeur) throws SQLException {
        if (valeur == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, valeur);
        }
    }

    static void setDouble(PreparedStatement ps, int index, Double valeur) throws SQLException {
        if (valeur == null) {
            ps.setNull(index, Types.DECIMAL);
        } else {
            ps.setDouble(index, valeur);
        }
    }

    static void setDate(PreparedStatement ps, int index, LocalDate valeur) throws SQLException {
        if (valeur == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setDate(index, Date.valueOf(valeur));
        }
    }

    /** Récupère la clé générée par un INSERT exécuté avec RETURN_GENERATED_KEYS. */
    static int cleGeneree(Statement st) throws SQLException {
        try (ResultSet rs = st.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("Aucune clé générée");
        }
    }
}
