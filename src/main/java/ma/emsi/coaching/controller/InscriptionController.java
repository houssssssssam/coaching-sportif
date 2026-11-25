package ma.emsi.coaching.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ma.emsi.coaching.model.NiveauActivite;
import ma.emsi.coaching.model.Objectif;
import ma.emsi.coaching.model.ProfilSportif;
import ma.emsi.coaching.model.Sexe;
import ma.emsi.coaching.model.Utilisateur;
import ma.emsi.coaching.service.AuthService;
import ma.emsi.coaching.service.UtilisateurService;
import ma.emsi.coaching.service.ValidationException;
import ma.emsi.coaching.util.Alertes;
import ma.emsi.coaching.util.Formulaires;
import ma.emsi.coaching.util.Vues;

/**
 * Inscription autonome d'un sportif depuis l'écran de connexion.
 */
public class InscriptionController {

    @FXML private TextField champPrenom;
    @FXML private TextField champNom;
    @FXML private TextField champEmail;
    @FXML private PasswordField champMotDePasse;
    @FXML private PasswordField champConfirmation;
    @FXML private ComboBox<Sexe> comboSexe;
    @FXML private DatePicker champNaissance;
    @FXML private TextField champTaille;
    @FXML private TextField champPoids;
    @FXML private ComboBox<Objectif> comboObjectif;
    @FXML private ComboBox<NiveauActivite> comboActivite;
    @FXML private Label messageErreur;

    @FXML
    private void initialize() {
        comboSexe.getItems().setAll(Sexe.values());
        comboObjectif.getItems().setAll(Objectif.values());
        comboActivite.getItems().setAll(NiveauActivite.values());
        comboActivite.setValue(NiveauActivite.MODERE);
        messageErreur.setText("");
    }

    @FXML
    private void creerCompte() {
        messageErreur.setText("");
        try {
            Utilisateur u = new Utilisateur();
            u.setPrenom(champPrenom.getText());
            u.setNom(champNom.getText());
            u.setEmail(champEmail.getText());

            ProfilSportif p = new ProfilSportif();
            p.setSexe(comboSexe.getValue());
            p.setDateNaissance(champNaissance.getValue());
            p.setTailleCm(Formulaires.lireDouble(champTaille, "Taille"));
            p.setObjectif(comboObjectif.getValue());
            p.setNiveauActivite(comboActivite.getValue());
            double poids = Formulaires.lireDouble(champPoids, "Poids actuel");

            new UtilisateurService().inscrire(u, champMotDePasse.getText(), champConfirmation.getText(), p, poids);
            // Connexion automatique après l'inscription
            new AuthService().connecter(u.getEmail(), champMotDePasse.getText());
            Vues.afficher("main", "Accueil", 1280, 780);
            Alertes.info("Bienvenue " + u.getPrenom() + " ! Votre compte est créé. Vous pouvez déjà suivre "
                    + "votre nutrition et vos mesures ; un coach vous sera attribué par l'administrateur.");
        } catch (ValidationException e) {
            messageErreur.setText(e.getMessage());
        } catch (RuntimeException e) {
            Alertes.gerer(e);
        }
    }

    @FXML
    private void retourConnexion() {
        Vues.afficher("login", "Connexion", 420, 540);
    }
}
