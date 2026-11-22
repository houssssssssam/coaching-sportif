package ma.emsi.coaching.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Fournit les connexions JDBC à partir du fichier db.properties.
 */
public final class DatabaseConnection {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = DatabaseConnection.class.getResourceAsStream("/db.properties")) {
            if (in == null) {
                throw new IllegalStateException("Fichier db.properties introuvable dans les ressources");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire db.properties", e);
        }
    }

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                PROPS.getProperty("db.url"),
                PROPS.getProperty("db.user"),
                PROPS.getProperty("db.password"));
    }
}
