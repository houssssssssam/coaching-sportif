package ma.emsi.coaching.controller;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import ma.emsi.coaching.model.Mesure;
import ma.emsi.coaching.model.Role;
import ma.emsi.coaching.model.Utilisateur;
import ma.emsi.coaching.service.ExportService;
import ma.emsi.coaching.service.NutritionService;
import ma.emsi.coaching.service.ObjectifNutritionnel;
import ma.emsi.coaching.service.Session;
import ma.emsi.coaching.service.StatistiquesSportif;
import ma.emsi.coaching.service.SuiviService;
import ma.emsi.coaching.service.ValidationException;
import ma.emsi.coaching.util.Alertes;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Tableau de bord d'un sportif : indicateurs, graphiques et export CSV.
 * Ouvert depuis le menu par le sportif, ou par son coach depuis "Mes sportifs".
 */
public class TableauBordController {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM");

    @FXML private Label titre;
    @FXML private ComboBox<ExportService.TypeExport> comboExport;
    @FXML private FlowPane cartes;
    @FXML private LineChart<String, Number> graphePoids;
    @FXML private NumberAxis axePoids;
    @FXML private BarChart<String, Number> grapheSeances;
    @FXML private NumberAxis axeSeances;
    @FXML private LineChart<String, Number> grapheCalories;

    private final SuiviService suiviService = new SuiviService();
    private final NutritionService nutritionService = new NutritionService();
    private int sportifId;
    private String nomSportif;

    @FXML
    private void initialize() {
        comboExport.getItems().setAll(ExportService.TypeExport.values());
        comboExport.setValue(ExportService.TypeExport.SEANCES);
        Utilisateur u = Session.getUtilisateur();
        if (u.getRole() == Role.SPORTIF) {
            afficher(u.getId(), u.getNomComplet());
        }
    }

    /** Affiche le tableau de bord d'un sportif (appelé directement par le coach). */
    public void afficher(int id, String nom) {
        this.sportifId = id;
        this.nomSportif = nom;
        titre.setText(Session.getUtilisateur().getRole() == Role.SPORTIF
                ? "Tableau de bord" : "Tableau de bord de " + nom);
        cartes.getChildren().clear();
        StatistiquesSportif st = suiviService.statistiques(id);
        remplirCartes(st);
        remplirPoids(suiviService.mesures(id));
        remplirSeances(st.seancesParSemaine());
        remplirCalories();
    }

    private void remplirCartes(StatistiquesSportif st) {
        if (st.poidsActuel() != null) {
            Double v = st.variationPoids();
            carte("Poids actuel", st.poidsActuel() + " kg",
                    v == null ? "" : (v > 0 ? "+" : "") + v + " kg depuis le début");
        }
        carte("Séances (4 semaines)", String.valueOf(st.seancesRealisees()),
                st.seancesPrevues() > 0 ? "sur " + st.seancesPrevues() + " prévues" : "aucun programme actif");
        carte("Assiduité", st.assiduitePct() == null ? "-" : st.assiduitePct() + " %", "séances réalisées / prévues");
        carte("Ressenti moyen", st.ressentiMoyen() == null ? "-" : st.ressentiMoyen() + " / 5", "4 dernières semaines");
    }

    private void carte(String libelle, String valeur, String detail) {
        Label l = new Label(libelle);
        l.getStyleClass().add("carte-libelle");
        Label v = new Label(valeur);
        v.getStyleClass().add("carte-valeur");
        Label d = new Label(detail);
        d.getStyleClass().add("texte-secondaire");
        VBox c = new VBox(4, l, v, d);
        c.getStyleClass().add("carte");
        cartes.getChildren().add(c);
    }

    private void remplirPoids(List<Mesure> mesures) {
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (Mesure m : mesures) {
            serie.getData().add(new XYChart.Data<>(m.getDateMesure().format(JOUR), m.getPoidsKg()));
            min = Math.min(min, m.getPoidsKg());
            max = Math.max(max, m.getPoidsKg());
        }
        graphePoids.getData().setAll(List.of(serie));
        if (!mesures.isEmpty()) {
            // Échelle resserrée autour des valeurs pour que la tendance soit lisible
            axePoids.setAutoRanging(false);
            axePoids.setLowerBound(Math.floor(min - 1));
            axePoids.setUpperBound(Math.ceil(max + 1));
            axePoids.setTickUnit(Math.max(1, Math.round((max - min + 2) / 6)));
        }
    }

    private void remplirSeances(Map<LocalDate, Long> parSemaine) {
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        parSemaine.forEach((lundi, n) -> serie.getData().add(new XYChart.Data<>(lundi.format(JOUR), n)));
        grapheSeances.getData().setAll(List.of(serie));
        // Un nombre de séances est entier : graduation de 1 en 1
        long max = parSemaine.values().stream().mapToLong(Long::longValue).max().orElse(0);
        axeSeances.setAutoRanging(false);
        axeSeances.setLowerBound(0);
        axeSeances.setUpperBound(Math.max(3, max + 1));
        axeSeances.setTickUnit(1);
    }

    private void remplirCalories() {
        LocalDate fin = LocalDate.now();
        LocalDate debut = fin.minusDays(13);
        // Seuls les jours où des repas ont été saisis sont tracés : un jour vide n'est pas un jour à 0 kcal
        Map<LocalDate, Double> jours = nutritionService.caloriesParJour(sportifId, debut, fin);
        jours.values().removeIf(kcal -> kcal == 0);
        XYChart.Series<String, Number> consommees = new XYChart.Series<>();
        consommees.setName("Consommées");
        jours.forEach((d, kcal) -> consommees.getData().add(new XYChart.Data<>(d.format(JOUR), kcal)));
        try {
            ObjectifNutritionnel o = nutritionService.objectifDe(sportifId);
            XYChart.Series<String, Number> objectif = new XYChart.Series<>();
            objectif.setName("Objectif (" + Math.round(o.calories()) + " kcal)");
            jours.keySet().forEach(d -> objectif.getData().add(new XYChart.Data<>(d.format(JOUR), o.calories())));
            grapheCalories.getData().setAll(List.of(consommees, objectif));
        } catch (ValidationException e) {
            grapheCalories.getData().setAll(List.of(consommees));
        }
    }

    @FXML
    private void exporter() {
        ExportService.TypeExport type = comboExport.getValue();
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter en CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));
        fc.setInitialFileName((type.name().toLowerCase() + "-" + nomSportif + "-" + LocalDate.now())
                .replace(' ', '-') + ".csv");
        File f = fc.showSaveDialog(titre.getScene().getWindow());
        if (f == null) {
            return;
        }
        try {
            int n = new ExportService().exporter(sportifId, type, f.toPath());
            Alertes.info(n + " ligne(s) exportée(s) dans " + f.getName() + ".");
        } catch (RuntimeException e) {
            Alertes.gerer(e);
        }
    }
}
