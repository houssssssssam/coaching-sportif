package ma.emsi.coaching.dao;

import ma.emsi.coaching.model.NiveauActivite;
import ma.emsi.coaching.model.Objectif;
import ma.emsi.coaching.model.ProfilSportif;
import ma.emsi.coaching.model.Sexe;
import ma.emsi.coaching.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class ProfilSportifDao {

    static void inserer(Connection cn, ProfilSportif p) throws SQLException {
        String sql = "INSERT INTO profil_sportif (utilisateur_id, coach_id, sexe, date_naissance, taille_cm, "
                + "objectif, niveau_activite) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, p.getUtilisateurId());
            JdbcUtils.setInteger(ps, 2, p.getCoachId());
            ps.setString(3, p.getSexe().name());
            JdbcUtils.setDate(ps, 4, p.getDateNaissance());
            ps.setDouble(5, p.getTailleCm());
            ps.setString(6, p.getObjectif().name());
            ps.setString(7, p.getNiveauActivite().name());
            ps.executeUpdate();
        }
    }

    public void modifier(ProfilSportif p) {
        String sql = "UPDATE profil_sportif SET coach_id = ?, sexe = ?, date_naissance = ?, taille_cm = ?, "
                + "objectif = ?, niveau_activite = ? WHERE utilisateur_id = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            JdbcUtils.setInteger(ps, 1, p.getCoachId());
            ps.setString(2, p.getSexe().name());
            JdbcUtils.setDate(ps, 3, p.getDateNaissance());
            ps.setDouble(4, p.getTailleCm());
            ps.setString(5, p.getObjectif().name());
            ps.setString(6, p.getNiveauActivite().name());
            ps.setInt(7, p.getUtilisateurId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la modification du profil " + p.getUtilisateurId(), e);
        }
    }

    public Optional<ProfilSportif> trouverParSportif(int sportifId) {
        String sql = "SELECT * FROM profil_sportif WHERE utilisateur_id = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, sportifId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapper(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Erreur lors de la lecture du profil " + sportifId, e);
        }
    }

    private ProfilSportif mapper(ResultSet rs) throws SQLException {
        ProfilSportif p = new ProfilSportif();
        p.setUtilisateurId(rs.getInt("utilisateur_id"));
        p.setCoachId(JdbcUtils.getInteger(rs, "coach_id"));
        p.setSexe(Sexe.valueOf(rs.getString("sexe")));
        p.setDateNaissance(JdbcUtils.getDate(rs, "date_naissance"));
        p.setTailleCm(rs.getDouble("taille_cm"));
        p.setObjectif(Objectif.valueOf(rs.getString("objectif")));
        p.setNiveauActivite(NiveauActivite.valueOf(rs.getString("niveau_activite")));
        return p;
    }
}
