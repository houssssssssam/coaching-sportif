package ma.emsi.coaching.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ma.emsi.coaching.service.AuthService;
import ma.emsi.coaching.service.ValidationException;
import ma.emsi.coaching.util.Alertes;
import ma.emsi.coaching.util.Vues;

public class LoginController {

    @FXML
    private TextField champEmail;
    @FXML
    private PasswordField champMotDePasse;
    @FXML
    private Label messageErreur;
    @FXML
    private Button boutonConnexion;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        messageErreur.setText("");
    }

    @FXML
    private void ouvrirInscription() {
        Vues.afficher("inscription", "Inscription", 520, 760);
    }

    @FXML
    private void seConnecter() {
        messageErreur.setText("");
        try {
            authService.connecter(champEmail.getText(), champMotDePasse.getText());
            Vues.afficher("main", "Accueil", 1280, 780);
        } catch (ValidationException e) {
            messageErreur.setText(e.getMessage());
            champMotDePasse.clear();
            champMotDePasse.requestFocus();
        } catch (RuntimeException e) {
            Alertes.gerer(e);
        }
    }
}
