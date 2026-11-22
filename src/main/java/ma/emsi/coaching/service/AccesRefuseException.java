package ma.emsi.coaching.service;

/**
 * Levée quand l'utilisateur connecté n'a pas le droit d'effectuer une action.
 */
public class AccesRefuseException extends RuntimeException {

    public AccesRefuseException(String message) {
        super(message);
    }
}
