package ma.emsi.coaching.dao;

import ma.emsi.coaching.model.Aliment;
import ma.emsi.coaching.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AlimentDao implements Dao<Aliment> {

    @Override
    public Aliment ajouter(Aliment a) {
        String sql = "INSERT INTO aliment (nom, categorie, calories_100g, proteines_100g, glucides_100g, "
                + "lipides_100g, cree_par) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            remplir(ps, a);
            JdbcUtils.setInteger(ps, 7, a.getCreePar());
            ps.executeUpdate();
            a.setId(JdbcUtils.cleGeneree(ps));
            return a;
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de l'ajout de l'aliment " + a.getNom(), e);
        }
    }

    @Override
    public void modifier(Aliment a) {
        String sql = "UPDATE aliment SET nom = ?, categorie = ?, calories_100g = ?, proteines_100g = ?, "
                + "glucides_100g = ?, lipides_100g = ? WHERE id = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            remplir(ps, a);
            ps.setInt(7, a.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la modification de l'aliment " + a.getId(), e);
        }
    }

    @Override
    public void supprimer(int id) {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM aliment WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Impossible de supprimer cet aliment : il est présent dans des repas.", e);
        }
    }

    @Override
    public Optional<Aliment> trouverParId(int id) {
        List<Aliment> l = lister("SELECT * FROM aliment WHERE id = ?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    /** Catalogue officiel uniquement (hors aliments personnalisés). */
    @Override
    public List<Aliment> trouverTous() {
        return lister("SELECT * FROM aliment WHERE cree_par IS NULL ORDER BY categorie, nom");
    }

    /**
     * Recherche façon MyFitnessPal : aliments du catalogue + aliments personnalisés
     * du sportif connecté, dont le nom contient le texte saisi.
     */
    public List<Aliment> rechercher(String texte, int sportifId) {
        return lister("SELECT * FROM aliment WHERE nom LIKE ? AND (cree_par IS NULL OR cree_par = ?) "
                + "ORDER BY cree_par IS NULL, nom LIMIT 30", "%" + texte.trim() + "%", sportifId);
    }

    public List<Aliment> trouverPersonnalises(int sportifId) {
        return lister("SELECT * FROM aliment WHERE cree_par = ? ORDER BY nom", sportifId);
    }

    private void remplir(PreparedStatement ps, Aliment a) throws SQLException {
        ps.setString(1, a.getNom());
        ps.setString(2, a.getCategorie());
        ps.setDouble(3, a.getCalories100g());
        ps.setDouble(4, a.getProteines100g());
        ps.setDouble(5, a.getGlucides100g());
        ps.setDouble(6, a.getLipides100g());
    }

    private List<Aliment> lister(String sql, Object... params) {
        List<Aliment> liste = new ArrayList<>();
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
            throw new DaoException("Erreur lors de la lecture des aliments", e);
        }
        return liste;
    }

    static Aliment mapper(ResultSet rs) throws SQLException {
        Aliment a = new Aliment();
        a.setId(rs.getInt("id"));
        a.setNom(rs.getString("nom"));
        a.setCategorie(rs.getString("categorie"));
        a.setCalories100g(rs.getDouble("calories_100g"));
        a.setProteines100g(rs.getDouble("proteines_100g"));
        a.setGlucides100g(rs.getDouble("glucides_100g"));
        a.setLipides100g(rs.getDouble("lipides_100g"));
        a.setCreePar(JdbcUtils.getInteger(rs, "cree_par"));
        return a;
    }
}
