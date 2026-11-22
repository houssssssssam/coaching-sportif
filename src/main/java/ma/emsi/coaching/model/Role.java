package ma.emsi.coaching.model;

public enum Role {
    ADMIN("Administrateur"),
    COACH("Coach"),
    SPORTIF("Sportif");

    private final String libelle;

    Role(String libelle) {
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
