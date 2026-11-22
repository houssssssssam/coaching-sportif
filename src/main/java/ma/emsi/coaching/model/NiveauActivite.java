package ma.emsi.coaching.model;

/**
 * Niveau d'activité physique quotidienne et facteur multiplicateur
 * appliqué au métabolisme de base.
 */
public enum NiveauActivite {
    SEDENTAIRE("Sédentaire", 1.2),
    LEGER("Légèrement actif", 1.375),
    MODERE("Modérément actif", 1.55),
    ACTIF("Actif", 1.725),
    TRES_ACTIF("Très actif", 1.9);

    private final String libelle;
    private final double facteur;

    NiveauActivite(String libelle, double facteur) {
        this.libelle = libelle;
        this.facteur = facteur;
    }

    public String getLibelle() {
        return libelle;
    }

    public double getFacteur() {
        return facteur;
    }

    @Override
    public String toString() {
        return libelle;
    }
}
