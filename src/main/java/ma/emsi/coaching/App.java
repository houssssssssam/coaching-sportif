package ma.emsi.coaching;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import ma.emsi.coaching.util.Alertes;
import ma.emsi.coaching.util.Vues;

import java.util.TimeZone;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        // Java applique UTC+1 pour le Maroc alors que l'horloge Windows est en UTC+0 :
        // on aligne l'application sur l'heure du PC pour que "aujourd'hui" soit le même partout.
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        // Toute exception non traitée dans l'interface est affichée proprement
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> Platform.runLater(() -> Alertes.gerer(e)));

        Vues.initialiser(stage);
        stage.setMinWidth(420);
        stage.setMinHeight(480);
        Vues.afficher("login", "Connexion", 420, 540);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
