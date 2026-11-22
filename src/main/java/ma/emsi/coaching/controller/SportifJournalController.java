package ma.emsi.coaching.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ma.emsi.coaching.model.Aliment;
import ma.emsi.coaching.model.Repas;
import ma.emsi.coaching.model.TypeRepas;
import ma.emsi.coaching.service.BilanJournalier;
import ma.emsi.coaching.service.NutritionService;
import ma.emsi.coaching.service.Session;
import ma.emsi.coaching.service.ValidationException;
import ma.emsi.coaching.util.Alertes;
import ma.emsi.coaching.util.Formulaires;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Journal alimentaire façon MyFitnessPal : recherche d'aliments, quantité, bilan du jour.
 */
public class SportifJournalController {

    @FXML private DatePicker champJour;
    @FXML private Label texteCalories;
    @FXML private ProgressBar barreCalories;
    @FXML private Label texteStatut;
    @FXML private Label texteProteines;
    @FXML private Label texteGlucides;
    @FXML private Label texteLipides;

    @FXML private TableView<Repas> table;
    @FXML private TableColumn<Repas, String> colRepas;
    @FXML private TableColumn<Repas, String> colAliment;
    @FXML private TableColumn<Repas, String> colQuantite;
    @FXML private TableColumn<Repas, String> colKcal;
    @FXML private TableColumn<Repas, String> colP;
    @FXML private TableColumn<Repas, String> colG;
    @FXML private TableColumn<Repas, String> colL;
    @FXML private Button boutonSupprimer;

    @FXML private TextField champRecherche;
    @FXML private ListView<Aliment> listeResultats;
    @FXML private Label alimentChoisi;
    @FXML private TextField champQuantite;
    @FXML private ComboBox<TypeRepas> comboTypeRepas;
    @FXML private Label apercu;

    @FXML private TextField persoNom;
    @FXML private TextField persoKcal;
    @FXML private TextField persoProteines;
    @FXML private TextField persoGlucides;
    @FXML private TextField persoLipides;

    private final NutritionService service = new NutritionService();

    @FXML
    private void initialize() {
        colRepas.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTypeRepas().getLibelle()));
        colAliment.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAliment().getNom()));
        colQuantite.setCellValueFactory(c -> new SimpleStringProperty(Formulaires.texte(c.getValue().getQuantiteG()) + " g"));
        colKcal.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.0f", c.getValue().getCalories())));
        colP.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.1f", c.getValue().getProteines())));
        colG.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.1f", c.getValue().getGlucides())));
        colL.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.1f", c.getValue().getLipides())));
        boutonSupprimer.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        // Résultats de recherche : nom + calories pour 100 g, "(perso)" pour les aliments personnalisés
        listeResultats.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(Aliment a, boolean vide) {
                super.updateItem(a, vide);
                setText(vide || a == null ? null : a.getNom() + (a.isPersonnalise() ? " (perso)" : "")
                        + " · " + Formulaires.texte(a.getCalories100g()) + " kcal/100 g");
            }
        });
        champRecherche.textProperty().addListener((o, a, n) -> rechercher(n));
        listeResultats.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> majApercu());
        champQuantite.textProperty().addListener((o, a, n) -> majApercu());

        comboTypeRepas.getItems().setAll(TypeRepas.values());
        comboTypeRepas.setValue(repasSelonHeure());
        champJour.setValue(LocalDate.now());
        champJour.valueProperty().addListener((o, a, n) -> charger());
        charger();
    }

    /** Propose le type de repas selon l'heure actuelle. */
    private static TypeRepas repasSelonHeure() {
        int h = LocalTime.now().getHour();
        if (h < 11) {
            return TypeRepas.PETIT_DEJEUNER;
        }
        if (h < 15) {
            return TypeRepas.DEJEUNER;
        }
        return h < 18 ? TypeRepas.COLLATION : TypeRepas.DINER;
    }

    private void charger() {
        LocalDate jour = champJour.getValue();
        int id = Session.getUtilisateur().getId();
        try {
            BilanJournalier b = service.bilanDuJour(id, jour);
            table.getItems().setAll(b.repas());
            texteCalories.setText(String.format("%.0f / %.0f kcal", b.calories(), b.objectif().calories()));
            barreCalories.setProgress(Math.min(1.0, b.pourcentage() / 100));
            String restant = b.restant() >= 0
                    ? String.format("reste %.0f kcal", b.restant())
                    : String.format("dépassement de %.0f kcal", -b.restant());
            texteStatut.setText(b.statut().getLibelle() + " · " + restant);
            texteStatut.getStyleClass().removeAll("statut-ok", "statut-alerte");
            texteStatut.getStyleClass().add(
                    b.statut() == BilanJournalier.Statut.DANS_OBJECTIF ? "statut-ok" : "statut-alerte");
            texteProteines.setText(String.format("%.0f / %.0f g", b.proteinesG(), b.objectif().proteinesG()));
            texteGlucides.setText(String.format("%.0f / %.0f g", b.glucidesG(), b.objectif().glucidesG()));
            texteLipides.setText(String.format("%.0f / %.0f g", b.lipidesG(), b.objectif().lipidesG()));
        } catch (ValidationException e) {
            // Pas de pesée : on ne peut pas calculer l'objectif, mais on affiche quand même le journal
            texteCalories.setText("Objectif indisponible");
            texteStatut.setText(e.getMessage());
            barreCalories.setProgress(0);
            texteProteines.setText("-");
            texteGlucides.setText("-");
            texteLipides.setText("-");
        }
    }

    private void rechercher(String texte) {
        try {
            listeResultats.getItems().setAll(service.rechercherAliments(texte));
        } catch (RuntimeException e) {
            Alertes.gerer(e);
        }
    }

    private void majApercu() {
        Aliment a = listeResultats.getSelectionModel().getSelectedItem();
        if (a == null) {
            alimentChoisi.setText("Sélectionnez un aliment dans la liste.");
            apercu.setText("");
            return;
        }
        alimentChoisi.setText(String.format("%s : %s kcal · P %s g · G %s g · L %s g (pour 100 g)",
                a.getNom(), Formulaires.texte(a.getCalories100g()), Formulaires.texte(a.getProteines100g()),
                Formulaires.texte(a.getGlucides100g()), Formulaires.texte(a.getLipides100g())));
        try {
            double q = Formulaires.lireDouble(champQuantite, "Quantité");
            apercu.setText(String.format("Pour %s g : %.0f kcal", Formulaires.texte(q), a.getCalories100g() * q / 100));
        } catch (RuntimeException e) {
            apercu.setText("");
        }
    }

    @FXML
    private void ajouter() {
        Aliment a = listeResultats.getSelectionModel().getSelectedItem();
        if (a == null) {
            Alertes.erreur("Recherchez puis sélectionnez un aliment.");
            return;
        }
        try {
            Repas r = new Repas();
            r.setAliment(a);
            r.setDateRepas(champJour.getValue());
            r.setTypeRepas(comboTypeRepas.getValue());
            r.setQuantiteG(Formulaires.lireDouble(champQuantite, "Quantité"));
            service.ajouterAuJournal(r);
            champQuantite.clear();
            charger();
        } catch (RuntimeException e) {
            Alertes.gerer(e);
        }
    }

    @FXML
    private void supprimer() {
        Repas r = table.getSelectionModel().getSelectedItem();
        if (r == null || !Alertes.confirmer("Retirer « " + r.getAliment().getNom() + " » du journal ?")) {
            return;
        }
        try {
            service.supprimerRepas(r.getId());
            charger();
        } catch (RuntimeException e) {
            Alertes.gerer(e);
        }
    }

    @FXML
    private void creerAliment() {
        try {
            Aliment a = new Aliment();
            a.setNom(persoNom.getText());
            a.setCalories100g(Formulaires.lireDouble(persoKcal, "kcal"));
            a.setProteines100g(Formulaires.lireDouble(persoProteines, "Protéines"));
            a.setGlucides100g(Formulaires.lireDouble(persoGlucides, "Glucides"));
            a.setLipides100g(Formulaires.lireDouble(persoLipides, "Lipides"));
            service.ajouterAliment(a);
            for (TextField t : new TextField[]{persoNom, persoKcal, persoProteines, persoGlucides, persoLipides}) {
                t.clear();
            }
            // On affiche directement le nouvel aliment dans les résultats
            champRecherche.setText(a.getNom());
            listeResultats.getItems().stream().filter(x -> x.getId() == a.getId()).findFirst()
                    .ifPresent(x -> listeResultats.getSelectionModel().select(x));
            Alertes.info("Aliment « " + a.getNom() + " » créé. Indiquez la quantité puis ajoutez-le.");
        } catch (RuntimeException e) {
            Alertes.gerer(e);
        }
    }

    @FXML
    private void jourPrecedent() {
        champJour.setValue(champJour.getValue().minusDays(1));
    }

    @FXML
    private void jourSuivant() {
        champJour.setValue(champJour.getValue().plusDays(1));
    }

    @FXML
    private void aujourdHui() {
        champJour.setValue(LocalDate.now());
    }
}
