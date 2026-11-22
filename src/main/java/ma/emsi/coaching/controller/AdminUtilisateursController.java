package ma.emsi.coaching.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ma.emsi.coaching.model.NiveauActivite;
import ma.emsi.coaching.model.Objectif;
import ma.emsi.coaching.model.ProfilSportif;
import ma.emsi.coaching.model.Role;
import ma.emsi.coaching.model.Sexe;
import ma.emsi.coaching.model.Utilisateur;
import ma.emsi.coaching.service.UtilisateurService;
import ma.emsi.coaching.util.Alertes;
import ma.emsi.coaching.util.Formulaires;

import java.util.Objects;

/**
 * Gestion des comptes par l'administrateur.
 */
public class AdminUtilisateursController {

    private static final String TOUS = "Tous les rôles";

    @FXML private TextField champRecherche;
    @FXML private ComboBox<String> filtreRole;
    @FXML private TableView<Utilisateur> table;
    @FXML private TableColumn<Utilisateur, String> colNom;
    @FXML private TableColumn<Utilisateur, String> colPrenom;
    @FXML private TableColumn<Utilisateur, String> colEmail;
    @FXML private TableColumn<Utilisateur, String> colRole;
    @FXML private TableColumn<Utilisateur, String> colStatut;

    @FXML private Label titreFormulaire;
    @FXML private TextField champNom;
    @FXML private TextField champPrenom;
    @FXML private TextField champEmail;
    @FXML private ComboBox<Role> comboRole;
    @FXML private Label labelMotDePasse;
    @FXML private PasswordField champMotDePasse;
    @FXML private VBox sectionProfil;
    @FXML private ComboBox<Utilisateur> comboCoach;
    @FXML private ComboBox<Sexe> comboSexe;
    @FXML private DatePicker champNaissance;
    @FXML private TextField champTaille;
    @FXML private ComboBox<Objectif> comboObjectif;
    @FXML private ComboBox<NiveauActivite> comboActivite;
    @FXML private Button boutonStatut;
    @FXML private Button boutonMotDePasse;

    private final UtilisateurService service = new UtilisateurService();
    private FilteredList<Utilisateur> liste;
    /** Utilisateur en cours de modification (null = création). */
    private Utilisateur selection;

    @FXML
    private void initialize() {
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colPrenom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrenom()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole().getLibelle()));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActif() ? "Actif" : "Désactivé"));

        filtreRole.getItems().add(TOUS);
        for (Role r : Role.values()) {
            filtreRole.getItems().add(r.getLibelle());
        }
        filtreRole.setValue(TOUS);

        comboRole.getItems().setAll(Role.values());
        comboSexe.getItems().setAll(Sexe.values());
        comboObjectif.getItems().setAll(Objectif.values());
        comboActivite.getItems().setAll(NiveauActivite.values());
        comboCoach.getItems().setAll(service.listerParRole(Role.COACH));

        // La section profil n'apparaît que pour un sportif
        sectionProfil.managedProperty().bind(sectionProfil.visibleProperty());
        comboRole.valueProperty().addListener((o, a, n) -> sectionProfil.setVisible(n == Role.SPORTIF));

        champRecherche.textProperty().addListener((o, a, n) -> filtrer());
        filtreRole.valueProperty().addListener((o, a, n) -> filtrer());
        table.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            if (n != null) {
                remplir(n);
            }
        });

        charger();
        nouveau();
    }

    private void charger() {
        liste = new FilteredList<>(FXCollections.observableArrayList(service.listerTous()));
        table.setItems(liste);
        filtrer();
    }

    private void filtrer() {
        String texte = champRecherche.getText() == null ? "" : champRecherche.getText().toLowerCase().trim();
        String role = filtreRole.getValue();
        liste.setPredicate(u ->
                (TOUS.equals(role) || u.getRole().getLibelle().equals(role))
                        && (texte.isEmpty()
                        || u.getNomComplet().toLowerCase().contains(texte)
                        || u.getEmail().contains(texte)));
    }

    @FXML
    private void nouveau() {
        selection = null;
        table.getSelectionModel().clearSelection();
        titreFormulaire.setText("Nouvel utilisateur");
        champNom.clear();
        champPrenom.clear();
        champEmail.clear();
        champMotDePasse.clear();
        comboRole.setDisable(false);
        comboRole.setValue(Role.SPORTIF);
        labelMotDePasse.setText("Mot de passe");
        comboCoach.setValue(null);
        comboSexe.setValue(null);
        champNaissance.setValue(null);
        champTaille.clear();
        comboObjectif.setValue(null);
        comboActivite.setValue(null);
        boutonStatut.setDisable(true);
        boutonMotDePasse.setDisable(true);
    }

    private void remplir(Utilisateur u) {
        selection = u;
        titreFormulaire.setText("Modifier " + u.getNomComplet());
        champNom.setText(u.getNom());
        champPrenom.setText(u.getPrenom());
        champEmail.setText(u.getEmail());
        champMotDePasse.clear();
        labelMotDePasse.setText("Nouveau mot de passe (facultatif)");
        comboRole.setValue(u.getRole());
        comboRole.setDisable(true); // le rôle n'est pas modifiable après création
        boutonStatut.setDisable(false);
        boutonStatut.setText(u.isActif() ? "Désactiver" : "Réactiver");
        boutonMotDePasse.setDisable(false);

        if (u.getRole() == Role.SPORTIF) {
            service.trouverProfil(u.getId()).ifPresent(p -> {
                comboCoach.setValue(comboCoach.getItems().stream()
                        .filter(c -> Objects.equals(c.getId(), p.getCoachId())).findFirst().orElse(null));
                comboSexe.setValue(p.getSexe());
                champNaissance.setValue(p.getDateNaissance());
                champTaille.setText(Formulaires.texte(p.getTailleCm()));
                comboObjectif.setValue(p.getObjectif());
                comboActivite.setValue(p.getNiveauActivite());
            });
        }
    }

    @FXML
    private void enregistrer() {
        try {
            Utilisateur u = selection != null ? selection : new Utilisateur();
            u.setNom(champNom.getText());
            u.setPrenom(champPrenom.getText());
            u.setEmail(champEmail.getText());
            u.setRole(comboRole.getValue());
            if (u.getRole() == null) {
                Alertes.erreur("Choisissez un rôle.");
                return;
            }

            ProfilSportif profil = u.getRole() == Role.SPORTIF ? lireProfil(u) : null;

            if (selection == null) {
                if (profil != null) {
                    service.creerSportif(u, champMotDePasse.getText(), profil);
                } else {
                    service.creer(u, champMotDePasse.getText());
                }
                Alertes.info("Compte créé pour " + u.getNomComplet() + ".");
            } else {
                service.modifier(u);
                if (profil != null) {
                    service.modifierProfil(profil);
                }
                Alertes.info("Modifications enregistrées.");
            }
            charger();
            nouveau();
        } catch (RuntimeException e) {
            Alertes.gerer(e);
            charger(); // on recharge pour annuler d'éventuelles modifications locales
        }
    }

    private ProfilSportif lireProfil(Utilisateur u) {
        ProfilSportif p = new ProfilSportif();
        p.setUtilisateurId(u.getId());
        p.setCoachId(comboCoach.getValue() == null ? null : comboCoach.getValue().getId());
        p.setSexe(comboSexe.getValue());
        p.setDateNaissance(champNaissance.getValue());
        p.setTailleCm(Formulaires.lireDouble(champTaille, "Taille"));
        p.setObjectif(comboObjectif.getValue());
        p.setNiveauActivite(comboActivite.getValue());
        return p;
    }

    @FXML
    private void changerStatut() {
        if (selection == null) {
            return;
        }
        boolean activer = !selection.isActif();
        String action = activer ? "réactiver" : "désactiver";
        if (!Alertes.confirmer("Voulez-vous " + action + " le compte de " + selection.getNomComplet() + " ?")) {
            return;
        }
        try {
            service.changerStatut(selection.getId(), activer);
            charger();
            nouveau();
        } catch (RuntimeException e) {
            Alertes.gerer(e);
        }
    }

    @FXML
    private void reinitialiserMotDePasse() {
        if (selection == null) {
            return;
        }
        try {
            service.reinitialiserMotDePasse(selection.getId(), champMotDePasse.getText());
            champMotDePasse.clear();
            Alertes.info("Mot de passe modifié pour " + selection.getNomComplet() + ".");
        } catch (RuntimeException e) {
            Alertes.gerer(e);
        }
    }
}
