package ma.emsi.coaching.model;

public enum StatutAffectation {
    ACTIF("Actif"),
    TERMINE("Terminé");

    private final String libelle;

    StatutAffectation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    @Override
    public String toString() {
        return libelle;
    }
}
