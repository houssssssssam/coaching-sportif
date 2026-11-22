package ma.emsi.coaching.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import ma.emsi.coaching.dao.DaoException;
import ma.emsi.coaching.service.AccesRefuseException;
import ma.emsi.coaching.service.ValidationException;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Boîtes de dialogue standard et traitement centralisé des erreurs.
 */
public final class Alertes {

    private static final Logger LOG = Logger.getLogger(Alertes.class.getName());

    private Alertes() {
    }

    public static void info(String message) {
        afficher(Alert.AlertType.INFORMATION, "Information", message);
    }

    public static void erreur(String message) {
        afficher(Alert.AlertType.ERROR, "Erreur", message);
    }

    public static boolean confirmer(String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        a.setTitle("Confirmation");
        a.setHeaderText(null);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.YES;
    }

    /**
     * Affiche une erreur adaptée à son type :
     * - erreur de saisie ou droit refusé : message tel quel
     * - erreur de base de données : message générique + détail dans les logs
     * - autre : erreur inattendue
     */
    public static void gerer(Throwable t) {
        Throwable e = t;
        while (e.getCause() != null && !(e instanceof ValidationException)
                && !(e instanceof AccesRefuseException) && !(e instanceof DaoException)) {
            e = e.getCause();
        }
        if (e instanceof ValidationException || e instanceof AccesRefuseException) {
            afficher(Alert.AlertType.WARNING, "Attention", e.getMessage());
        } else if (e instanceof DaoException) {
            LOG.log(Level.SEVERE, "Erreur base de données", e);
            erreur(e.getMessage() + "\n\nVérifiez que le serveur MySQL est démarré.");
        } else {
            LOG.log(Level.SEVERE, "Erreur inattendue", t);
            erreur("Une erreur inattendue s'est produite : " + t.getMessage());
        }
    }

    private static void afficher(Alert.AlertType type, String titre, String message) {
        Alert a = new Alert(type, message, ButtonType.OK);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
