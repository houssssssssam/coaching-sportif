package ma.emsi.coaching.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Chargement des vues FXML et changement d'écran.
 */
public final class Vues {

    private static final String DOSSIER_VUES = "/ma/emsi/coaching/view/";
    private static final String CSS = "/ma/emsi/coaching/css/style.css";

    private static Stage fenetre;

    private Vues() {
    }

    public static void initialiser(Stage stage) {
        fenetre = stage;
    }

    /** Indique si une vue existe (utile pour les écrans pas encore créés). */
    public static boolean existe(String nom) {
        return Vues.class.getResource(DOSSIER_VUES + nom + ".fxml") != null;
    }

    /** Charge un fichier FXML et renvoie le loader (pour récupérer son contrôleur). */
    public static FXMLLoader charger(String nom) {
        URL url = Vues.class.getResource(DOSSIER_VUES + nom + ".fxml");
        if (url == null) {
            throw new IllegalArgumentException("Vue introuvable : " + nom);
        }
        FXMLLoader loader = new FXMLLoader(url);
        try {
            loader.load();
            return loader;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger la vue " + nom, e);
        }
    }

    /** Ouvre une vue dans une nouvelle fenêtre et renvoie son loader (pour accéder au contrôleur). */
    public static FXMLLoader ouvrirFenetre(String nom, String titre, double largeur, double hauteur) {
        FXMLLoader loader = charger(nom);
        Scene scene = new Scene(loader.getRoot(), largeur, hauteur);
        scene.getStylesheets().add(Vues.class.getResource(CSS).toExternalForm());
        scene.getRoot().setStyle("-fx-padding: 20;");
        Stage s = new Stage();
        s.initOwner(fenetre);
        s.setTitle("Coaching sportif - " + titre);
        s.setScene(scene);
        s.show();
        return loader;
    }

    /** Remplace tout le contenu de la fenêtre par une vue. */
    public static void afficher(String nom, String titre, double largeur, double hauteur) {
        Parent racine = charger(nom).getRoot();
        Scene scene = new Scene(racine, largeur, hauteur);
        scene.getStylesheets().add(Vues.class.getResource(CSS).toExternalForm());
        fenetre.setTitle("Coaching sportif - " + titre);
        fenetre.setScene(scene);
        fenetre.centerOnScreen();
        fenetre.show();
    }
}
