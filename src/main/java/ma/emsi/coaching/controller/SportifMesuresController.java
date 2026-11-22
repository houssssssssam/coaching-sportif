package ma.emsi.coaching.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ma.emsi.coaching.model.Mesure;
import ma.emsi.coaching.service.Session;
import ma.emsi.coaching.service.SuiviService;
import ma.emsi.coaching.service.VariationSuspecteException;
import ma.emsi.coaching.util.Alertes;
import ma.emsi.coaching.util.Formulaires;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Suivi des mesures corporelles (poids, tour de taille, masse grasse).
 */
public class SportifMesuresController {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private TableView<Mesure> table;
    @FXML private TableColumn<Mesure, String> colDate;
    @FXML private TableColumn<Mesure, String> colPoids;
    @FXML private TableColumn<Mesure, String> colEcart;
    @FXML private TableColumn<Mesure, String> colTaille;
    @FXML private TableColumn<Mesure, String> colGras;
    @FXML private Label resume;

    @FXML private Label titreFormulaire;
    @FXML private DatePicker champDate;
    @FXML private TextField champPoids;
    @FXML private TextField champTaille;
    @FXML private TextField champGras;
    @FXML private Button boutonSupprimer;

    private final SuiviService service = new SuiviService();
    /** Écart de poids par rapport à la mesure précédente, par identifiant de mesure. */
    private final Map<Integer, Double> ecarts = new HashMap<>();
    private Mesure selection;

    @FXML
    private void initialize() {
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateMesure().format(FORMAT)));
        colPoids.setCellValueFactory(c -> new SimpleStringProperty(Formulaires.texte(c.getValue().getPoidsKg())));
        colEcart.setCellValueFactory(c -> {
            Double e = ecarts.get(c.getValue().getId());
            return new SimpleStringProperty(e == null ? "" : (e > 0 ? "+" : "") + e);
        });
        colTaille.setCellValueFactory(c -> new SimpleStringProperty(Formulaires.texte(c.getValue().getTourTailleCm())));
        colGras.setCellValueFactory(c -> new SimpleStringProperty(Formulaires.texte(c.getValue().getMasseGrassePct())));

        table.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            if (n != null) {
                remplir(n);
            }
        });
        charger();
        nouvelle();
    }

    private void charger() {
        List<Mesure> mesures = service.mesures(Session.getUtilisateur().getId()); // de la plus ancienne à la plus récente
        ecarts.clear();
        for (int i = 1; i < mesures.size(); i++) {
            double e = mesures.get(i).getPoidsKg() - mesures.get(i - 1).getPoidsKg();
            ecarts.put(mesures.get(i).getId(), Math.round(e * 10) / 10.0);
        }
        List<Mesure> affichage = new ArrayList<>(mesures);
        Collections.reverse(affichage); // la plus récente en haut
        table.getItems().setAll(affichage);
        if (mesures.size() >= 2) {
            double total = mesures.get(mesures.size() - 1).getPoidsKg() - mesures.get(0).getPoidsKg();
            resume.setText(String.format("Évolution depuis le %s : %+.1f kg",
                    mesures.get(0).getDateMesure().format(FORMAT), total));
        } else {
            resume.setText("");
        }
    }

    @FXML
    private void nouvelle() {
        selection = null;
        table.getSelectionModel().clearSelection();
        titreFormulaire.setText("Nouvelle mesure");
        champDate.setValue(LocalDate.now());
        champPoids.clear();
        champTaille.clear();
        champGras.clear();
        boutonSupprimer.setDisable(true);
    }

    private void remplir(Mesure m) {
        selection = m;
        titreFormulaire.setText("Modifier la mesure");
        champDate.setValue(m.getDateMesure());
        champPoids.setText(Formulaires.texte(m.getPoidsKg()));
        champTaille.setText(Formulaires.texte(m.getTourTailleCm()));
        champGras.setText(Formulaires.texte(m.getMasseGrassePct()));
        boutonSupprimer.setDisable(false);
    }

    @FXML
    private void enregistrer() {
        try {
            Mesure m = selection != null ? selection : new Mesure();
            m.setDateMesure(champDate.getValue());
            m.setPoidsKg(Formulaires.lireDouble(champPoids, "Poids"));
            m.setTourTailleCm(Formulaires.lireDoubleOptionnel(champTaille, "Tour de taille"));
            m.setMasseGrassePct(Formulaires.lireDoubleOptionnel(champGras, "Masse grasse"));
            if (selection == null) {
                try {
                    service.enregistrerMesure(m, false);
                } catch (VariationSuspecteException e) {
                    // Règle métier : grosse variation en peu de temps -> on demande confirmation
                    if (!Alertes.confirmer(e.getMessage())) {
                        return;
                    }
                    service.enregistrerMesure(m, true);
                }
            } else {
                service.modifierMesure(m);
            }
            charger();
            nouvelle();
        } catch (RuntimeException e) {
            Alertes.gerer(e);
            charger();
        }
    }

    @FXML
    private void supprimer() {
        if (selection == null || !Alertes.confirmer("Supprimer la mesure du "
                + selection.getDateMesure().format(FORMAT) + " ?")) {
            return;
        }
        try {
            service.supprimerMesure(selection.getId());
            charger();
            nouvelle();
        } catch (RuntimeException e) {
            Alertes.gerer(e);
        }
    }
}
