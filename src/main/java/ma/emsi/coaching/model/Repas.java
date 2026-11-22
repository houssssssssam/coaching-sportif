package ma.emsi.coaching.model;

import java.time.LocalDate;

/**
 * Ligne du journal alimentaire : un aliment consommé à un repas (table repas).
 */
public class Repas {

    private int id;
    private int sportifId;
    private LocalDate dateRepas;
    private TypeRepas typeRepas;
    private Aliment aliment;
    private double quantiteG;

    public Repas() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSportifId() {
        return sportifId;
    }

    public void setSportifId(int sportifId) {
        this.sportifId = sportifId;
    }

    public LocalDate getDateRepas() {
        return dateRepas;
    }

    public void setDateRepas(LocalDate dateRepas) {
        this.dateRepas = dateRepas;
    }

    public TypeRepas getTypeRepas() {
        return typeRepas;
    }

    public void setTypeRepas(TypeRepas typeRepas) {
        this.typeRepas = typeRepas;
    }

    public Aliment getAliment() {
        return aliment;
    }

    public void setAliment(Aliment aliment) {
        this.aliment = aliment;
    }

    public double getQuantiteG() {
        return quantiteG;
    }

    public void setQuantiteG(double quantiteG) {
        this.quantiteG = quantiteG;
    }

    public double getCalories() {
        return aliment.getCalories100g() * quantiteG / 100;
    }

    public double getProteines() {
        return aliment.getProteines100g() * quantiteG / 100;
    }

    public double getGlucides() {
        return aliment.getGlucides100g() * quantiteG / 100;
    }

    public double getLipides() {
        return aliment.getLipides100g() * quantiteG / 100;
    }

    @Override
    public String toString() {
        return typeRepas + " - " + aliment + " (" + quantiteG + " g)";
    }
}
