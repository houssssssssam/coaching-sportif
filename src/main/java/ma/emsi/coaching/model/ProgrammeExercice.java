package ma.emsi.coaching.model;

/**
 * Ligne d'un programme : un exercice, un jour, des séries/répétitions ou une durée.
 */
public class ProgrammeExercice {

    private int id;
    private int programmeId;
    private Exercice exercice;
    private int jour;
    private int ordre;
    private Integer series;
    private Integer repetitions;
    private Double chargeKg;
    private Integer dureeMin;
    private String consigne;

    public ProgrammeExercice() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProgrammeId() {
        return programmeId;
    }

    public void setProgrammeId(int programmeId) {
        this.programmeId = programmeId;
    }

    public Exercice getExercice() {
        return exercice;
    }

    public void setExercice(Exercice exercice) {
        this.exercice = exercice;
    }

    public int getJour() {
        return jour;
    }

    public void setJour(int jour) {
        this.jour = jour;
    }

    public int getOrdre() {
        return ordre;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }

    public Integer getSeries() {
        return series;
    }

    public void setSeries(Integer series) {
        this.series = series;
    }

    public Integer getRepetitions() {
        return repetitions;
    }

    public void setRepetitions(Integer repetitions) {
        this.repetitions = repetitions;
    }

    public Double getChargeKg() {
        return chargeKg;
    }

    public void setChargeKg(Double chargeKg) {
        this.chargeKg = chargeKg;
    }

    public Integer getDureeMin() {
        return dureeMin;
    }

    public void setDureeMin(Integer dureeMin) {
        this.dureeMin = dureeMin;
    }

    public String getConsigne() {
        return consigne;
    }

    public void setConsigne(String consigne) {
        this.consigne = consigne;
    }

    @Override
    public String toString() {
        return "Jour " + jour + " - " + (exercice != null ? exercice.getNom() : "?");
    }
}
