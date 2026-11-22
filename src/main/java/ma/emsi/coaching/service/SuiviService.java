package ma.emsi.coaching.service;

import ma.emsi.coaching.dao.AffectationDao;
import ma.emsi.coaching.dao.MesureDao;
import ma.emsi.coaching.dao.ProgrammeExerciceDao;
import ma.emsi.coaching.dao.SeanceDao;
import ma.emsi.coaching.model.Affectation;
import ma.emsi.coaching.model.Mesure;
import ma.emsi.coaching.model.ProgrammeExercice;
import ma.emsi.coaching.model.Role;
import ma.emsi.coaching.model.Seance;
import ma.emsi.coaching.model.Utilisateur;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Suivi de progression : séances, mesures corporelles et statistiques.
 */
public class SuiviService {

    /** Au-delà de cet écart de poids en moins de 7 jours, on demande une confirmation. */
    static final double VARIATION_SUSPECTE_KG = 3.0;

    private final SeanceDao seanceDao = new SeanceDao();
    private final MesureDao mesureDao = new MesureDao();
    private final AffectationDao affectationDao = new AffectationDao();
    private final ProgrammeExerciceDao ligneDao = new ProgrammeExerciceDao();
    private final AccesService acces = new AccesService();

    // ---------------- Séances ----------------

    /** Le sportif enregistre une séance ; elle est rattachée automatiquement à son programme actif. */
    public Seance enregistrerSeance(Seance s) {
        Utilisateur u = Session.exiger(Role.SPORTIF);
        s.setSportifId(u.getId());
        validerSeance(s);
        s.setProgrammeId(affectationDao.trouverActive(u.getId())
                .map(a -> a.getProgramme().getId()).orElse(null));
        return seanceDao.ajouter(s);
    }

    public void modifierSeance(Seance s) {
        verifierProprietaireSeance(s.getId());
        validerSeance(s);
        seanceDao.modifier(s);
    }

    public void supprimerSeance(int seanceId) {
        verifierProprietaireSeance(seanceId);
        seanceDao.supprimer(seanceId);
    }

    public List<Seance> seances(int sportifId) {
        acces.verifierAccesSportif(sportifId);
        return seanceDao.trouverParSportif(sportifId);
    }

    private void validerSeance(Seance s) {
        if (s.getDateSeance() == null || s.getDateSeance().isAfter(LocalDate.now())) {
            throw new ValidationException("La date de la séance est obligatoire et ne peut pas être dans le futur.");
        }
        if (s.getDureeMin() < 5 || s.getDureeMin() > 300) {
            throw new ValidationException("La durée doit être comprise entre 5 et 300 minutes.");
        }
        if (s.getRessenti() < 1 || s.getRessenti() > 5) {
            throw new ValidationException("Le ressenti doit être compris entre 1 et 5.");
        }
    }

    private void verifierProprietaireSeance(int seanceId) {
        Utilisateur u = Session.exiger(Role.SPORTIF);
        Seance s = seanceDao.trouverParId(seanceId)
                .orElseThrow(() -> new ValidationException("Séance introuvable."));
        if (s.getSportifId() != u.getId()) {
            throw new AccesRefuseException("Cette séance ne vous appartient pas.");
        }
    }

    // ---------------- Mesures ----------------

    /**
     * Enregistre une pesée. Règle de cohérence : si le poids varie de plus de 3 kg par rapport
     * à une pesée de moins de 7 jours, l'enregistrement est refusé tant que l'utilisateur
     * ne l'a pas confirmé (probable erreur de saisie).
     */
    public Mesure enregistrerMesure(Mesure m, boolean confirme) {
        Utilisateur u = Session.exiger(Role.SPORTIF, Role.COACH);
        if (u.getRole() == Role.SPORTIF) {
            m.setSportifId(u.getId());
        }
        acces.verifierAccesSportif(m.getSportifId());
        validerMesure(m);
        if (!confirme) {
            verifierVariation(m);
        }
        return mesureDao.ajouter(m);
    }

    public void modifierMesure(Mesure m) {
        Mesure existante = mesureDao.trouverParId(m.getId())
                .orElseThrow(() -> new ValidationException("Mesure introuvable."));
        acces.verifierAccesSportif(existante.getSportifId());
        Session.exiger(Role.SPORTIF, Role.COACH);
        m.setSportifId(existante.getSportifId());
        validerMesure(m);
        mesureDao.modifier(m);
    }

    public void supprimerMesure(int mesureId) {
        Mesure existante = mesureDao.trouverParId(mesureId)
                .orElseThrow(() -> new ValidationException("Mesure introuvable."));
        acces.verifierAccesSportif(existante.getSportifId());
        Session.exiger(Role.SPORTIF, Role.COACH);
        mesureDao.supprimer(mesureId);
    }

    public List<Mesure> mesures(int sportifId) {
        acces.verifierAccesSportif(sportifId);
        return mesureDao.trouverParSportif(sportifId);
    }

    private void validerMesure(Mesure m) {
        if (m.getDateMesure() == null || m.getDateMesure().isAfter(LocalDate.now())) {
            throw new ValidationException("La date de la mesure est obligatoire et ne peut pas être dans le futur.");
        }
        if (m.getPoidsKg() < 30 || m.getPoidsKg() > 300) {
            throw new ValidationException("Le poids doit être compris entre 30 et 300 kg.");
        }
        if (m.getTourTailleCm() != null && (m.getTourTailleCm() < 40 || m.getTourTailleCm() > 200)) {
            throw new ValidationException("Le tour de taille doit être compris entre 40 et 200 cm.");
        }
        if (m.getMasseGrassePct() != null && (m.getMasseGrassePct() < 2 || m.getMasseGrassePct() > 70)) {
            throw new ValidationException("La masse grasse doit être comprise entre 2 et 70 %.");
        }
    }

    private void verifierVariation(Mesure m) {
        mesureDao.trouverParSportif(m.getSportifId()).stream()
                .filter(p -> Math.abs(ChronoUnit.DAYS.between(p.getDateMesure(), m.getDateMesure())) < 7)
                .filter(p -> Math.abs(p.getPoidsKg() - m.getPoidsKg()) > VARIATION_SUSPECTE_KG)
                .findFirst()
                .ifPresent(p -> {
                    throw new VariationSuspecteException(String.format(
                            "Variation de %.1f kg par rapport à la pesée du %s. Confirmez-vous ce poids ?",
                            m.getPoidsKg() - p.getPoidsKg(), p.getDateMesure()));
                });
    }

    // ---------------- Statistiques ----------------

    public StatistiquesSportif statistiques(int sportifId) {
        acces.verifierAccesSportif(sportifId);
        LocalDate aujourdHui = LocalDate.now();

        // Séances par semaine sur les 8 dernières semaines (y compris les semaines vides)
        LocalDate lundiCourant = aujourdHui.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate debut8 = lundiCourant.minusWeeks(7);
        Map<LocalDate, Long> parSemaine = new TreeMap<>();
        for (int i = 0; i < 8; i++) {
            parSemaine.put(debut8.plusWeeks(i), 0L);
        }
        List<Seance> seances8 = seanceDao.trouverParSportifEntre(sportifId, debut8, aujourdHui);
        for (Seance s : seances8) {
            LocalDate lundi = s.getDateSeance().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            parSemaine.merge(lundi, 1L, Long::sum);
        }

        // Assiduité sur les 4 dernières semaines (ou depuis le début du programme si plus récent)
        LocalDate debut4 = aujourdHui.minusDays(27);
        Optional<Affectation> actif = affectationDao.trouverActive(sportifId);
        LocalDate debutPeriode = actif.map(a -> a.getDateDebut().isAfter(debut4) ? a.getDateDebut() : debut4)
                .orElse(debut4);
        List<Seance> seances4 = seanceDao.trouverParSportifEntre(sportifId, debutPeriode, aujourdHui);
        int realisees = seances4.size();

        int prevues = 0;
        Double assiduite = null;
        if (actif.isPresent()) {
            long joursParSemaine = ligneDao.trouverParProgramme(actif.get().getProgramme().getId()).stream()
                    .map(ProgrammeExercice::getJour).distinct().count();
            long jours = ChronoUnit.DAYS.between(debutPeriode, aujourdHui) + 1;
            prevues = (int) Math.round(joursParSemaine * jours / 7.0);
            if (prevues > 0) {
                assiduite = Math.min(100.0, Math.round(realisees * 1000.0 / prevues) / 10.0);
            }
        }
        Double ressenti = seances4.isEmpty() ? null
                : Math.round(seances4.stream().mapToInt(Seance::getRessenti).average().orElse(0) * 10) / 10.0;

        List<Mesure> mesures = mesureDao.trouverParSportif(sportifId);
        Double depart = mesures.isEmpty() ? null : mesures.get(0).getPoidsKg();
        Double actuel = mesures.isEmpty() ? null : mesures.get(mesures.size() - 1).getPoidsKg();

        return new StatistiquesSportif(parSemaine, realisees, prevues, assiduite, ressenti, depart, actuel);
    }
}
