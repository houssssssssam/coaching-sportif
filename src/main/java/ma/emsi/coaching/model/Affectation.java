package ma.emsi.coaching.model;

import java.time.LocalDate;

/**
 * Affectation d'un programme à un sportif (table affectation).
 */
public class Affectation {

    private int id;
    private int sportifId;
    private Programme programme;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private StatutAffectation statut;

    public Affectation() {
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

    public Programme getProgramme() {
        return programme;
    }

    public void setProgramme(Programme programme) {
        this.programme = programme;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public StatutAffectation getStatut() {
        return statut;
    }

    public void setStatut(StatutAffectation statut) {
        this.statut = statut;
    }

    @Override
    public String toString() {
        return "Affectation{sportifId=" + sportifId + ", programme=" + programme + ", statut=" + statut + "}";
    }
}
