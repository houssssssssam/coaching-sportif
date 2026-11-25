package ma.emsi.coaching.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import ma.emsi.coaching.model.Role;
import ma.emsi.coaching.model.Utilisateur;
import ma.emsi.coaching.service.BilanJournalier;
import ma.emsi.coaching.service.CatalogueService;
import ma.emsi.coaching.service.NutritionService;
import ma.emsi.coaching.service.ProgrammeService;
import ma.emsi.coaching.service.Session;
import ma.emsi.coaching.service.StatistiquesSportif;
import ma.emsi.coaching.service.SuiviService;
import ma.emsi.coaching.service.UtilisateurService;
import ma.emsi.coaching.service.ValidationException;

import java.time.LocalDate;

/**
 * Page d'accueil : quelques indicateurs selon le rôle.
 */
public class AccueilController {

    @FXML
    private Label titre;
    @FXML
    private FlowPane cartes;

    @FXML
    private void initialize() {
        Utilisateur u = Session.getUtilisateur();
        titre.setText("Bonjour " + u.getPrenom());
        switch (u.getRole()) {
            case ADMIN -> accueilAdmin();
            case COACH -> accueilCoach();
            case SPORTIF -> accueilSportif(u.getId());
        }
    }

    private void accueilAdmin() {
        UtilisateurService us = new UtilisateurService();
        ajouterCarte("Coachs", String.valueOf(us.listerParRole(Role.COACH).size()));
        ajouterCarte("Sportifs", String.valueOf(us.listerParRole(Role.SPORTIF).size()));
        ajouterCarte("Sportifs sans coach", String.valueOf(us.listerSansCoach().size()));
        ajouterCarte("Exercices", String.valueOf(new CatalogueService().exercices().size()));
        ajouterCarte("Aliments", String.valueOf(new NutritionService().catalogue().size()));
    }

    private void accueilCoach() {
        ProgrammeService ps = new ProgrammeService();
        ajouterCarte("Sportifs suivis", String.valueOf(ps.sportifsSuivis().size()));
        ajouterCarte("Programmes créés", String.valueOf(ps.mesProgrammes().size()));
    }

    private void accueilSportif(int id) {
        new ProgrammeService().programmeActif(id).ifPresentOrElse(
                a -> ajouterCarte("Programme actif", a.getProgramme().getNom()),
                () -> ajouterCarte("Programme actif", "Aucun"));

        StatistiquesSportif st = new SuiviService().statistiques(id);
        ajouterCarte("Séances (4 semaines)", String.valueOf(st.seancesRealisees()));
        if (st.assiduitePct() != null) {
            ajouterCarte("Assiduité", st.assiduitePct() + " %");
        }
        if (st.poidsActuel() != null) {
            ajouterCarte("Poids actuel", st.poidsActuel() + " kg");
        }
        try {
            BilanJournalier b = new NutritionService().bilanDuJour(id, LocalDate.now());
            ajouterCarte("Calories du jour", String.format("%.0f / %.0f kcal", b.calories(), b.objectif().calories()));
            ajouterCarte("Bilan du jour", b.statut().getLibelle());
        } catch (ValidationException e) {
            ajouterCarte("Calories du jour", "Ajoutez une pesée");
        }
    }

    private void ajouterCarte(String libelle, String valeur) {
        Label l = new Label(libelle);
        l.getStyleClass().add("carte-libelle");
        Label v = new Label(valeur);
        v.getStyleClass().add("carte-valeur");
        VBox carte = new VBox(6, l, v);
        carte.getStyleClass().add("carte");
        cartes.getChildren().add(carte);
    }
}
