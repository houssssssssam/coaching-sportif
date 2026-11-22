package ma.emsi.coaching.service;

/**
 * Objectif journalier calculé pour un sportif.
 *
 * @param metabolismeBase métabolisme de base (Mifflin-St Jeor), en kcal
 * @param maintien        dépense journalière totale (métabolisme x facteur d'activité), en kcal
 * @param calories        objectif calorique après ajustement selon l'objectif, en kcal
 * @param proteinesG      objectif de protéines, en grammes
 * @param glucidesG       objectif de glucides, en grammes
 * @param lipidesG        objectif de lipides, en grammes
 */
public record ObjectifNutritionnel(double metabolismeBase, double maintien, double calories,
                                   double proteinesG, double glucidesG, double lipidesG) {
}
