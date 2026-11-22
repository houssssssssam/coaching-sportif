package ma.emsi.coaching.model;

/**
 * Objectif du sportif. L'ajustement calorique est appliqué
 * aux besoins de maintien (TDEE) pour obtenir l'objectif journalier.
 */
public enum Objectif {
    PERTE_POIDS("Perte de poids", -500),
    MAINTIEN("Maintien", 0),
    PRISE_MASSE("Prise de masse", 300);

    private final String libelle;
    private final int ajustementKcal;

    Objectif(String libelle, int ajustementKcal) {
        this.libelle = libelle;
        this.ajustementKcal = ajustementKcal;
    }

    public String getLibelle() {
        return libelle;
    }

    public int getAjustementKcal() {
        return ajustementKcal;
    }

    @Override
    public String toString() {
        return libelle;
    }
}
