package ma.emsi.coaching;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import ma.emsi.coaching.util.Alertes;
import ma.emsi.coaching.util.Vues;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        // Toute exception non traitée dans l'interface est affichée proprement
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> Platform.runLater(() -> Alertes.gerer(e)));

        Vues.initialiser(stage);
        stage.setMinWidth(420);
        stage.setMinHeight(480);
        Vues.afficher("login", "Connexion", 420, 500);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
