package ma.emsi.coaching.dao;

import ma.emsi.coaching.model.Seance;
import ma.emsi.coaching.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SeanceDao implements Dao<Seance> {

    @Override
    public Seance ajouter(Seance s) {
        String sql = "INSERT INTO seance (sportif_id, programme_id, date_seance, duree_min, ressenti, commentaire) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getSportifId());
            JdbcUtils.setInteger(ps, 2, s.getProgrammeId());
            JdbcUtils.setDate(ps, 3, s.getDateSeance());
            ps.setInt(4, s.getDureeMin());
            ps.setInt(5, s.getRessenti());
            ps.setString(6, s.getCommentaire());
            ps.executeUpdate();
            s.setId(JdbcUtils.cleGeneree(ps));
            return s;
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de l'enregistrement de la séance", e);
        }
    }

    @Override
    public void modifier(Seance s) {
        String sql = "UPDATE seance SET date_seance = ?, duree_min = ?, ressenti = ?, commentaire = ? WHERE id = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            JdbcUtils.setDate(ps, 1, s.getDateSeance());
            ps.setInt(2, s.getDureeMin());
            ps.setInt(3, s.getRessenti());
            ps.setString(4, s.getCommentaire());
            ps.setInt(5, s.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la modification de la séance " + s.getId(), e);
        }
    }

    @Override
    public void supprimer(int id) {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM seance WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la suppression de la séance " + id, e);
        }
    }

    @Override
    public Optional<Seance> trouverParId(int id) {
        List<Seance> l = lister("SELECT * FROM seance WHERE id = ?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    @Override
    public List<Seance> trouverTous() {
        return lister("SELECT * FROM seance ORDER BY date_seance DESC");
    }

    public List<Seance> trouverParSportif(int sportifId) {
        return lister("SELECT * FROM seance WHERE sportif_id = ? ORDER BY date_seance DESC", sportifId);
    }

    public List<Seance> trouverParSportifEntre(int sportifId, LocalDate debut, LocalDate fin) {
        return lister("SELECT * FROM seance WHERE sportif_id = ? AND date_seance BETWEEN ? AND ? "
                + "ORDER BY date_seance", sportifId, java.sql.Date.valueOf(debut), java.sql.Date.valueOf(fin));
    }

    private List<Seance> lister(String sql, Object... params) {
        List<Seance> liste = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Seance s = new Seance();
                    s.setId(rs.getInt("id"));
                    s.setSportifId(rs.getInt("sportif_id"));
                    s.setProgrammeId(JdbcUtils.getInteger(rs, "programme_id"));
                    s.setDateSeance(JdbcUtils.getDate(rs, "date_seance"));
                    s.setDureeMin(rs.getInt("duree_min"));
                    s.setRessenti(rs.getInt("ressenti"));
                    s.setCommentaire(rs.getString("commentaire"));
                    liste.add(s);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la lecture des séances", e);
        }
        return liste;
    }
}
