package ma.emsi.coaching.model;

public enum NiveauProgramme {
    DEBUTANT("Débutant"),
    INTERMEDIAIRE("Intermédiaire"),
    AVANCE("Avancé");

    private final String libelle;

    NiveauProgramme(String libelle) {
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
