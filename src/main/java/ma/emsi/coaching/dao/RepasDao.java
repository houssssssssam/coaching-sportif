package ma.emsi.coaching.dao;

import ma.emsi.coaching.model.Repas;
import ma.emsi.coaching.model.TypeRepas;
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

public class RepasDao implements Dao<Repas> {

    private static final String SELECT = "SELECT r.id AS r_id, r.sportif_id, r.date_repas, r.type_repas, "
            + "r.quantite_g, a.* FROM repas r JOIN aliment a ON a.id = r.aliment_id ";

    @Override
    public Repas ajouter(Repas r) {
        String sql = "INSERT INTO repas (sportif_id, date_repas, type_repas, aliment_id, quantite_g) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getSportifId());
            JdbcUtils.setDate(ps, 2, r.getDateRepas());
            ps.setString(3, r.getTypeRepas().name());
            ps.setInt(4, r.getAliment().getId());
            ps.setDouble(5, r.getQuantiteG());
            ps.executeUpdate();
            r.setId(JdbcUtils.cleGeneree(ps));
            return r;
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de l'ajout au journal alimentaire", e);
        }
    }

    @Override
    public void modifier(Repas r) {
        String sql = "UPDATE repas SET date_repas = ?, type_repas = ?, aliment_id = ?, quantite_g = ? WHERE id = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            JdbcUtils.setDate(ps, 1, r.getDateRepas());
            ps.setString(2, r.getTypeRepas().name());
            ps.setInt(3, r.getAliment().getId());
            ps.setDouble(4, r.getQuantiteG());
            ps.setInt(5, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la modification du repas " + r.getId(), e);
        }
    }

    @Override
    public void supprimer(int id) {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM repas WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la suppression du repas " + id, e);
        }
    }

    @Override
    public Optional<Repas> trouverParId(int id) {
        List<Repas> l = lister(SELECT + "WHERE r.id = ?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    @Override
    public List<Repas> trouverTous() {
        return lister(SELECT + "ORDER BY r.date_repas DESC");
    }

    /** Journal d'une journée, trié par type de repas. */
    public List<Repas> trouverParJour(int sportifId, LocalDate jour) {
        return lister(SELECT + "WHERE r.sportif_id = ? AND r.date_repas = ? ORDER BY r.type_repas, r.id",
                sportifId, Date.valueOf(jour));
    }

    public List<Repas> trouverEntre(int sportifId, LocalDate debut, LocalDate fin) {
        return lister(SELECT + "WHERE r.sportif_id = ? AND r.date_repas BETWEEN ? AND ? ORDER BY r.date_repas",
                sportifId, Date.valueOf(debut), Date.valueOf(fin));
    }

    private List<Repas> lister(String sql, Object... params) {
        List<Repas> liste = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Repas r = new Repas();
                    r.setId(rs.getInt("r_id"));
                    r.setSportifId(rs.getInt("sportif_id"));
                    r.setDateRepas(JdbcUtils.getDate(rs, "date_repas"));
                    r.setTypeRepas(TypeRepas.valueOf(rs.getString("type_repas")));
                    r.setQuantiteG(rs.getDouble("quantite_g"));
                    r.setAliment(AlimentDao.mapper(rs));
                    liste.add(r);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la lecture du journal alimentaire", e);
        }
        return liste;
    }
}
