package ma.emsi.coaching.model;

/**
 * Aliment du catalogue, valeurs nutritionnelles pour 100 g (table aliment).
 */
public class Aliment {

    private int id;
    private String nom;
    private String categorie;
    private double calories100g;
    private double proteines100g;
    private double glucides100g;
    private double lipides100g;
    private Integer creePar;

    public Aliment() {
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

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public double getCalories100g() {
        return calories100g;
    }

    public void setCalories100g(double calories100g) {
        this.calories100g = calories100g;
    }

    public double getProteines100g() {
        return proteines100g;
    }

    public void setProteines100g(double proteines100g) {
        this.proteines100g = proteines100g;
    }

    public double getGlucides100g() {
        return glucides100g;
    }

    public void setGlucides100g(double glucides100g) {
        this.glucides100g = glucides100g;
    }

    public double getLipides100g() {
        return lipides100g;
    }

    public void setLipides100g(double lipides100g) {
        this.lipides100g = lipides100g;
    }

    public Integer getCreePar() {
        return creePar;
    }

    public void setCreePar(Integer creePar) {
        this.creePar = creePar;
    }

    /** Un aliment est personnalisé s'il a été créé par un sportif. */
    public boolean isPersonnalise() {
        return creePar != null;
    }

    @Override
    public String toString() {
        return nom;
    }
}
