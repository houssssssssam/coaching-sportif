package ma.emsi.coaching.dao;

import ma.emsi.coaching.model.Affectation;
import ma.emsi.coaching.model.StatutAffectation;
import ma.emsi.coaching.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AffectationDao {

    private static final String SELECT = "SELECT a.id AS a_id, a.sportif_id, a.date_debut, a.date_fin, a.statut, p.* "
            + "FROM affectation a JOIN programme p ON p.id = a.programme_id ";

    /**
     * Affecte un programme à un sportif. S'il avait déjà un programme actif,
     * celui-ci est clôturé dans la même transaction (règle : un seul programme actif).
     */
    public Affectation affecter(int sportifId, int programmeId, LocalDate dateDebut) {
        String cloture = "UPDATE affectation SET statut = 'TERMINE', date_fin = ? "
                + "WHERE sportif_id = ? AND statut = 'ACTIF'";
        String insertion = "INSERT INTO affectation (sportif_id, programme_id, date_debut, statut) "
                + "VALUES (?, ?, ?, 'ACTIF')";
        try (Connection cn = DatabaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            try (PreparedStatement ps1 = cn.prepareStatement(cloture);
                 PreparedStatement ps2 = cn.prepareStatement(insertion, Statement.RETURN_GENERATED_KEYS)) {
                ps1.setDate(1, Date.valueOf(dateDebut.minusDays(1)));
                ps1.setInt(2, sportifId);
                ps1.executeUpdate();

                ps2.setInt(1, sportifId);
                ps2.setInt(2, programmeId);
                ps2.setDate(3, Date.valueOf(dateDebut));
                ps2.executeUpdate();
                int id = JdbcUtils.cleGeneree(ps2);
                cn.commit();
                return trouverParId(id).orElseThrow();
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de l'affectation du programme", e);
        }
    }

    /** Termine le programme actif d'un sportif sans en affecter un nouveau. */
    public void terminer(int affectationId, LocalDate dateFin) {
        String sql = "UPDATE affectation SET statut = 'TERMINE', date_fin = ? WHERE id = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(dateFin));
            ps.setInt(2, affectationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la clôture de l'affectation " + affectationId, e);
        }
    }

    public Optional<Affectation> trouverParId(int id) {
        List<Affectation> l = lister(SELECT + "WHERE a.id = ?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    public Optional<Affectation> trouverActive(int sportifId) {
        List<Affectation> l = lister(SELECT + "WHERE a.sportif_id = ? AND a.statut = 'ACTIF'", sportifId);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    /** Historique des programmes d'un sportif, du plus récent au plus ancien. */
    public List<Affectation> trouverParSportif(int sportifId) {
        return lister(SELECT + "WHERE a.sportif_id = ? ORDER BY a.date_debut DESC", sportifId);
    }

    private List<Affectation> lister(String sql, Object... params) {
        List<Affectation> liste = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Affectation a = new Affectation();
                    a.setId(rs.getInt("a_id"));
                    a.setSportifId(rs.getInt("sportif_id"));
                    a.setProgramme(ProgrammeDao.mapper(rs));
                    a.setDateDebut(JdbcUtils.getDate(rs, "date_debut"));
                    a.setDateFin(JdbcUtils.getDate(rs, "date_fin"));
                    a.setStatut(StatutAffectation.valueOf(rs.getString("statut")));
                    liste.add(a);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la lecture des affectations", e);
        }
        return liste;
    }
}
