package ma.emsi.coaching.service;

import java.time.LocalDate;
import java.util.Map;

/**
 * Indicateurs du tableau de bord d'un sportif.
 *
 * @param seancesParSemaine nombre de séances par semaine (clé = lundi de la semaine), 8 dernières semaines
 * @param seancesRealisees  séances réalisées sur les 4 dernières semaines
 * @param seancesPrevues    séances prévues par le programme actif sur la même période
 * @param assiduitePct      seancesRealisees / seancesPrevues, en % (null si pas de programme actif)
 * @param ressentiMoyen     ressenti moyen sur les 4 dernières semaines (null si aucune séance)
 * @param poidsDepart       première pesée (null si aucune)
 * @param poidsActuel       dernière pesée (null si aucune)
 */
public record StatistiquesSportif(Map<LocalDate, Long> seancesParSemaine, int seancesRealisees,
                                  int seancesPrevues, Double assiduitePct, Double ressentiMoyen,
                                  Double poidsDepart, Double poidsActuel) {

    public Double variationPoids() {
        return poidsDepart == null || poidsActuel == null ? null
                : Math.round((poidsActuel - poidsDepart) * 10) / 10.0;
    }
}
