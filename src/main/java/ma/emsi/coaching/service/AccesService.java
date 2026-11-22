package ma.emsi.coaching.service;

import ma.emsi.coaching.dao.ProfilSportifDao;
import ma.emsi.coaching.model.ProfilSportif;
import ma.emsi.coaching.model.Utilisateur;

import java.util.Objects;

/**
 * Règle de cloisonnement : qui peut consulter les données d'un sportif ?
 * - l'admin : tout le monde
 * - un coach : uniquement les sportifs qui lui sont assignés
 * - un sportif : uniquement lui-même
 */
public class AccesService {

    private final ProfilSportifDao profilDao = new ProfilSportifDao();

    public void verifierAccesSportif(int sportifId) {
        Utilisateur u = Session.getUtilisateur();
        switch (u.getRole()) {
            case ADMIN -> {
            }
            case SPORTIF -> {
                if (u.getId() != sportifId) {
                    throw new AccesRefuseException("Vous ne pouvez consulter que vos propres données.");
                }
            }
            case COACH -> {
                Integer coachId = profilDao.trouverParSportif(sportifId)
                        .map(ProfilSportif::getCoachId).orElse(null);
                if (!Objects.equals(coachId, u.getId())) {
                    throw new AccesRefuseException("Ce sportif n'est pas suivi par vous.");
                }
            }
        }
    }
}
