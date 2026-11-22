package ma.emsi.coaching.dao;

import ma.emsi.coaching.model.Exercice;
import ma.emsi.coaching.model.TypeExercice;
import ma.emsi.coaching.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExerciceDao implements Dao<Exercice> {

    @Override
    public Exercice ajouter(Exercice e) {
        String sql = "INSERT INTO exercice (nom, groupe_musculaire, type, description) VALUES (?, ?, ?, ?)";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            remplir(ps, e);
            ps.executeUpdate();
            e.setId(JdbcUtils.cleGeneree(ps));
            return e;
        } catch (SQLException ex) {
            throw new DaoException("Erreur lors de l'ajout de l'exercice " + e.getNom(), ex);
        }
    }

    @Override
    public void modifier(Exercice e) {
        String sql = "UPDATE exercice SET nom = ?, groupe_musculaire = ?, type = ?, description = ? WHERE id = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            remplir(ps, e);
            ps.setInt(5, e.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("Erreur lors de la modification de l'exercice " + e.getId(), ex);
        }
    }

    @Override
    public void supprimer(int id) {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM exercice WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("Impossible de supprimer cet exercice : il est utilisé dans un programme.", ex);
        }
    }

    @Override
    public Optional<Exercice> trouverParId(int id) {
        List<Exercice> l = lister("SELECT * FROM exercice WHERE id = ?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    @Override
    public List<Exercice> trouverTous() {
        return lister("SELECT * FROM exercice ORDER BY groupe_musculaire, nom");
    }

    /** Recherche par nom ou groupe musculaire (insensible à la casse). */
    public List<Exercice> rechercher(String texte) {
        String motif = "%" + texte.trim() + "%";
        return lister("SELECT * FROM exercice WHERE nom LIKE ? OR groupe_musculaire LIKE ? "
                + "ORDER BY groupe_musculaire, nom", motif, motif);
    }

    private void remplir(PreparedStatement ps, Exercice e) throws SQLException {
        ps.setString(1, e.getNom());
        ps.setString(2, e.getGroupeMusculaire());
        ps.setString(3, e.getType().name());
        ps.setString(4, e.getDescription());
    }

    private List<Exercice> lister(String sql, Object... params) {
        List<Exercice> liste = new ArrayList<>();
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
        } catch (SQLException ex) {
            throw new DaoException("Erreur lors de la lecture des exercices", ex);
        }
        return liste;
    }

    static Exercice mapper(ResultSet rs) throws SQLException {
        Exercice e = new Exercice();
        e.setId(rs.getInt("id"));
        e.setNom(rs.getString("nom"));
        e.setGroupeMusculaire(rs.getString("groupe_musculaire"));
        e.setType(TypeExercice.valueOf(rs.getString("type")));
        e.setDescription(rs.getString("description"));
        return e;
    }
}
