package ma.emsi.coaching;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ma.emsi.coaching.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        Label titre = new Label("Coaching sportif personnalisé");
        titre.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        Label statut = new Label(testerConnexion());

        VBox root = new VBox(15, titre, statut);
        root.setPadding(new Insets(30));

        stage.setTitle("Coaching sportif");
        stage.setScene(new Scene(root, 500, 200));
        stage.show();
    }

    private String testerConnexion() {
        try (Connection cn = DatabaseConnection.getConnection();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM utilisateur")) {
            rs.next();
            return "Connexion à MySQL : OK (" + rs.getInt(1) + " utilisateurs en base)";
        } catch (Exception e) {
            return "Erreur de connexion : " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
