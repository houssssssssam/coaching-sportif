package ma.emsi.coaching.service;

import ma.emsi.coaching.dao.AffectationDao;
import ma.emsi.coaching.dao.ProfilSportifDao;
import ma.emsi.coaching.dao.ProgrammeDao;
import ma.emsi.coaching.dao.ProgrammeExerciceDao;
import ma.emsi.coaching.dao.UtilisateurDao;
import ma.emsi.coaching.model.Affectation;
import ma.emsi.coaching.model.Exercice;
import ma.emsi.coaching.model.ProfilSportif;
import ma.emsi.coaching.model.Programme;
import ma.emsi.coaching.model.ProgrammeExercice;
import ma.emsi.coaching.model.Role;
import ma.emsi.coaching.model.TypeExercice;
import ma.emsi.coaching.model.Utilisateur;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Programmes d'entraînement : création par le coach, contenu, affectation aux sportifs.
 */
public class ProgrammeService {

    private final ProgrammeDao programmeDao = new ProgrammeDao();
    private final ProgrammeExerciceDao ligneDao = new ProgrammeExerciceDao();
    private final AffectationDao affectationDao = new AffectationDao();
    private final UtilisateurDao utilisateurDao = new UtilisateurDao();
    private final ProfilSportifDao profilDao = new ProfilSportifDao();
    private final AccesService acces = new AccesService();

    // ---------------- Programmes ----------------

    /** Le coach voit ses programmes, l'admin les voit tous. */
    public List<Programme> mesProgrammes() {
        Utilisateur u = Session.exiger(Role.COACH, Role.ADMIN);
        return u.getRole() == Role.ADMIN ? programmeDao.trouverTous() : programmeDao.trouverParCoach(u.getId());
    }

    public Programme creer(Programme p) {
        Utilisateur coach = Session.exiger(Role.COACH);
        p.setCoachId(coach.getId());
        valider(p);
        return programmeDao.ajouter(p);
    }

    public void modifier(Programme p) {
        verifierProprietaire(p.getId());
        valider(p);
        programmeDao.modifier(p);
    }

    public void supprimer(int programmeId) {
        verifierProprietaire(programmeId);
        programmeDao.supprimer(programmeId);
    }

    private void valider(Programme p) {
        if (p.getNom() == null || p.getNom().trim().length() < 3) {
            throw new ValidationException("Le nom du programme doit contenir au moins 3 caractères.");
        }
        if (p.getObjectif() == null || p.getNiveau() == null) {
            throw new ValidationException("Choisissez un objectif et un niveau.");
        }
        p.setNom(p.getNom().trim());
    }

    private Programme verifierProprietaire(int programmeId) {
        Utilisateur u = Session.exiger(Role.COACH, Role.ADMIN);
        Programme p = programmeDao.trouverParId(programmeId)
                .orElseThrow(() -> new ValidationException("Programme introuvable."));
        if (u.getRole() == Role.COACH && p.getCoachId() != u.getId()) {
            throw new AccesRefuseException("Ce programme appartient à un autre coach.");
        }
        return p;
    }

    // ---------------- Contenu d'un programme ----------------

    public List<ProgrammeExercice> contenu(int programmeId) {
        Utilisateur u = Session.getUtilisateur();
        if (u.getRole() == Role.SPORTIF) {
            boolean affecte = affectationDao.trouverParSportif(u.getId()).stream()
                    .anyMatch(a -> a.getProgramme().getId() == programmeId);
            if (!affecte) {
                throw new AccesRefuseException("Ce programme ne vous a pas été affecté.");
            }
        } else {
            verifierProprietaire(programmeId);
        }
        return ligneDao.trouverParProgramme(programmeId);
    }

    public ProgrammeExercice ajouterLigne(ProgrammeExercice pe) {
        verifierProprietaire(pe.getProgrammeId());
        validerLigne(pe);
        return ligneDao.ajouter(pe);
    }

    public void modifierLigne(ProgrammeExercice pe) {
        verifierProprietaire(pe.getProgrammeId());
        validerLigne(pe);
        ligneDao.modifier(pe);
    }

    public void supprimerLigne(int programmeId, int ligneId) {
        verifierProprietaire(programmeId);
        ligneDao.supprimer(ligneId);
    }

    /**
     * Une ligne de musculation demande des séries et des répétitions (ou une consigne de durée,
     * ex. gainage) ; une ligne de cardio ou de mobilité demande une durée.
     */
    private void validerLigne(ProgrammeExercice pe) {
        Exercice e = pe.getExercice();
        if (e == null) {
            throw new ValidationException("Choisissez un exercice.");
        }
        if (pe.getJour() < 1 || pe.getJour() > 7) {
            throw new ValidationException("Le jour doit être compris entre 1 et 7.");
        }
        if (e.getType() == TypeExercice.FORCE) {
            if (pe.getSeries() == null || pe.getSeries() < 1 || pe.getSeries() > 10) {
                throw new ValidationException("Un exercice de force demande entre 1 et 10 séries.");
            }
            boolean repsOk = pe.getRepetitions() != null && pe.getRepetitions() >= 1 && pe.getRepetitions() <= 100;
            boolean consigne = pe.getConsigne() != null && !pe.getConsigne().isBlank();
            if (!repsOk && !consigne) {
                throw new ValidationException("Indiquez les répétitions (1 à 100) ou une consigne (ex. tenir 30 s).");
            }
            if (pe.getChargeKg() != null && (pe.getChargeKg() < 0 || pe.getChargeKg() > 500)) {
                throw new ValidationException("La charge doit être comprise entre 0 et 500 kg.");
            }
        } else if (pe.getDureeMin() == null || pe.getDureeMin() < 1 || pe.getDureeMin() > 180) {
            throw new ValidationException("Un exercice de " + e.getType().getLibelle().toLowerCase()
                    + " demande une durée entre 1 et 180 minutes.");
        }
    }

    // ---------------- Affectations ----------------

    /** Sportifs suivis par le coach connecté (tous les sportifs pour l'admin). */
    public List<Utilisateur> sportifsSuivis() {
        Utilisateur u = Session.exiger(Role.COACH, Role.ADMIN);
        return u.getRole() == Role.ADMIN ? utilisateurDao.trouverParRole(Role.SPORTIF)
                : utilisateurDao.trouverSportifsDuCoach(u.getId());
    }

    /**
     * Affecte un programme à un sportif suivi par le coach.
     * L'ancien programme actif est automatiquement clôturé (un seul programme actif).
     */
    public Affectation affecter(int sportifId, int programmeId, LocalDate dateDebut) {
        Session.exiger(Role.COACH);
        acces.verifierAccesSportif(sportifId);
        verifierProprietaire(programmeId);
        if (dateDebut == null) {
            throw new ValidationException("Choisissez une date de début.");
        }
        if (ligneDao.trouverParProgramme(programmeId).isEmpty()) {
            throw new ValidationException("Ce programme est vide : ajoutez des exercices avant de l'affecter.");
        }
        Optional<Affectation> actuelle = affectationDao.trouverActive(sportifId);
        if (actuelle.isPresent() && !dateDebut.isAfter(actuelle.get().getDateDebut())) {
            throw new ValidationException("La date de début doit être postérieure au début du programme actuel ("
                    + actuelle.get().getDateDebut() + ").");
        }
        return affectationDao.affecter(sportifId, programmeId, dateDebut);
    }

    public void terminerProgramme(int sportifId) {
        Session.exiger(Role.COACH);
        acces.verifierAccesSportif(sportifId);
        Affectation a = affectationDao.trouverActive(sportifId)
                .orElseThrow(() -> new ValidationException("Ce sportif n'a pas de programme actif."));
        affectationDao.terminer(a.getId(), LocalDate.now());
    }

    public Optional<Affectation> programmeActif(int sportifId) {
        acces.verifierAccesSportif(sportifId);
        return affectationDao.trouverActive(sportifId);
    }

    public List<Affectation> historique(int sportifId) {
        acces.verifierAccesSportif(sportifId);
        return affectationDao.trouverParSportif(sportifId);
    }

    /** Avertissement non bloquant : l'objectif du programme diffère de celui du sportif. */
    public boolean estCompatible(int sportifId, Programme p) {
        return profilDao.trouverParSportif(sportifId)
                .map(ProfilSportif::getObjectif)
                .map(o -> o == p.getObjectif())
                .orElse(true);
    }
}
