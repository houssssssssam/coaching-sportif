package ma.emsi.coaching.dao;

import ma.emsi.coaching.model.Mesure;
import ma.emsi.coaching.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MesureDao implements Dao<Mesure> {

    @Override
    public Mesure ajouter(Mesure m) {
        String sql = "INSERT INTO mesure (sportif_id, date_mesure, poids_kg, tour_taille_cm, masse_grasse_pct) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getSportifId());
            JdbcUtils.setDate(ps, 2, m.getDateMesure());
            ps.setDouble(3, m.getPoidsKg());
            JdbcUtils.setDouble(ps, 4, m.getTourTailleCm());
            JdbcUtils.setDouble(ps, 5, m.getMasseGrassePct());
            ps.executeUpdate();
            m.setId(JdbcUtils.cleGeneree(ps));
            return m;
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DaoException("Une mesure existe déjà pour cette date.", e);
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de l'enregistrement de la mesure", e);
        }
    }

    @Override
    public void modifier(Mesure m) {
        String sql = "UPDATE mesure SET date_mesure = ?, poids_kg = ?, tour_taille_cm = ?, masse_grasse_pct = ? "
                + "WHERE id = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            JdbcUtils.setDate(ps, 1, m.getDateMesure());
            ps.setDouble(2, m.getPoidsKg());
            JdbcUtils.setDouble(ps, 3, m.getTourTailleCm());
            JdbcUtils.setDouble(ps, 4, m.getMasseGrassePct());
            ps.setInt(5, m.getId());
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DaoException("Une mesure existe déjà pour cette date.", e);
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la modification de la mesure " + m.getId(), e);
        }
    }

    @Override
    public void supprimer(int id) {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM mesure WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la suppression de la mesure " + id, e);
        }
    }

    @Override
    public Optional<Mesure> trouverParId(int id) {
        List<Mesure> l = lister("SELECT * FROM mesure WHERE id = ?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    @Override
    public List<Mesure> trouverTous() {
        return lister("SELECT * FROM mesure ORDER BY date_mesure");
    }

    /** Mesures d'un sportif, de la plus ancienne à la plus récente (pratique pour les courbes). */
    public List<Mesure> trouverParSportif(int sportifId) {
        return lister("SELECT * FROM mesure WHERE sportif_id = ? ORDER BY date_mesure", sportifId);
    }

    public Optional<Mesure> trouverDerniere(int sportifId) {
        List<Mesure> l = lister("SELECT * FROM mesure WHERE sportif_id = ? ORDER BY date_mesure DESC LIMIT 1",
                sportifId);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    private List<Mesure> lister(String sql, Object... params) {
        List<Mesure> liste = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Mesure m = new Mesure();
                    m.setId(rs.getInt("id"));
                    m.setSportifId(rs.getInt("sportif_id"));
                    m.setDateMesure(JdbcUtils.getDate(rs, "date_mesure"));
                    m.setPoidsKg(rs.getDouble("poids_kg"));
                    m.setTourTailleCm(JdbcUtils.getDouble(rs, "tour_taille_cm"));
                    m.setMasseGrassePct(JdbcUtils.getDouble(rs, "masse_grasse_pct"));
                    liste.add(m);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la lecture des mesures", e);
        }
        return liste;
    }
}
