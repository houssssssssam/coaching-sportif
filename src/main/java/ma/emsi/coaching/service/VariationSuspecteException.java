package ma.emsi.coaching.service;

/**
 * Variation de poids inhabituelle : l'interface doit demander confirmation
 * puis rappeler enregistrerMesure(mesure, true).
 */
public class VariationSuspecteException extends ValidationException {

    public VariationSuspecteException(String message) {
        super(message);
    }
}
