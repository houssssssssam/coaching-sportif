package ma.emsi.coaching.model;

public enum TypeRepas {
    PETIT_DEJEUNER("Petit-déjeuner"),
    DEJEUNER("Déjeuner"),
    DINER("Dîner"),
    COLLATION("Collation");

    private final String libelle;

    TypeRepas(String libelle) {
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
