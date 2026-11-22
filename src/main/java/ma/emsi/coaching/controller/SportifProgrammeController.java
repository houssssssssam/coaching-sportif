package ma.emsi.coaching.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import ma.emsi.coaching.model.Affectation;
import ma.emsi.coaching.model.Programme;
import ma.emsi.coaching.model.ProgrammeExercice;
import ma.emsi.coaching.service.ProgrammeService;
import ma.emsi.coaching.service.Session;
import ma.emsi.coaching.util.Formulaires;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Le sportif consulte son programme actif, présenté jour par jour.
 */
public class SportifProgrammeController {

    private static final String[] NOMS_JOURS = {"", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"};

    @FXML private Label nomProgramme;
    @FXML private Label infosProgramme;
    @FXML private FlowPane jours;

    private final ProgrammeService service = new ProgrammeService();

    @FXML
    private void initialize() {
        int id = Session.getUtilisateur().getId();
        Optional<Affectation> actif = service.programmeActif(id);
        if (actif.isEmpty()) {
            nomProgramme.setText("Aucun programme actif");
            infosProgramme.setText("Votre coach ne vous a pas encore affecté de programme.");
            return;
        }
        Programme p = actif.get().getProgramme();
        nomProgramme.setText(p.getNom());
        infosProgramme.setText(p.getObjectif().getLibelle() + " · " + p.getNiveau().getLibelle()
                + " · depuis le " + actif.get().getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + (p.getDescription() == null ? "" : "\n" + p.getDescription()));

        Map<Integer, List<ProgrammeExercice>> parJour = service.contenu(p.getId()).stream()
                .collect(Collectors.groupingBy(ProgrammeExercice::getJour, TreeMap::new, Collectors.toList()));
        parJour.forEach((jour, lignes) -> jours.getChildren().add(carteJour(jour, lignes)));
    }

    private VBox carteJour(int jour, List<ProgrammeExercice> lignes) {
        VBox carte = new VBox(8);
        carte.getStyleClass().add("carte");
        carte.setPrefWidth(300);
        Label titre = new Label("Jour " + jour + " · " + NOMS_JOURS[jour]);
        titre.getStyleClass().add("titre-formulaire");
        carte.getChildren().add(titre);
        for (ProgrammeExercice pe : lignes) {
            Label nom = new Label(pe.getExercice().getNom());
            nom.setStyle("-fx-font-weight: bold;");
            Label detail = new Label(detail(pe));
            detail.getStyleClass().add("texte-secondaire");
            detail.setWrapText(true);
            carte.getChildren().add(new VBox(2, nom, detail));
        }
        return carte;
    }

    private static String detail(ProgrammeExercice pe) {
        StringBuilder sb = new StringBuilder();
        if (pe.getDureeMin() != null) {
            sb.append(pe.getDureeMin()).append(" min");
        } else {
            if (pe.getSeries() != null) {
                sb.append(pe.getSeries()).append(" séries");
            }
            if (pe.getRepetitions() != null) {
                sb.append(" de ").append(pe.getRepetitions()).append(" répétitions");
            }
            if (pe.getChargeKg() != null) {
                sb.append(" à ").append(Formulaires.texte(pe.getChargeKg())).append(" kg");
            }
        }
        if (pe.getConsigne() != null) {
            sb.append(sb.length() > 0 ? " · " : "").append(pe.getConsigne());
        }
        return sb.toString();
    }
}
