package ma.emsi.coaching.model;

/**
 * Exercice du catalogue (table exercice).
 */
public class Exercice {

    private int id;
    private String nom;
    private String groupeMusculaire;
    private TypeExercice type;
    private String description;

    public Exercice() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getGroupeMusculaire() {
        return groupeMusculaire;
    }

    public void setGroupeMusculaire(String groupeMusculaire) {
        this.groupeMusculaire = groupeMusculaire;
    }

    public TypeExercice getType() {
        return type;
    }

    public void setType(TypeExercice type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return nom;
    }
}
