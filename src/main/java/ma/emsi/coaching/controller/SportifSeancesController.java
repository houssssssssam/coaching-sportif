package ma.emsi.coaching.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import ma.emsi.coaching.model.Seance;
import ma.emsi.coaching.service.Session;
import ma.emsi.coaching.service.StatistiquesSportif;
import ma.emsi.coaching.service.SuiviService;
import ma.emsi.coaching.util.Alertes;
import ma.emsi.coaching.util.Formulaires;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.IntStream;

/**
 * Journal des séances d'entraînement du sportif.
 */
public class SportifSeancesController {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private TableView<Seance> table;
    @FXML private TableColumn<Seance, String> colDate;
    @FXML private TableColumn<Seance, String> colDuree;
    @FXML private TableColumn<Seance, String> colRessenti;
    @FXML private TableColumn<Seance, String> colCommentaire;
    @FXML private Label resume;

    @FXML private Label titreFormulaire;
    @FXML private DatePicker champDate;
    @FXML private TextField champDuree;
    @FXML private ComboBox<Integer> comboRessenti;
    @FXML private TextArea champCommentaire;
    @FXML private Button boutonSupprimer;

    private final SuiviService service = new SuiviService();
    private Seance selection;

    @FXML
    private void initialize() {
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateSeance().format(FORMAT)));
        colDuree.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDureeMin() + " min"));
        colRessenti.setCellValueFactory(c -> new SimpleStringProperty(etoiles(c.getValue().getRessenti())));
        colCommentaire.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCommentaire() == null ? "" : c.getValue().getCommentaire()));
        comboRessenti.getItems().setAll(IntStream.rangeClosed(1, 5).boxed().toList());

        table.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            if (n != null) {
                remplir(n);
            }
        });
        charger();
        nouvelle();
    }

    private void charger() {
        int id = Session.getUtilisateur().getId();
        table.getItems().setAll(service.seances(id));
        StatistiquesSportif st = service.statistiques(id);
        String texte = st.seancesRealisees() + " séance(s) sur les 4 dernières semaines";
        if (st.assiduitePct() != null) {
            texte += " · assiduité " + st.assiduitePct() + " % (" + st.seancesPrevues() + " prévues)";
        }
        resume.setText(texte);
    }

    @FXML
    private void nouvelle() {
        selection = null;
        table.getSelectionModel().clearSelection();
        titreFormulaire.setText("Nouvelle séance");
        champDate.setValue(LocalDate.now());
        champDuree.clear();
        comboRessenti.setValue(4);
        champCommentaire.clear();
        boutonSupprimer.setDisable(true);
    }

    private void remplir(Seance s) {
        selection = s;
        titreFormulaire.setText("Modifier la séance");
        champDate.setValue(s.getDateSeance());
        champDuree.setText(String.valueOf(s.getDureeMin()));
        comboRessenti.setValue(s.getRessenti());
        champCommentaire.setText(s.getCommentaire() == null ? "" : s.getCommentaire());
        boutonSupprimer.setDisable(false);
    }

    @FXML
    private void enregistrer() {
        try {
            Seance s = selection != null ? selection : new Seance();
            s.setDateSeance(champDate.getValue());
            s.setDureeMin(Formulaires.lireEntier(champDuree, "Durée"));
            s.setRessenti(comboRessenti.getValue() == null ? 0 : comboRessenti.getValue());
            s.setCommentaire(champCommentaire.getText().isBlank() ? null : champCommentaire.getText().trim());
            if (selection == null) {
                service.enregistrerSeance(s);
            } else {
                service.modifierSeance(s);
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
        if (selection == null || !Alertes.confirmer("Supprimer la séance du "
                + selection.getDateSeance().format(FORMAT) + " ?")) {
            return;
        }
        try {
            service.supprimerSeance(selection.getId());
            charger();
            nouvelle();
        } catch (RuntimeException e) {
            Alertes.gerer(e);
        }
    }

    private static String etoiles(int n) {
        return "★".repeat(n) + "☆".repeat(5 - n);
    }
}
