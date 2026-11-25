package ma.emsi.coaching.service;

import ma.emsi.coaching.model.Mesure;
import ma.emsi.coaching.model.Repas;
import ma.emsi.coaching.model.Seance;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Export des données de suivi d'un sportif au format CSV.
 * Séparateur ";" et encodage UTF-8 avec BOM pour une ouverture directe dans Excel.
 */
public class ExportService {

    private static final Logger LOG = Logger.getLogger(ExportService.class.getName());
    private static final String SEP = ";";

    public enum TypeExport {
        SEANCES("Séances"),
        MESURES("Mesures"),
        JOURNAL("Journal alimentaire (30 derniers jours)");

        private final String libelle;

        TypeExport(String libelle) {
            this.libelle = libelle;
        }

        @Override
        public String toString() {
            return libelle;
        }
    }

    private final SuiviService suiviService = new SuiviService();
    private final NutritionService nutritionService = new NutritionService();

    /** Écrit le fichier et renvoie le nombre de lignes de données exportées. */
    public int exporter(int sportifId, TypeExport type, Path fichier) {
        try (BufferedWriter w = Files.newBufferedWriter(fichier, StandardCharsets.UTF_8)) {
            w.write('﻿'); // BOM : Excel reconnaît l'UTF-8 et affiche les accents
            int n = switch (type) {
                case SEANCES -> ecrireSeances(w, suiviService.seances(sportifId));
                case MESURES -> ecrireMesures(w, suiviService.mesures(sportifId));
                case JOURNAL -> ecrireJournal(w, nutritionService.journal(sportifId,
                        LocalDate.now().minusDays(29), LocalDate.now()));
            };
            LOG.info(() -> "Export " + type.name() + " du sportif " + sportifId + " : " + n + " ligne(s) vers " + fichier);
            return n;
        } catch (IOException e) {
            throw new ValidationException("Impossible d'écrire le fichier : " + e.getMessage());
        }
    }

    private int ecrireSeances(BufferedWriter w, List<Seance> seances) throws IOException {
        ligne(w, "Date", "Durée (min)", "Ressenti (1-5)", "Commentaire");
        for (Seance s : seances) {
            ligne(w, s.getDateSeance().toString(), String.valueOf(s.getDureeMin()),
                    String.valueOf(s.getRessenti()), s.getCommentaire());
        }
        return seances.size();
    }

    private int ecrireMesures(BufferedWriter w, List<Mesure> mesures) throws IOException {
        ligne(w, "Date", "Poids (kg)", "Tour de taille (cm)", "Masse grasse (%)");
        for (Mesure m : mesures) {
            ligne(w, m.getDateMesure().toString(), nombre(m.getPoidsKg()),
                    nombre(m.getTourTailleCm()), nombre(m.getMasseGrassePct()));
        }
        return mesures.size();
    }

    private int ecrireJournal(BufferedWriter w, List<Repas> repas) throws IOException {
        ligne(w, "Date", "Repas", "Aliment", "Quantité (g)", "Calories (kcal)", "Protéines (g)",
                "Glucides (g)", "Lipides (g)");
        for (Repas r : repas) {
            ligne(w, r.getDateRepas().toString(), r.getTypeRepas().getLibelle(), r.getAliment().getNom(),
                    nombre(r.getQuantiteG()), nombre(r.getCalories()), nombre(r.getProteines()),
                    nombre(r.getGlucides()), nombre(r.getLipides()));
        }
        return repas.size();
    }

    private static void ligne(BufferedWriter w, String... valeurs) throws IOException {
        for (int i = 0; i < valeurs.length; i++) {
            if (i > 0) {
                w.write(SEP);
            }
            w.write(echapper(valeurs[i]));
        }
        w.newLine();
    }

    /** Entoure de guillemets les valeurs qui contiennent le séparateur, un guillemet ou un retour à la ligne. */
    static String echapper(String v) {
        if (v == null) {
            return "";
        }
        if (v.contains(SEP) || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    /** Nombre avec une décimale et une virgule (format français, lu correctement par Excel). */
    private static String nombre(Double d) {
        return d == null ? "" : String.format(Locale.FRANCE, "%.1f", d);
    }
}
