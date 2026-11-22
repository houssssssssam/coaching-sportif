package ma.emsi.coaching.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ma.emsi.coaching.model.Affectation;
import ma.emsi.coaching.model.ProfilSportif;
import ma.emsi.coaching.model.Programme;
import ma.emsi.coaching.model.Utilisateur;
import ma.emsi.coaching.service.NutritionService;
import ma.emsi.coaching.service.ObjectifNutritionnel;
import ma.emsi.coaching.service.ProgrammeService;
import ma.emsi.coaching.service.StatistiquesSportif;
import ma.emsi.coaching.service.SuiviService;
import ma.emsi.coaching.service.UtilisateurService;
import ma.emsi.coaching.service.ValidationException;
import ma.emsi.coaching.util.Alertes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Le coach consulte ses sportifs, leur progression, et leur affecte des programmes.
 */
public class CoachSportifsController {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Une ligne du tableau : le sportif et ses indicateurs principaux. */
    private record LigneSportif(Utilisateur sportif, ProfilSportif profil, Affectation actif,
                                StatistiquesSportif stats) {
    }

    @FXML private TextField champRecherche;
    @FXML private TableView<LigneSportif> table;
    @FXML private TableColumn<LigneSportif, String> colNom;
    @FXML private TableColumn<LigneSportif, String> colObjectif;
    @FXML private TableColumn<LigneSportif, String> colProgramme;
    @FXML private TableColumn<LigneSportif, String> colPoids;
    @FXML private TableColumn<LigneSportif, String> colAssiduite;

    @FXML private VBox panneauDetail;
    @FXML private Label titreDetail;
    @FXML private Label texteProfil;
    @FXML private Label texteNutrition;
    @FXML private Label texteSuivi;
    @FXML private ComboBox<Programme> comboProgramme;
    @FXML private DatePicker champDateDebut;
    @FXML private Button boutonAffecter;
    @FXML private Button boutonTerminer;
    @FXML private ListView<String> listeHistorique;

    private final ProgrammeService programmeService = new ProgrammeService();
    private final SuiviService suiviService = new SuiviService();
    private final NutritionService nutritionService = new NutritionService();
    private final UtilisateurService utilisateurService = new UtilisateurService();

    private FilteredList<LigneSportif> liste;
    private LigneSportif selection;

    @FXML
    private void initialize() {
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().sportif().getNomComplet()));
        colObjectif.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().profil() == null ? "" : c.getValue().profil().getObjectif().getLibelle()));
        colProgramme.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().actif() == null ? "Aucun" : c.getValue().actif().getProgramme().getNom()));
        colPoids.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().stats().poidsActuel() == null ? "-" : c.getValue().stats().poidsActuel() + " kg"));
        colAssiduite.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().stats().assiduitePct() == null ? "-" : c.getValue().stats().assiduitePct() + " %"));

        comboProgramme.setPromptText("Choisir un programme");
        champRecherche.textProperty().addListener((o, a, n) -> {
            String t = n == null ? "" : n.toLowerCase().trim();
            liste.setPredicate(l -> t.isEmpty() || l.sportif().getNomComplet().toLowerCase().contains(t));
        });
        table.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            if (n != null) {
                afficher(n);
            }
        });

        charger(null);
        panneauDetail.setDisable(true);
    }

    private void charger(Integer idASelectionner) {
        List<LigneSportif> lignes = new ArrayList<>();
        for (Utilisateur s : programmeService.sportifsSuivis()) {
            lignes.add(new LigneSportif(s,
                    utilisateurService.trouverProfil(s.getId()).orElse(null),
                    programmeService.programmeActif(s.getId()).orElse(null),
                    suiviService.statistiques(s.getId())));
        }
        liste = new FilteredList<>(FXCollections.observableArrayList(lignes));
        table.setItems(liste);
        comboProgramme.getItems().setAll(programmeService.mesProgrammes());
        if (idASelectionner != null) {
            lignes.stream().filter(l -> l.sportif().getId() == idASelectionner).findFirst()
                    .ifPresent(l -> table.getSelectionModel().select(l));
        }
    }

    private void afficher(LigneSportif l) {
        selection = l;
        panneauDetail.setDisable(false);
        Utilisateur s = l.sportif();
        titreDetail.setText(s.getNomComplet());

        ProfilSportif p = l.profil();
        texteProfil.setText(p == null ? "Profil non renseigné."
                : String.format("%s, %d ans, %s cm%nObjectif : %s%nActivité : %s",
                p.getSexe().getLibelle(), p.getAge(), p.getTailleCm(),
                NutritionService.decrire(p.getObjectif()), p.getNiveauActivite().getLibelle()));

        try {
            ObjectifNutritionnel o = nutritionService.objectifDe(s.getId());
            texteNutrition.setText(String.format(
                    "%.0f kcal par jour (maintien : %.0f kcal)%nProtéines %.0f g · Glucides %.0f g · Lipides %.0f g",
                    o.calories(), o.maintien(), o.proteinesG(), o.glucidesG(), o.lipidesG()));
        } catch (ValidationException e) {
            texteNutrition.setText(e.getMessage());
        }

        StatistiquesSportif st = l.stats();
        StringBuilder suivi = new StringBuilder();
        suivi.append("Séances réalisées : ").append(st.seancesRealisees());
        if (st.seancesPrevues() > 0) {
            suivi.append(" sur ").append(st.seancesPrevues()).append(" prévues (").append(st.assiduitePct()).append(" %)");
        }
        if (st.ressentiMoyen() != null) {
            suivi.append("\nRessenti moyen : ").append(st.ressentiMoyen()).append(" / 5");
        }
        if (st.poidsActuel() != null) {
            Double v = st.variationPoids();
            suivi.append("\nPoids : ").append(st.poidsDepart()).append(" → ").append(st.poidsActuel()).append(" kg (")
                    .append(v > 0 ? "+" : "").append(v).append(" kg)");
        }
        texteSuivi.setText(suivi.toString());

        comboProgramme.setValue(null);
        champDateDebut.setValue(LocalDate.now());
        boutonTerminer.setDisable(l.actif() == null);

        List<String> historique = new ArrayList<>();
        for (Affectation a : programmeService.historique(s.getId())) {
            historique.add(a.getProgramme().getNom() + " · " + a.getDateDebut().format(FORMAT)
                    + (a.getDateFin() == null ? " → en cours" : " → " + a.getDateFin().format(FORMAT)));
        }
        listeHistorique.getItems().setAll(historique);
    }

    @FXML
    private void affecter() {
        if (selection == null) {
            return;
        }
        Programme p = comboProgramme.getValue();
        if (p == null) {
            Alertes.erreur("Choisissez un programme.");
            return;
        }
        int id = selection.sportif().getId();
        // Avertissement non bloquant si l'objectif du programme ne correspond pas à celui du sportif
        if (!programmeService.estCompatible(id, p) && !Alertes.confirmer(
                "L'objectif de ce programme (" + p.getObjectif().getLibelle()
                        + ") ne correspond pas à celui du sportif. Continuer quand même ?")) {
            return;
        }
        String remplace = Optional.ofNullable(selection.actif())
                .map(a -> "\nLe programme actuel « " + a.getProgramme().getNom() + " » sera terminé.").orElse("");
        if (!Alertes.confirmer("Affecter « " + p.getNom() + " » à " + selection.sportif().getNomComplet()
                + " à partir du " + champDateDebut.getValue().format(FORMAT) + " ?" + remplace)) {
            return;
        }
        try {
            programmeService.affecter(id, p.getId(), champDateDebut.getValue());
            charger(id);
        } catch (RuntimeException e) {
            Alertes.gerer(e);
        }
    }

    @FXML
    private void terminer() {
        if (selection == null || selection.actif() == null) {
            return;
        }
        if (!Alertes.confirmer("Terminer le programme « " + selection.actif().getProgramme().getNom() + " » ?")) {
            return;
        }
        try {
            int id = selection.sportif().getId();
            programmeService.terminerProgramme(id);
            charger(id);
        } catch (RuntimeException e) {
            Alertes.gerer(e);
        }
    }
}
