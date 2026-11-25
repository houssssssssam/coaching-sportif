package ma.emsi.coaching.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ma.emsi.coaching.model.Role;
import ma.emsi.coaching.model.Utilisateur;
import ma.emsi.coaching.service.AuthService;
import ma.emsi.coaching.service.Session;
import ma.emsi.coaching.util.Alertes;
import ma.emsi.coaching.util.Vues;

import java.util.List;

/**
 * Fenêtre principale : menu latéral selon le rôle + zone de contenu.
 */
public class MainController {

    /** Une entrée du menu : libellé affiché et nom du fichier FXML à charger. */
    private record EntreeMenu(String libelle, String vue) {
    }

    @FXML
    private VBox menu;
    @FXML
    private StackPane contenu;
    @FXML
    private Label nomUtilisateur;
    @FXML
    private Label roleUtilisateur;

    private Button boutonActif;

    @FXML
    private void initialize() {
        Utilisateur u = Session.getUtilisateur();
        nomUtilisateur.setText(u.getNomComplet());
        roleUtilisateur.setText(u.getRole().getLibelle());

        for (EntreeMenu entree : menuPour(u.getRole())) {
            Button b = new Button(entree.libelle());
            b.getStyleClass().add("bouton-menu");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> ouvrir(entree, b));
            menu.getChildren().add(b);
        }
        // On ouvre la première entrée (Accueil) au démarrage
        Button premier = (Button) menu.getChildren().get(0);
        premier.fire();
    }

    private List<EntreeMenu> menuPour(Role role) {
        return switch (role) {
            case ADMIN -> List.of(
                    new EntreeMenu("Accueil", "accueil"),
                    new EntreeMenu("Utilisateurs", "admin-utilisateurs"),
                    new EntreeMenu("Exercices", "admin-exercices"),
                    new EntreeMenu("Aliments", "admin-aliments"));
            case COACH -> List.of(
                    new EntreeMenu("Accueil", "accueil"),
                    new EntreeMenu("Mes programmes", "coach-programmes"),
                    new EntreeMenu("Mes sportifs", "coach-sportifs"));
            case SPORTIF -> List.of(
                    new EntreeMenu("Accueil", "accueil"),
                    new EntreeMenu("Tableau de bord", "tableau-bord"),
                    new EntreeMenu("Mon programme", "sportif-programme"),
                    new EntreeMenu("Mes séances", "sportif-seances"),
                    new EntreeMenu("Mes mesures", "sportif-mesures"),
                    new EntreeMenu("Journal alimentaire", "sportif-journal"),
                    new EntreeMenu("Mon profil", "sportif-profil"));
        };
    }

    private void ouvrir(EntreeMenu entree, Button bouton) {
        if (boutonActif != null) {
            boutonActif.getStyleClass().remove("actif");
        }
        bouton.getStyleClass().add("actif");
        boutonActif = bouton;

        try {
            Node vue = Vues.existe(entree.vue())
                    ? Vues.charger(entree.vue()).getRoot()
                    : enConstruction(entree.libelle());
            contenu.getChildren().setAll(vue);
        } catch (RuntimeException e) {
            Alertes.gerer(e);
        }
    }

    private Node enConstruction(String libelle) {
        Label l = new Label(libelle + " : écran en cours de développement.");
        l.getStyleClass().add("texte-secondaire");
        return l;
    }

    @FXML
    private void seDeconnecter() {
        if (Alertes.confirmer("Voulez-vous vous déconnecter ?")) {
            new AuthService().deconnecter();
            Vues.afficher("login", "Connexion", 420, 540);
        }
    }
}
