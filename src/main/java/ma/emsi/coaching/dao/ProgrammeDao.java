package ma.emsi.coaching.dao;

import ma.emsi.coaching.model.NiveauProgramme;
import ma.emsi.coaching.model.Objectif;
import ma.emsi.coaching.model.Programme;
import ma.emsi.coaching.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProgrammeDao implements Dao<Programme> {

    @Override
    public Programme ajouter(Programme p) {
        String sql = "INSERT INTO programme (nom, description, objectif, niveau, coach_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNom());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getObjectif().name());
            ps.setString(4, p.getNiveau().name());
            ps.setInt(5, p.getCoachId());
            ps.executeUpdate();
            p.setId(JdbcUtils.cleGeneree(ps));
            return p;
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de l'ajout du programme " + p.getNom(), e);
        }
    }

    @Override
    public void modifier(Programme p) {
        String sql = "UPDATE programme SET nom = ?, description = ?, objectif = ?, niveau = ? WHERE id = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, p.getNom());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getObjectif().name());
            ps.setString(4, p.getNiveau().name());
            ps.setInt(5, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la modification du programme " + p.getId(), e);
        }
    }

    @Override
    public void supprimer(int id) {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM programme WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Impossible de supprimer ce programme : il est affecté à des sportifs.", e);
        }
    }

    @Override
    public Optional<Programme> trouverParId(int id) {
        List<Programme> l = lister("SELECT * FROM programme WHERE id = ?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    @Override
    public List<Programme> trouverTous() {
        return lister("SELECT * FROM programme ORDER BY nom");
    }

    public List<Programme> trouverParCoach(int coachId) {
        return lister("SELECT * FROM programme WHERE coach_id = ? ORDER BY nom", coachId);
    }

    private List<Programme> lister(String sql, Object... params) {
        List<Programme> liste = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapper(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la lecture des programmes", e);
        }
        return liste;
    }

    static Programme mapper(ResultSet rs) throws SQLException {
        Programme p = new Programme();
        p.setId(rs.getInt("id"));
        p.setNom(rs.getString("nom"));
        p.setDescription(rs.getString("description"));
        p.setObjectif(Objectif.valueOf(rs.getString("objectif")));
        p.setNiveau(NiveauProgramme.valueOf(rs.getString("niveau")));
        p.setCoachId(rs.getInt("coach_id"));
        Timestamp ts = rs.getTimestamp("date_creation");
        p.setDateCreation(ts == null ? null : ts.toLocalDateTime());
        return p;
    }
}
