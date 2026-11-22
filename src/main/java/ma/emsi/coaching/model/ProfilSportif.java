package ma.emsi.coaching.model;

import java.time.LocalDate;
import java.time.Period;

/**
 * Profil physique d'un sportif, utilisé pour le calcul des besoins caloriques.
 */
public class ProfilSportif {

    private int utilisateurId;
    private Integer coachId;
    private Sexe sexe;
    private LocalDate dateNaissance;
    private double tailleCm;
    private Objectif objectif;
    private NiveauActivite niveauActivite;

    public ProfilSportif() {
    }

    public int getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(int utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public Integer getCoachId() {
        return coachId;
    }

    public void setCoachId(Integer coachId) {
        this.coachId = coachId;
    }

    public Sexe getSexe() {
        return sexe;
    }

    public void setSexe(Sexe sexe) {
        this.sexe = sexe;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public double getTailleCm() {
        return tailleCm;
    }

    public void setTailleCm(double tailleCm) {
        this.tailleCm = tailleCm;
    }

    public Objectif getObjectif() {
        return objectif;
    }

    public void setObjectif(Objectif objectif) {
        this.objectif = objectif;
    }

    public NiveauActivite getNiveauActivite() {
        return niveauActivite;
    }

    public void setNiveauActivite(NiveauActivite niveauActivite) {
        this.niveauActivite = niveauActivite;
    }

    public int getAge() {
        return Period.between(dateNaissance, LocalDate.now()).getYears();
    }

    @Override
    public String toString() {
        return "ProfilSportif{utilisateurId=" + utilisateurId + ", objectif=" + objectif + "}";
    }
}
