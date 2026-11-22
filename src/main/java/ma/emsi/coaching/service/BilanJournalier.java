package ma.emsi.coaching.service;

import ma.emsi.coaching.model.Repas;

import java.time.LocalDate;
import java.util.List;

/**
 * Comparaison entre ce que le sportif a mangé un jour donné et son objectif.
 */
public record BilanJournalier(LocalDate jour, List<Repas> repas, ObjectifNutritionnel objectif,
                              double calories, double proteinesG, double glucidesG, double lipidesG,
                              Statut statut) {

    public enum Statut {
        AUCUNE_SAISIE("Aucun repas saisi"),
        INSUFFISANT("Apport insuffisant"),
        DANS_OBJECTIF("Dans l'objectif"),
        EXCES("Objectif dépassé");

        private final String libelle;

        Statut(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }
    }

    /** Pourcentage de l'objectif calorique atteint. */
    public double pourcentage() {
        return objectif.calories() == 0 ? 0 : calories * 100 / objectif.calories();
    }

    public double restant() {
        return objectif.calories() - calories;
    }
}
