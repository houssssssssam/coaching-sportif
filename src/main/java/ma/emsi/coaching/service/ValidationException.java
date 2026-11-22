package ma.emsi.coaching.service;

/**
 * Erreur métier ou de saisie. Son message est destiné à être affiché
 * tel quel à l'utilisateur.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
