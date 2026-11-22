package ma.emsi.coaching.model;

import java.time.LocalDate;

/**
 * Séance d'entraînement réalisée par un sportif (table seance).
 */
public class Seance {

    private int id;
    private int sportifId;
    private Integer programmeId;
    private LocalDate dateSeance;
    private int dureeMin;
    private int ressenti;
    private String commentaire;

    public Seance() {
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

    public Integer getProgrammeId() {
        return programmeId;
    }

    public void setProgrammeId(Integer programmeId) {
        this.programmeId = programmeId;
    }

    public LocalDate getDateSeance() {
        return dateSeance;
    }

    public void setDateSeance(LocalDate dateSeance) {
        this.dateSeance = dateSeance;
    }

    public int getDureeMin() {
        return dureeMin;
    }

    public void setDureeMin(int dureeMin) {
        this.dureeMin = dureeMin;
    }

    public int getRessenti() {
        return ressenti;
    }

    public void setRessenti(int ressenti) {
        this.ressenti = ressenti;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    @Override
    public String toString() {
        return "Seance{" + dateSeance + ", " + dureeMin + " min, ressenti=" + ressenti + "}";
    }
}
