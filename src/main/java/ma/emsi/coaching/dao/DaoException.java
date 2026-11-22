package ma.emsi.coaching.dao;

/**
 * Exception levée par la couche d'accès aux données.
 * Elle enveloppe les SQLException pour que les couches supérieures
 * n'aient pas à dépendre de JDBC.
 */
public class DaoException extends RuntimeException {

    public DaoException(String message, Throwable cause) {
        super(message, cause);
    }

    public DaoException(String message) {
        super(message);
    }
}
