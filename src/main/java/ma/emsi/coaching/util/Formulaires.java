package ma.emsi.coaching.util;

import javafx.scene.control.TextField;
import ma.emsi.coaching.service.ValidationException;

/**
 * Lecture et conversion des champs de saisie.
 */
public final class Formulaires {

    private Formulaires() {
    }

    /** Lit un nombre décimal obligatoire (accepte la virgule ou le point). */
    public static double lireDouble(TextField champ, String libelle) {
        String t = champ.getText() == null ? "" : champ.getText().trim().replace(',', '.');
        if (t.isEmpty()) {
            throw new ValidationException("Le champ « " + libelle + " » est obligatoire.");
        }
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            throw new ValidationException("Le champ « " + libelle + " » doit être un nombre.");
        }
    }

    /** Lit un nombre décimal facultatif : renvoie null si le champ est vide. */
    public static Double lireDoubleOptionnel(TextField champ, String libelle) {
        String t = champ.getText() == null ? "" : champ.getText().trim();
        return t.isEmpty() ? null : lireDouble(champ, libelle);
    }

    /** Lit un entier obligatoire. */
    public static int lireEntier(TextField champ, String libelle) {
        String t = champ.getText() == null ? "" : champ.getText().trim();
        if (t.isEmpty()) {
            throw new ValidationException("Le champ « " + libelle + " » est obligatoire.");
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            throw new ValidationException("Le champ « " + libelle + " » doit être un nombre entier.");
        }
    }

    /** Lit un entier facultatif : renvoie null si le champ est vide. */
    public static Integer lireEntierOptionnel(TextField champ, String libelle) {
        String t = champ.getText() == null ? "" : champ.getText().trim();
        return t.isEmpty() ? null : lireEntier(champ, libelle);
    }

    /** Affiche un nombre sans ".0" inutile. */
    public static String texte(Number n) {
        if (n == null) {
            return "";
        }
        double d = n.doubleValue();
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }
}
