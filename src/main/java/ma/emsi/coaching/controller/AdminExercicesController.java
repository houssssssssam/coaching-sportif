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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import ma.emsi.coaching.model.Exercice;
import ma.emsi.coaching.model.TypeExercice;
import ma.emsi.coaching.service.CatalogueService;
import ma.emsi.coaching.util.Alertes;

import java.util.List;

/**
 * Gestion du catalogue des exercices par l'administrateur.
 */
public class AdminExercicesController {

    private static final String TOUS = "Tous les types";

    @FXML private TextField champRecherche;
    @FXML private ComboBox<String> filtreType;
    @FXML private TableView<Exercice> table;
    @FXML private TableColumn<Exercice, String> colNom;
    @FXML private TableColumn<Exercice, String> colGroupe;
    @FXML private TableColumn<Exercice, String> colType;

    @FXML private Label titreFormulaire;
    @FXML private TextField champNom;
    @FXML private ComboBox<String> comboGroupe;
    @FXML private ComboBox<TypeExercice> comboType;
    @FXML private TextArea champDescription;
    @FXML private Button boutonSupprimer;

    private final CatalogueService service = new CatalogueService();
    private FilteredList<Exercice> liste;
    private Exercice selection;

    @FXML
    private void initialize() {
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colGroupe.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGroupeMusculaire()));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType().getLibelle()));

        filtreType.getItems().add(TOUS);
        for (TypeExercice t : TypeExercice.values()) {
            filtreType.getItems().add(t.getLibelle());
        }
        filtreType.setValue(TOUS);
        comboType.getItems().setAll(TypeExercice.values());

        champRecherche.textProperty().addListener((o, a, n) -> filtrer());
        filtreType.valueProperty().addListener((o, a, n) -> filtrer());
        table.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            if (n != null) {
                remplir(n);
            }
        });

        charger();
        nouveau();
    }

    private void charger() {
        List<Exercice> exercices = service.exercices();
        liste = new FilteredList<>(FXCollections.observableArrayList(exercices));
        table.setItems(liste);
        // Les groupes déjà utilisés sont proposés dans la liste déroulante
        comboGroupe.getItems().setAll(exercices.stream().map(Exercice::getGroupeMusculaire)
                .distinct().sorted().toList());
        filtrer();
    }

    private void filtrer() {
        String texte = champRecherche.getText() == null ? "" : champRecherche.getText().toLowerCase().trim();
        String type = filtreType.getValue();
        liste.setPredicate(e ->
                (TOUS.equals(type) || e.getType().getLibelle().equals(type))
                        && (texte.isEmpty()
                        || e.getNom().toLowerCase().contains(texte)
                        || e.getGroupeMusculaire().toLowerCase().contains(texte)));
    }

    @FXML
    private void nouveau() {
        selection = null;
        table.getSelectionModel().clearSelection();
        titreFormulaire.setText("Nouvel exercice");
        champNom.clear();
        comboGroupe.setValue(null);
        comboGroupe.getEditor().clear();
        comboType.setValue(TypeExercice.FORCE);
        champDescription.clear();
        boutonSupprimer.setDisable(true);
    }

    private void remplir(Exercice e) {
        selection = e;
        titreFormulaire.setText("Modifier l'exercice");
        champNom.setText(e.getNom());
        comboGroupe.setValue(e.getGroupeMusculaire());
        comboType.setValue(e.getType());
        champDescription.setText(e.getDescription());
        boutonSupprimer.setDisable(false);
    }

    @FXML
    private void enregistrer() {
        try {
            Exercice e = selection != null ? selection : new Exercice();
            e.setNom(champNom.getText());
            // Dans une ComboBox éditable, le texte saisi est dans l'éditeur
            e.setGroupeMusculaire(comboGroupe.getEditor().getText());
            e.setType(comboType.getValue());
            e.setDescription(champDescription.getText());
            if (selection == null) {
                service.ajouter(e);
            } else {
                service.modifier(e);
            }
            charger();
            nouveau();
        } catch (RuntimeException ex) {
            Alertes.gerer(ex);
            charger();
        }
    }

    @FXML
    private void supprimer() {
        if (selection == null || !Alertes.confirmer("Supprimer l'exercice « " + selection.getNom() + " » ?")) {
            return;
        }
        try {
            service.supprimer(selection.getId());
            charger();
            nouveau();
        } catch (RuntimeException ex) {
            Alertes.gerer(ex);
        }
    }
}
