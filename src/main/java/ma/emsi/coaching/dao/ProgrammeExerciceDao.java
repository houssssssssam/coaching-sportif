package ma.emsi.coaching.dao;

import ma.emsi.coaching.model.ProgrammeExercice;
import ma.emsi.coaching.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Lignes d'un programme (exercice, jour, séries, répétitions...).
 */
public class ProgrammeExerciceDao {

    public ProgrammeExercice ajouter(ProgrammeExercice pe) {
        String sql = "INSERT INTO programme_exercice (programme_id, exercice_id, jour, ordre, series, repetitions, "
                + "charge_kg, duree_min, consigne) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, pe.getProgrammeId());
            remplir(ps, pe, 2);
            ps.executeUpdate();
            pe.setId(JdbcUtils.cleGeneree(ps));
            return pe;
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de l'ajout de l'exercice au programme", e);
        }
    }

    public void modifier(ProgrammeExercice pe) {
        String sql = "UPDATE programme_exercice SET exercice_id = ?, jour = ?, ordre = ?, series = ?, "
                + "repetitions = ?, charge_kg = ?, duree_min = ?, consigne = ? WHERE id = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            remplir(ps, pe, 1);
            ps.setInt(9, pe.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la modification de la ligne " + pe.getId(), e);
        }
    }

    public void supprimer(int id) {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM programme_exercice WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la suppression de la ligne " + id, e);
        }
    }

    /** Contenu complet d'un programme, trié par jour puis par ordre. */
    public List<ProgrammeExercice> trouverParProgramme(int programmeId) {
        String sql = "SELECT pe.id AS pe_id, pe.programme_id, pe.jour, pe.ordre, pe.series, pe.repetitions, "
                + "pe.charge_kg, pe.duree_min, pe.consigne, e.* "
                + "FROM programme_exercice pe JOIN exercice e ON e.id = pe.exercice_id "
                + "WHERE pe.programme_id = ? ORDER BY pe.jour, pe.ordre";
        List<ProgrammeExercice> liste = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, programmeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProgrammeExercice pe = new ProgrammeExercice();
                    pe.setId(rs.getInt("pe_id"));
                    pe.setProgrammeId(rs.getInt("programme_id"));
                    pe.setExercice(ExerciceDao.mapper(rs));
                    pe.setJour(rs.getInt("jour"));
                    pe.setOrdre(rs.getInt("ordre"));
                    pe.setSeries(JdbcUtils.getInteger(rs, "series"));
                    pe.setRepetitions(JdbcUtils.getInteger(rs, "repetitions"));
                    pe.setChargeKg(JdbcUtils.getDouble(rs, "charge_kg"));
                    pe.setDureeMin(JdbcUtils.getInteger(rs, "duree_min"));
                    pe.setConsigne(rs.getString("consigne"));
                    liste.add(pe);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la lecture du programme " + programmeId, e);
        }
        return liste;
    }

    private void remplir(PreparedStatement ps, ProgrammeExercice pe, int debut) throws SQLException {
        ps.setInt(debut, pe.getExercice().getId());
        ps.setInt(debut + 1, pe.getJour());
        ps.setInt(debut + 2, pe.getOrdre());
        JdbcUtils.setInteger(ps, debut + 3, pe.getSeries());
        JdbcUtils.setInteger(ps, debut + 4, pe.getRepetitions());
        JdbcUtils.setDouble(ps, debut + 5, pe.getChargeKg());
        JdbcUtils.setInteger(ps, debut + 6, pe.getDureeMin());
        ps.setString(debut + 7, pe.getConsigne());
    }
}
