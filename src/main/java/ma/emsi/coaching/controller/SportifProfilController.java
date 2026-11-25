package ma.emsi.coaching.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ma.emsi.coaching.model.Mesure;
import ma.emsi.coaching.model.NiveauActivite;
import ma.emsi.coaching.model.Objectif;
import ma.emsi.coaching.model.ProfilSportif;
import ma.emsi.coaching.model.Sexe;
import ma.emsi.coaching.service.AuthService;
import ma.emsi.coaching.service.NutritionService;
import ma.emsi.coaching.service.ObjectifNutritionnel;
import ma.emsi.coaching.service.Session;
import ma.emsi.coaching.service.SuiviService;
import ma.emsi.coaching.service.UtilisateurService;
import ma.emsi.coaching.service.VariationSuspecteException;
import ma.emsi.coaching.util.Alertes;
import ma.emsi.coaching.util.Formulaires;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Le sportif met à jour son profil physique et voit son objectif nutritionnel recalculé.
 */
public class SportifProfilController {

    @FXML private Label texteCoach;
    @FXML private ComboBox<Sexe> comboSexe;
    @FXML private DatePicker champNaissance;
    @FXML private TextField champTaille;
    @FXML private ComboBox<Objectif> comboObjectif;
    @FXML private ComboBox<NiveauActivite> comboActivite;
    @FXML private Label texteObjectif;
    @FXML private Label texteCalcul;
    @FXML private Label textePoids;
    @FXML private TextField champPoids;
    @FXML private PasswordField champAncien;
    @FXML private PasswordField champNouveau;
    @FXML private PasswordField champConfirmation;

    private final UtilisateurService utilisateurService = new UtilisateurService();
    private final SuiviService suiviService = new SuiviService();
    private ProfilSportif profil;
    private Double dernierPoids;
    private Mesure derniereMesure;

    @FXML
    private void initialize() {
        comboSexe.getItems().setAll(Sexe.values());
        comboObjectif.getItems().setAll(Objectif.values());
        comboActivite.getItems().setAll(NiveauActivite.values());
        charger();

        // Aperçu en direct : l'objectif est recalculé dès qu'un champ change, avant l'enregistrement
        comboSexe.valueProperty().addListener((o, a, n) -> apercu());
        champNaissance.valueProperty().addListener((o, a, n) -> apercu());
        champTaille.textProperty().addListener((o, a, n) -> apercu());
        comboObjectif.valueProperty().addListener((o, a, n) -> apercu());
        comboActivite.valueProperty().addListener((o, a, n) -> apercu());
    }

    private void charger() {
        int id = Session.getUtilisateur().getId();
        profil = utilisateurService.trouverProfil(id).orElse(null);
        if (profil == null) {
            texteObjectif.setText("Profil introuvable : contactez l'administrateur.");
            return;
        }
        texteCoach.setText(utilisateurService.coachDe(id)
                .map(c -> "Coach : " + c.getNomComplet()).orElse("Aucun coach assigné"));
        comboSexe.setValue(profil.getSexe());
        champNaissance.setValue(profil.getDateNaissance());
        champTaille.setText(Formulaires.texte(profil.getTailleCm()));
        comboObjectif.setValue(profil.getObjectif());
        comboActivite.setValue(profil.getNiveauActivite());
        List<Mesure> mesures = suiviService.mesures(id);
        derniereMesure = mesures.isEmpty() ? null : mesures.get(mesures.size() - 1);
        dernierPoids = derniereMesure == null ? null : derniereMesure.getPoidsKg();
        textePoids.setText(derniereMesure == null ? "Aucune pesée enregistrée."
                : "Dernière pesée : " + Formulaires.texte(derniereMesure.getPoidsKg()) + " kg, le "
                + derniereMesure.getDateMesure().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        afficherObjectif(profil, false);
    }

    /** Recalcule l'objectif à partir des valeurs saisies, sans rien enregistrer. */
    private void apercu() {
        if (profil == null) {
            return;
        }
        try {
            ProfilSportif saisi = new ProfilSportif();
            saisi.setSexe(comboSexe.getValue());
            saisi.setDateNaissance(champNaissance.getValue());
            saisi.setTailleCm(Formulaires.lireDouble(champTaille, "Taille"));
            saisi.setObjectif(comboObjectif.getValue());
            saisi.setNiveauActivite(comboActivite.getValue());
            if (saisi.getSexe() == null || saisi.getDateNaissance() == null
                    || saisi.getObjectif() == null || saisi.getNiveauActivite() == null) {
                return;
            }
            boolean modifie = saisi.getSexe() != profil.getSexe()
                    || !saisi.getDateNaissance().equals(profil.getDateNaissance())
                    || saisi.getTailleCm() != profil.getTailleCm()
                    || saisi.getObjectif() != profil.getObjectif()
                    || saisi.getNiveauActivite() != profil.getNiveauActivite();
            afficherObjectif(saisi, modifie);
        } catch (RuntimeException e) {
            // saisie incomplète (ex. taille en cours de frappe) : on garde l'affichage actuel
        }
    }

    private void afficherObjectif(ProfilSportif p, boolean apercu) {
        if (dernierPoids == null) {
            texteObjectif.setText("Aucune pesée enregistrée : ajoutez une mesure pour calculer l'objectif.");
            texteCalcul.setText("");
            return;
        }
        ObjectifNutritionnel o = NutritionService.calculerObjectif(p, dernierPoids);
        texteObjectif.setText(String.format("%.0f kcal par jour%nProtéines %.0f g · Glucides %.0f g · Lipides %.0f g",
                o.calories(), o.proteinesG(), o.glucidesG(), o.lipidesG()));
        texteCalcul.setText(String.format(
                "Métabolisme de base (Mifflin-St Jeor) : %.0f kcal%nx facteur d'activité (%s) = %.0f kcal "
                        + "de maintien%nAjustement « %s » : %+d kcal%s",
                o.metabolismeBase(), p.getNiveauActivite().getLibelle(), o.maintien(),
                p.getObjectif().getLibelle(), p.getObjectif().getAjustementKcal(),
                apercu ? "\n\nAperçu : cliquez sur Enregistrer pour appliquer ces valeurs." : ""));
    }

    @FXML
    private void enregistrer() {
        if (profil == null) {
            return;
        }
        try {
            profil.setSexe(comboSexe.getValue());
            profil.setDateNaissance(champNaissance.getValue());
            profil.setTailleCm(Formulaires.lireDouble(champTaille, "Taille"));
            profil.setObjectif(comboObjectif.getValue());
            profil.setNiveauActivite(comboActivite.getValue());
            utilisateurService.modifierProfil(profil);
            Alertes.info("Profil mis à jour. Votre objectif nutritionnel a été recalculé.");
        } catch (RuntimeException e) {
            Alertes.gerer(e);
        }
        charger();
    }

    /**
     * Enregistre le poids du jour. Une seule mesure par jour : si une pesée existe déjà
     * aujourd'hui, elle est corrigée au lieu d'en créer une seconde.
     */
    @FXML
    private void enregistrerPoids() {
        try {
            double poids = Formulaires.lireDouble(champPoids, "Poids");
            if (derniereMesure != null && derniereMesure.getDateMesure().equals(LocalDate.now())) {
                derniereMesure.setPoidsKg(poids);
                suiviService.modifierMesure(derniereMesure);
            } else {
                Mesure m = new Mesure();
                m.setDateMesure(LocalDate.now());
                m.setPoidsKg(poids);
                try {
                    suiviService.enregistrerMesure(m, false);
                } catch (VariationSuspecteException e) {
                    if (!Alertes.confirmer(e.getMessage())) {
                        return;
                    }
                    suiviService.enregistrerMesure(m, true);
                }
            }
            champPoids.clear();
            charger();
            Alertes.info("Pesée enregistrée. Votre objectif nutritionnel a été recalculé.");
        } catch (RuntimeException e) {
            Alertes.gerer(e);
        }
    }

    @FXML
    private void changerMotDePasse() {
        try {
            new AuthService().changerMotDePasse(champAncien.getText(), champNouveau.getText(),
                    champConfirmation.getText());
            champAncien.clear();
            champNouveau.clear();
            champConfirmation.clear();
            Alertes.info("Mot de passe modifié.");
        } catch (RuntimeException e) {
            Alertes.gerer(e);
        }
    }
}
