package ma.emsi.coaching.dao;

import ma.emsi.coaching.model.ProfilSportif;
import ma.emsi.coaching.model.Role;
import ma.emsi.coaching.model.Utilisateur;
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

public class UtilisateurDao implements Dao<Utilisateur> {

    private static final String COLONNES =
            "id, nom, prenom, email, mot_de_passe_hash, role, actif, date_creation";

    @Override
    public Utilisateur ajouter(Utilisateur u) {
        try (Connection cn = DatabaseConnection.getConnection()) {
            inserer(cn, u);
            return u;
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de l'ajout de l'utilisateur " + u.getEmail(), e);
        }
    }

    /**
     * Crée un sportif et son profil dans une même transaction :
     * soit les deux lignes sont créées, soit aucune.
     */
    public Utilisateur ajouterSportif(Utilisateur u, ProfilSportif profil) {
        try (Connection cn = DatabaseConnection.getConnection()) {
            cn.setAutoCommit(false);
            try {
                inserer(cn, u);
                profil.setUtilisateurId(u.getId());
                ProfilSportifDao.inserer(cn, profil);
                cn.commit();
                return u;
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la création du sportif " + u.getEmail(), e);
        }
    }

    private void inserer(Connection cn, Utilisateur u) throws SQLException {
        String sql = "INSERT INTO utilisateur (nom, prenom, email, mot_de_passe_hash, role, actif) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getMotDePasseHash());
            ps.setString(5, u.getRole().name());
            ps.setBoolean(6, u.isActif());
            ps.executeUpdate();
            u.setId(JdbcUtils.cleGeneree(ps));
        }
    }

    @Override
    public void modifier(Utilisateur u) {
        String sql = "UPDATE utilisateur SET nom = ?, prenom = ?, email = ?, role = ?, actif = ? WHERE id = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getRole().name());
            ps.setBoolean(5, u.isActif());
            ps.setInt(6, u.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la modification de l'utilisateur " + u.getId(), e);
        }
    }

    public void modifierMotDePasse(int id, String nouveauHash) {
        String sql = "UPDATE utilisateur SET mot_de_passe_hash = ? WHERE id = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nouveauHash);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Erreur lors du changement de mot de passe", e);
        }
    }

    /** Active ou désactive un compte (on préfère désactiver plutôt que supprimer). */
    public void changerStatut(int id, boolean actif) {
        String sql = "UPDATE utilisateur SET actif = ? WHERE id = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setBoolean(1, actif);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Erreur lors du changement de statut de l'utilisateur " + id, e);
        }
    }

    @Override
    public void supprimer(int id) {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM utilisateur WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Impossible de supprimer l'utilisateur " + id
                    + " : il est probablement lié à des programmes ou affectations. Désactivez-le plutôt.", e);
        }
    }

    @Override
    public Optional<Utilisateur> trouverParId(int id) {
        return trouverUn("SELECT " + COLONNES + " FROM utilisateur WHERE id = ?", id);
    }

    public Optional<Utilisateur> trouverParEmail(String email) {
        return trouverUn("SELECT " + COLONNES + " FROM utilisateur WHERE email = ?", email);
    }

    @Override
    public List<Utilisateur> trouverTous() {
        return trouverListe("SELECT " + COLONNES + " FROM utilisateur ORDER BY role, nom, prenom");
    }

    public List<Utilisateur> trouverParRole(Role role) {
        return trouverListe("SELECT " + COLONNES + " FROM utilisateur WHERE role = ? ORDER BY nom, prenom",
                role.name());
    }

    /** Sportifs suivis par un coach donné. */
    public List<Utilisateur> trouverSportifsDuCoach(int coachId) {
        return trouverListe("SELECT u.id, u.nom, u.prenom, u.email, u.mot_de_passe_hash, u.role, u.actif, "
                + "u.date_creation FROM utilisateur u JOIN profil_sportif p ON p.utilisateur_id = u.id "
                + "WHERE p.coach_id = ? ORDER BY u.nom, u.prenom", coachId);
    }

    /** Sportifs inscrits qui n'ont pas encore de coach (inscription autonome). */
    public List<Utilisateur> trouverSportifsSansCoach() {
        return trouverListe("SELECT u.id, u.nom, u.prenom, u.email, u.mot_de_passe_hash, u.role, u.actif, "
                + "u.date_creation FROM utilisateur u JOIN profil_sportif p ON p.utilisateur_id = u.id "
                + "WHERE p.coach_id IS NULL ORDER BY u.date_creation DESC");
    }

    public boolean emailExiste(String email, int idExclu) {
        String sql = "SELECT COUNT(*) FROM utilisateur WHERE email = ? AND id <> ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, idExclu);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la vérification de l'email", e);
        }
    }

    private Optional<Utilisateur> trouverUn(String sql, Object param) {
        List<Utilisateur> liste = trouverListe(sql, param);
        return liste.isEmpty() ? Optional.empty() : Optional.of(liste.get(0));
    }

    private List<Utilisateur> trouverListe(String sql, Object... params) {
        List<Utilisateur> liste = new ArrayList<>();
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
            throw new DaoException("Erreur lors de la lecture des utilisateurs", e);
        }
        return liste;
    }

    private Utilisateur mapper(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setMotDePasseHash(rs.getString("mot_de_passe_hash"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setActif(rs.getBoolean("actif"));
        Timestamp ts = rs.getTimestamp("date_creation");
        u.setDateCreation(ts == null ? null : ts.toLocalDateTime());
        return u;
    }
}
