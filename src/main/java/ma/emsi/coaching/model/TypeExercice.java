package ma.emsi.coaching.model;

public enum TypeExercice {
    FORCE("Force"),
    CARDIO("Cardio"),
    MOBILITE("Mobilité");

    private final String libelle;

    TypeExercice(String libelle) {
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
