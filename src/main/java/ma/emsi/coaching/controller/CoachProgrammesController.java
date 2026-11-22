package ma.emsi.coaching.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import ma.emsi.coaching.model.Exercice;
import ma.emsi.coaching.model.NiveauProgramme;
import ma.emsi.coaching.model.Objectif;
import ma.emsi.coaching.model.Programme;
import ma.emsi.coaching.model.ProgrammeExercice;
import ma.emsi.coaching.model.TypeExercice;
import ma.emsi.coaching.service.CatalogueService;
import ma.emsi.coaching.service.ProgrammeService;
import ma.emsi.coaching.util.Alertes;
import ma.emsi.coaching.util.Formulaires;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Le coach crée ses programmes et définit leur contenu (exercices par jour).
 */
public class CoachProgrammesController {

    @FXML private ListView<Programme> listeProgrammes;
    @FXML private TabPane onglets;
    @FXML private Tab ongletExercices;

    @FXML private Label titreProgramme;
    @FXML private TextField champNom;
    @FXML private TextArea champDescription;
    @FXML private ComboBox<Objectif> comboObjectif;
    @FXML private ComboBox<NiveauProgramme> comboNiveau;
    @FXML private Button boutonSupprimerProgramme;

    @FXML private TableView<ProgrammeExercice> tableLignes;
    @FXML private TableColumn<ProgrammeExercice, String> colJour;
    @FXML private TableColumn<ProgrammeExercice, String> colExercice;
    @FXML private TableColumn<ProgrammeExercice, String> colDetail;
    @FXML private TableColumn<ProgrammeExercice, String> colConsigne;
    @FXML private Label resumeProgramme;

    @FXML private Label titreLigne;
    @FXML private ComboBox<Exercice> comboExercice;
    @FXML private ComboBox<Integer> comboJour;
    @FXML private TextField champOrdre;
    @FXML private Label labelSeries;
    @FXML private TextField champSeries;
    @FXML private Label labelReps;
    @FXML private TextField champReps;
    @FXML private Label labelCharge;
    @FXML private TextField champCharge;
    @FXML private Label labelDuree;
    @FXML private TextField champDuree;
    @FXML private TextField champConsigne;
    @FXML private Button boutonSupprimerLigne;

    private final ProgrammeService service = new ProgrammeService();
    private Programme programme;          // programme sélectionné (null = nouveau)
    private ProgrammeExercice ligne;      // ligne sélectionnée (null = nouvelle)

    @FXML
    private void initialize() {
        comboObjectif.getItems().setAll(Objectif.values());
        comboNiveau.getItems().setAll(NiveauProgramme.values());
        comboExercice.getItems().setAll(new CatalogueService().exercices());
        comboJour.getItems().setAll(IntStream.rangeClosed(1, 7).boxed().toList());

        listeProgrammes.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(Programme p, boolean vide) {
                super.updateItem(p, vide);
                setText(vide || p == null ? null
                        : p.getNom());
            }
        });
        listeProgrammes.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            if (n != null) {
                afficherProgramme(n);
            }
        });

        colJour.setCellValueFactory(c -> new SimpleStringProperty("J" + c.getValue().getJour()));
        colExercice.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getExercice().getNom()));
        colDetail.setCellValueFactory(c -> new SimpleStringProperty(detail(c.getValue())));
        colConsigne.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getConsigne() == null ? "" : c.getValue().getConsigne()));
        tableLignes.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            if (n != null) {
                afficherLigne(n);
            }
        });

        // Les champs proposés dépendent du type d'exercice (force : séries/reps, cardio : durée)
        comboExercice.valueProperty().addListener((o, a, n) -> adapterChamps(n));

        chargerProgrammes(null);
        nouveauProgramme();
    }

    // ---------------- Programmes ----------------

    private void chargerProgrammes(Integer idASelectionner) {
        List<Programme> programmes = service.mesProgrammes();
        listeProgrammes.getItems().setAll(programmes);
        if (idASelectionner != null) {
            programmes.stream().filter(p -> p.getId() == idASelectionner).findFirst()
                    .ifPresent(p -> listeProgrammes.getSelectionModel().select(p));
        }
    }

    @FXML
    private void nouveauProgramme() {
        programme = null;
        listeProgrammes.getSelectionModel().clearSelection();
        titreProgramme.setText("Nouveau programme");
        champNom.clear();
        champDescription.clear();
        comboObjectif.setValue(null);
        comboNiveau.setValue(NiveauProgramme.DEBUTANT);
        boutonSupprimerProgramme.setDisable(true);
        ongletExercices.setDisable(true); // il faut d'abord enregistrer le programme
        onglets.getSelectionModel().selectFirst();
        tableLignes.getItems().clear();
    }

    private void afficherProgramme(Programme p) {
        programme = p;
        titreProgramme.setText(p.getNom());
        champNom.setText(p.getNom());
        champDescription.setText(p.getDescription());
        comboObjectif.setValue(p.getObjectif());
        comboNiveau.setValue(p.getNiveau());
        boutonSupprimerProgramme.setDisable(false);
        ongletExercices.setDisable(false);
        chargerLignes();
        nouvelleLigne();
    }

    @FXML
    private void enregistrerProgramme() {
        try {
            Programme p = programme != null ? programme : new Programme();
            p.setNom(champNom.getText());
            p.setDescription(champDescription.getText());
            p.setObjectif(comboObjectif.getValue());
            p.setNiveau(comboNiveau.getValue());
            boolean creation = programme == null;
            if (creation) {
                service.creer(p);
            } else {
                service.modifier(p);
            }
            chargerProgrammes(p.getId());
            if (creation) {
                // On enchaîne directement sur l'ajout des exercices
                onglets.getSelectionModel().select(ongletExercices);
            }
        } catch (RuntimeException e) {
            Alertes.gerer(e);
            chargerProgrammes(programme == null ? null : programme.getId());
        }
    }

    @FXML
    private void supprimerProgramme() {
        if (programme == null || !Alertes.confirmer("Supprimer le programme « " + programme.getNom()
                + " » et tout son contenu ?")) {
            return;
        }
        try {
            service.supprimer(programme.getId());
            chargerProgrammes(null);
            nouveauProgramme();
        } catch (RuntimeException e) {
            Alertes.gerer(e);
        }
    }

    // ---------------- Lignes du programme ----------------

    private void chargerLignes() {
        List<ProgrammeExercice> lignes = service.contenu(programme.getId());
        tableLignes.getItems().setAll(lignes);
        long jours = lignes.stream().map(ProgrammeExercice::getJour).distinct().count();
        resumeProgramme.setText(lignes.size() + " exercice(s) répartis sur " + jours + " jour(s) par semaine.");
    }

    @FXML
    private void nouvelleLigne() {
        ligne = null;
        tableLignes.getSelectionModel().clearSelection();
        titreLigne.setText("Ajouter un exercice");
        comboExercice.setValue(null);
        comboJour.setValue(1);
        champOrdre.setText(String.valueOf(tableLignes.getItems().size() + 1));
        champSeries.clear();
        champReps.clear();
        champCharge.clear();
        champDuree.clear();
        champConsigne.clear();
        boutonSupprimerLigne.setDisable(true);
    }

    private void afficherLigne(ProgrammeExercice pe) {
        ligne = pe;
        titreLigne.setText("Modifier la ligne");
        comboExercice.getItems().stream().filter(e -> e.getId() == pe.getExercice().getId())
                .findFirst().ifPresent(comboExercice::setValue);
        comboJour.setValue(pe.getJour());
        champOrdre.setText(String.valueOf(pe.getOrdre()));
        champSeries.setText(Formulaires.texte(pe.getSeries()));
        champReps.setText(Formulaires.texte(pe.getRepetitions()));
        champCharge.setText(Formulaires.texte(pe.getChargeKg()));
        champDuree.setText(Formulaires.texte(pe.getDureeMin()));
        champConsigne.setText(pe.getConsigne() == null ? "" : pe.getConsigne());
        boutonSupprimerLigne.setDisable(false);
    }

    private void adapterChamps(Exercice e) {
        boolean force = e == null || e.getType() == TypeExercice.FORCE;
        for (var n : List.of(labelSeries, champSeries, labelReps, champReps, labelCharge, champCharge)) {
            n.setDisable(!force);
        }
        labelDuree.setDisable(force);
        champDuree.setDisable(force);
    }

    @FXML
    private void enregistrerLigne() {
        if (programme == null) {
            return;
        }
        try {
            ProgrammeExercice pe = ligne != null ? ligne : new ProgrammeExercice();
            pe.setProgrammeId(programme.getId());
            pe.setExercice(comboExercice.getValue());
            pe.setJour(comboJour.getValue() == null ? 0 : comboJour.getValue());
            pe.setOrdre(Formulaires.lireEntier(champOrdre, "Ordre"));
            boolean force = pe.getExercice() == null || pe.getExercice().getType() == TypeExercice.FORCE;
            pe.setSeries(force ? Formulaires.lireEntierOptionnel(champSeries, "Séries") : null);
            pe.setRepetitions(force ? Formulaires.lireEntierOptionnel(champReps, "Répétitions") : null);
            pe.setChargeKg(force ? Formulaires.lireDoubleOptionnel(champCharge, "Charge") : null);
            pe.setDureeMin(force ? null : Formulaires.lireEntierOptionnel(champDuree, "Durée"));
            pe.setConsigne(champConsigne.getText().isBlank() ? null : champConsigne.getText().trim());
            if (ligne == null) {
                service.ajouterLigne(pe);
            } else {
                service.modifierLigne(pe);
            }
            chargerLignes();
            nouvelleLigne();
        } catch (RuntimeException e) {
            Alertes.gerer(e);
            chargerLignes();
        }
    }

    @FXML
    private void supprimerLigne() {
        if (ligne == null || !Alertes.confirmer("Retirer « " + ligne.getExercice().getNom() + " » du programme ?")) {
            return;
        }
        try {
            service.supprimerLigne(programme.getId(), ligne.getId());
            chargerLignes();
            nouvelleLigne();
        } catch (RuntimeException e) {
            Alertes.gerer(e);
        }
    }

    /** Texte du type "4 x 8 à 60 kg" ou "20 min". */
    private static String detail(ProgrammeExercice pe) {
        if (pe.getDureeMin() != null) {
            return pe.getDureeMin() + " min";
        }
        StringBuilder sb = new StringBuilder();
        if (pe.getSeries() != null) {
            sb.append(pe.getSeries()).append(" séries");
        }
        if (pe.getRepetitions() != null) {
            sb.setLength(0);
            sb.append(pe.getSeries()).append(" x ").append(pe.getRepetitions());
        }
        if (pe.getChargeKg() != null) {
            sb.append(" à ").append(Formulaires.texte(pe.getChargeKg())).append(" kg");
        }
        return sb.toString();
    }
}
