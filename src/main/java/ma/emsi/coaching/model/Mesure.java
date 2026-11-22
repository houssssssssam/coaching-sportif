package ma.emsi.coaching.model;

import java.time.LocalDate;

/**
 * Mesure corporelle d'un sportif à une date donnée (table mesure).
 */
public class Mesure {

    private int id;
    private int sportifId;
    private LocalDate dateMesure;
    private double poidsKg;
    private Double tourTailleCm;
    private Double masseGrassePct;

    public Mesure() {
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

    public LocalDate getDateMesure() {
        return dateMesure;
    }

    public void setDateMesure(LocalDate dateMesure) {
        this.dateMesure = dateMesure;
    }

    public double getPoidsKg() {
        return poidsKg;
    }

    public void setPoidsKg(double poidsKg) {
        this.poidsKg = poidsKg;
    }

    public Double getTourTailleCm() {
        return tourTailleCm;
    }

    public void setTourTailleCm(Double tourTailleCm) {
        this.tourTailleCm = tourTailleCm;
    }

    public Double getMasseGrassePct() {
        return masseGrassePct;
    }

    public void setMasseGrassePct(Double masseGrassePct) {
        this.masseGrassePct = masseGrassePct;
    }

    @Override
    public String toString() {
        return "Mesure{" + dateMesure + ", " + poidsKg + " kg}";
    }
}
