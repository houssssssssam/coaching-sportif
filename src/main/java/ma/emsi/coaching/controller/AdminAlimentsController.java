package ma.emsi.coaching.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ma.emsi.coaching.model.Aliment;
import ma.emsi.coaching.service.NutritionService;
import ma.emsi.coaching.util.Alertes;
import ma.emsi.coaching.util.Formulaires;

import java.util.List;

/**
 * Gestion du catalogue officiel des aliments par l'administrateur.
 */
public class AdminAlimentsController {

    private static final String TOUTES = "Toutes les catégories";

    @FXML private TextField champRecherche;
    @FXML private ComboBox<String> filtreCategorie;
    @FXML private TableView<Aliment> table;
    @FXML private TableColumn<Aliment, String> colNom;
    @FXML private TableColumn<Aliment, String> colCategorie;
    @FXML private TableColumn<Aliment, String> colKcal;
    @FXML private TableColumn<Aliment, String> colProteines;
    @FXML private TableColumn<Aliment, String> colGlucides;
    @FXML private TableColumn<Aliment, String> colLipides;

    @FXML private Label titreFormulaire;
    @FXML private TextField champNom;
    @FXML private ComboBox<String> comboCategorie;
    @FXML private TextField champKcal;
    @FXML private TextField champProteines;
    @FXML private TextField champGlucides;
    @FXML private TextField champLipides;
    @FXML private Label kcalCalculees;
    @FXML private Button boutonSupprimer;

    private final NutritionService service = new NutritionService();
    private FilteredList<Aliment> liste;
    private Aliment selection;

    @FXML
    private void initialize() {
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colCategorie.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategorie()));
        colKcal.setCellValueFactory(c -> new SimpleStringProperty(Formulaires.texte(c.getValue().getCalories100g())));
        colProteines.setCellValueFactory(c -> new SimpleStringProperty(Formulaires.texte(c.getValue().getProteines100g())));
        colGlucides.setCellValueFactory(c -> new SimpleStringProperty(Formulaires.texte(c.getValue().getGlucides100g())));
        colLipides.setCellValueFactory(c -> new SimpleStringProperty(Formulaires.texte(c.getValue().getLipides100g())));

        champRecherche.textProperty().addListener((o, a, n) -> filtrer());
        filtreCategorie.valueProperty().addListener((o, a, n) -> filtrer());
        table.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            if (n != null) {
                remplir(n);
            }
        });
        // Aide à la saisie : calories attendues d'après les macros (4 / 4 / 9 kcal par gramme)
        for (TextField t : List.of(champProteines, champGlucides, champLipides)) {
            t.textProperty().addListener((o, a, n) -> afficherKcalCalculees());
        }

        charger();
        nouveau();
    }

    private void charger() {
        List<Aliment> aliments = service.catalogue();
        liste = new FilteredList<>(FXCollections.observableArrayList(aliments));
        table.setItems(liste);

        List<String> categories = aliments.stream().map(Aliment::getCategorie).distinct().sorted().toList();
        String filtreActuel = filtreCategorie.getValue();
        filtreCategorie.getItems().setAll(TOUTES);
        filtreCategorie.getItems().addAll(categories);
        filtreCategorie.setValue(filtreActuel != null && categories.contains(filtreActuel) ? filtreActuel : TOUTES);
        comboCategorie.getItems().setAll(categories);
        filtrer();
    }

    private void filtrer() {
        String texte = champRecherche.getText() == null ? "" : champRecherche.getText().toLowerCase().trim();
        String cat = filtreCategorie.getValue();
        liste.setPredicate(a ->
                (cat == null || TOUTES.equals(cat) || a.getCategorie().equals(cat))
                        && (texte.isEmpty() || a.getNom().toLowerCase().contains(texte)));
    }

    @FXML
    private void nouveau() {
        selection = null;
        table.getSelectionModel().clearSelection();
        titreFormulaire.setText("Nouvel aliment");
        champNom.clear();
        comboCategorie.setValue(null);
        comboCategorie.getEditor().clear();
        champKcal.clear();
        champProteines.clear();
        champGlucides.clear();
        champLipides.clear();
        boutonSupprimer.setDisable(true);
    }

    private void remplir(Aliment a) {
        selection = a;
        titreFormulaire.setText("Modifier l'aliment");
        champNom.setText(a.getNom());
        comboCategorie.setValue(a.getCategorie());
        champKcal.setText(Formulaires.texte(a.getCalories100g()));
        champProteines.setText(Formulaires.texte(a.getProteines100g()));
        champGlucides.setText(Formulaires.texte(a.getGlucides100g()));
        champLipides.setText(Formulaires.texte(a.getLipides100g()));
        boutonSupprimer.setDisable(false);
    }

    private void afficherKcalCalculees() {
        try {
            double p = Formulaires.lireDouble(champProteines, "Protéines");
            double g = Formulaires.lireDouble(champGlucides, "Glucides");
            double l = Formulaires.lireDouble(champLipides, "Lipides");
            kcalCalculees.setText(String.format("D'après les macros : environ %.0f kcal.", p * 4 + g * 4 + l * 9));
        } catch (RuntimeException e) {
            kcalCalculees.setText("");
        }
    }

    @FXML
    private void enregistrer() {
        try {
            Aliment a = selection != null ? selection : new Aliment();
            a.setNom(champNom.getText());
            a.setCategorie(comboCategorie.getEditor().getText());
            a.setCalories100g(Formulaires.lireDouble(champKcal, "Calories"));
            a.setProteines100g(Formulaires.lireDouble(champProteines, "Protéines"));
            a.setGlucides100g(Formulaires.lireDouble(champGlucides, "Glucides"));
            a.setLipides100g(Formulaires.lireDouble(champLipides, "Lipides"));
            if (selection == null) {
                service.ajouterAliment(a);
            } else {
                service.modifierAliment(a);
            }
            charger();
            nouveau();
        } catch (RuntimeException e) {
            Alertes.gerer(e);
            charger();
        }
    }

    @FXML
    private void supprimer() {
        if (selection == null || !Alertes.confirmer("Supprimer l'aliment « " + selection.getNom() + " » ?")) {
            return;
        }
        try {
            service.supprimerAliment(selection.getId());
            charger();
            nouveau();
        } catch (RuntimeException e) {
            Alertes.gerer(e);
        }
    }
}
