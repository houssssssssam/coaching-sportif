package ma.emsi.coaching.model;

import java.time.LocalDateTime;

/**
 * Programme d'entraînement créé par un coach (table programme).
 */
public class Programme {

    private int id;
    private String nom;
    private String description;
    private Objectif objectif;
    private NiveauProgramme niveau;
    private int coachId;
    private LocalDateTime dateCreation;

    public Programme() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Objectif getObjectif() {
        return objectif;
    }

    public void setObjectif(Objectif objectif) {
        this.objectif = objectif;
    }

    public NiveauProgramme getNiveau() {
        return niveau;
    }

    public void setNiveau(NiveauProgramme niveau) {
        this.niveau = niveau;
    }

    public int getCoachId() {
        return coachId;
    }

    public void setCoachId(int coachId) {
        this.coachId = coachId;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    @Override
    public String toString() {
        return nom;
    }
}
