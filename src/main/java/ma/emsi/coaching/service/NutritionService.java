package ma.emsi.coaching.service;

import ma.emsi.coaching.dao.AlimentDao;
import ma.emsi.coaching.dao.MesureDao;
import ma.emsi.coaching.dao.ProfilSportifDao;
import ma.emsi.coaching.dao.RepasDao;
import ma.emsi.coaching.model.Aliment;
import ma.emsi.coaching.model.Mesure;
import ma.emsi.coaching.model.Objectif;
import ma.emsi.coaching.model.ProfilSportif;
import ma.emsi.coaching.model.Repas;
import ma.emsi.coaching.model.Role;
import ma.emsi.coaching.model.Sexe;
import ma.emsi.coaching.model.Utilisateur;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Règle métier principale : calcul des besoins nutritionnels et suivi du journal alimentaire.
 */
public class NutritionService {

    /** Tolérance autour de l'objectif calorique pour être considéré "dans l'objectif". */
    private static final double TOLERANCE = 0.10;

    private final ProfilSportifDao profilDao = new ProfilSportifDao();
    private final MesureDao mesureDao = new MesureDao();
    private final RepasDao repasDao = new RepasDao();
    private final AlimentDao alimentDao = new AlimentDao();
    private final AccesService acces = new AccesService();

    // ------------------------------------------------------------------
    // Calcul de l'objectif
    // ------------------------------------------------------------------

    /**
     * Calcule l'objectif nutritionnel journalier :
     * 1. Métabolisme de base (Mifflin-St Jeor) :
     *    homme = 10 x poids + 6,25 x taille - 5 x âge + 5
     *    femme = 10 x poids + 6,25 x taille - 5 x âge - 161
     * 2. Maintien = métabolisme x facteur d'activité (1,2 à 1,9)
     * 3. Objectif = maintien + ajustement (-500 perte, 0 maintien, +300 prise de masse),
     *    sans descendre sous un plancher de sécurité (1500 kcal homme, 1200 kcal femme).
     * 4. Macros : protéines selon l'objectif (g/kg), lipides = 25 % des calories, glucides = le reste.
     */
    public static ObjectifNutritionnel calculerObjectif(ProfilSportif p, double poidsKg) {
        double base = 10 * poidsKg + 6.25 * p.getTailleCm() - 5 * p.getAge()
                + (p.getSexe() == Sexe.H ? 5 : -161);
        double maintien = base * p.getNiveauActivite().getFacteur();
        double plancher = p.getSexe() == Sexe.H ? 1500 : 1200;
        double calories = Math.max(plancher, maintien + p.getObjectif().getAjustementKcal());

        double proteinesParKg = switch (p.getObjectif()) {
            case PERTE_POIDS -> 2.0;
            case PRISE_MASSE -> 1.8;
            case MAINTIEN -> 1.6;
        };
        double proteines = proteinesParKg * poidsKg;
        double lipides = calories * 0.25 / 9;
        double glucides = Math.max(0, (calories - proteines * 4 - lipides * 9) / 4);
        return new ObjectifNutritionnel(arrondir(base), arrondir(maintien), arrondir(calories),
                arrondir(proteines), arrondir(glucides), arrondir(lipides));
    }

    /** Objectif d'un sportif à partir de son profil et de sa dernière pesée. */
    public ObjectifNutritionnel objectifDe(int sportifId) {
        acces.verifierAccesSportif(sportifId);
        ProfilSportif profil = profilDao.trouverParSportif(sportifId)
                .orElseThrow(() -> new ValidationException("Ce sportif n'a pas de profil."));
        double poids = mesureDao.trouverDerniere(sportifId).map(Mesure::getPoidsKg)
                .orElseThrow(() -> new ValidationException(
                        "Aucune pesée enregistrée : ajoutez une mesure pour calculer l'objectif."));
        return calculerObjectif(profil, poids);
    }

    // ------------------------------------------------------------------
    // Journal alimentaire
    // ------------------------------------------------------------------

    public BilanJournalier bilanDuJour(int sportifId, LocalDate jour) {
        ObjectifNutritionnel objectif = objectifDe(sportifId);
        List<Repas> repas = repasDao.trouverParJour(sportifId, jour);
        double kcal = repas.stream().mapToDouble(Repas::getCalories).sum();
        double p = repas.stream().mapToDouble(Repas::getProteines).sum();
        double g = repas.stream().mapToDouble(Repas::getGlucides).sum();
        double l = repas.stream().mapToDouble(Repas::getLipides).sum();

        BilanJournalier.Statut statut;
        if (repas.isEmpty()) {
            statut = BilanJournalier.Statut.AUCUNE_SAISIE;
        } else if (kcal < objectif.calories() * (1 - TOLERANCE)) {
            statut = BilanJournalier.Statut.INSUFFISANT;
        } else if (kcal > objectif.calories() * (1 + TOLERANCE)) {
            statut = BilanJournalier.Statut.EXCES;
        } else {
            statut = BilanJournalier.Statut.DANS_OBJECTIF;
        }
        return new BilanJournalier(jour, repas, objectif, arrondir(kcal), arrondir(p), arrondir(g),
                arrondir(l), statut);
    }

    /**
     * Calories consommées par jour sur une période (les jours sans repas valent 0),
     * dans l'ordre chronologique. Sert aux graphiques du tableau de bord.
     */
    public Map<LocalDate, Double> caloriesParJour(int sportifId, LocalDate debut, LocalDate fin) {
        acces.verifierAccesSportif(sportifId);
        Map<LocalDate, Double> parJour = new TreeMap<>();
        for (LocalDate d = debut; !d.isAfter(fin); d = d.plusDays(1)) {
            parJour.put(d, 0.0);
        }
        for (Repas r : repasDao.trouverEntre(sportifId, debut, fin)) {
            parJour.merge(r.getDateRepas(), r.getCalories(), Double::sum);
        }
        parJour.replaceAll((d, kcal) -> arrondir(kcal));
        return parJour;
    }

    public List<Repas> journal(int sportifId, LocalDate debut, LocalDate fin) {
        acces.verifierAccesSportif(sportifId);
        return repasDao.trouverEntre(sportifId, debut, fin);
    }

    public Repas ajouterAuJournal(Repas r) {
        Utilisateur u = Session.exiger(Role.SPORTIF);
        r.setSportifId(u.getId());
        validerRepas(r);
        return repasDao.ajouter(r);
    }

    public void modifierRepas(Repas r) {
        verifierProprietaireRepas(r.getId());
        validerRepas(r);
        repasDao.modifier(r);
    }

    public void supprimerRepas(int repasId) {
        verifierProprietaireRepas(repasId);
        repasDao.supprimer(repasId);
    }

    private void validerRepas(Repas r) {
        if (r.getAliment() == null || r.getTypeRepas() == null || r.getDateRepas() == null) {
            throw new ValidationException("Choisissez un aliment, un type de repas et une date.");
        }
        if (r.getQuantiteG() <= 0 || r.getQuantiteG() > 2000) {
            throw new ValidationException("La quantité doit être comprise entre 1 et 2000 g.");
        }
        if (r.getDateRepas().isAfter(LocalDate.now())) {
            throw new ValidationException("Impossible de saisir un repas dans le futur.");
        }
    }

    private void verifierProprietaireRepas(int repasId) {
        Utilisateur u = Session.exiger(Role.SPORTIF);
        Repas existant = repasDao.trouverParId(repasId)
                .orElseThrow(() -> new ValidationException("Repas introuvable."));
        if (existant.getSportifId() != u.getId()) {
            throw new AccesRefuseException("Ce repas ne vous appartient pas.");
        }
    }

    // ------------------------------------------------------------------
    // Aliments
    // ------------------------------------------------------------------

    /** Recherche d'aliments façon MyFitnessPal (au moins 2 caractères). */
    public List<Aliment> rechercherAliments(String texte) {
        Utilisateur u = Session.getUtilisateur();
        if (texte == null || texte.trim().length() < 2) {
            return List.of();
        }
        return alimentDao.rechercher(texte, u.getId());
    }

    public List<Aliment> catalogue() {
        Session.getUtilisateur();
        return alimentDao.trouverTous();
    }

    /**
     * Ajout d'un aliment. Un admin enrichit le catalogue officiel,
     * un sportif crée un aliment personnalisé visible par lui seul.
     */
    public Aliment ajouterAliment(Aliment a) {
        Utilisateur u = Session.exiger(Role.ADMIN, Role.SPORTIF);
        a.setCreePar(u.getRole() == Role.SPORTIF ? u.getId() : null);
        if (a.getCategorie() == null || a.getCategorie().isBlank()) {
            a.setCategorie(u.getRole() == Role.SPORTIF ? "Personnalisé" : "Divers");
        }
        validerAliment(a);
        return alimentDao.ajouter(a);
    }

    public void modifierAliment(Aliment a) {
        verifierDroitAliment(a.getId());
        validerAliment(a);
        alimentDao.modifier(a);
    }

    public void supprimerAliment(int id) {
        verifierDroitAliment(id);
        alimentDao.supprimer(id);
    }

    private void verifierDroitAliment(int id) {
        Utilisateur u = Session.exiger(Role.ADMIN, Role.SPORTIF);
        Aliment existant = alimentDao.trouverParId(id)
                .orElseThrow(() -> new ValidationException("Aliment introuvable."));
        boolean autorise = u.getRole() == Role.ADMIN
                ? !existant.isPersonnalise()
                : existant.getCreePar() != null && existant.getCreePar() == u.getId();
        if (!autorise) {
            throw new AccesRefuseException("Vous ne pouvez pas modifier cet aliment.");
        }
    }

    /**
     * Contrôle de cohérence : pour 100 g, protéines + glucides + lipides <= 100 g,
     * et les calories doivent correspondre aux macros (4/4/9 kcal par gramme) à 20 % près.
     */
    static void validerAliment(Aliment a) {
        if (a.getNom() == null || a.getNom().isBlank()) {
            throw new ValidationException("Le nom de l'aliment est obligatoire.");
        }
        if (a.getCalories100g() < 0 || a.getProteines100g() < 0 || a.getGlucides100g() < 0
                || a.getLipides100g() < 0) {
            throw new ValidationException("Les valeurs nutritionnelles ne peuvent pas être négatives.");
        }
        double macros = a.getProteines100g() + a.getGlucides100g() + a.getLipides100g();
        if (macros > 100) {
            throw new ValidationException("Protéines + glucides + lipides ne peuvent pas dépasser 100 g pour 100 g.");
        }
        double kcalTheorique = a.getProteines100g() * 4 + a.getGlucides100g() * 4 + a.getLipides100g() * 9;
        if (kcalTheorique > 20 && Math.abs(a.getCalories100g() - kcalTheorique) > kcalTheorique * 0.2) {
            throw new ValidationException(String.format(
                    "Les calories (%.0f kcal) ne correspondent pas aux macros (environ %.0f kcal attendues).",
                    a.getCalories100g(), kcalTheorique));
        }
        a.setNom(a.getNom().trim());
    }

    static double arrondir(double v) {
        return Math.round(v * 10) / 10.0;
    }

    /** Utile à l'affichage : libellé de l'objectif avec son ajustement. */
    public static String decrire(Objectif o) {
        int a = o.getAjustementKcal();
        return o.getLibelle() + (a == 0 ? "" : " (" + (a > 0 ? "+" : "") + a + " kcal)");
    }
}
