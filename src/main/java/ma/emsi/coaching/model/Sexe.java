package ma.emsi.coaching.model;

public enum Sexe {
    H("Homme"),
    F("Femme");

    private final String libelle;

    Sexe(String libelle) {
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
