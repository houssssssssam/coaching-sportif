package ma.emsi.coaching.service;

import ma.emsi.coaching.dao.ExerciceDao;
import ma.emsi.coaching.model.Exercice;
import ma.emsi.coaching.model.Role;

import java.util.List;

/**
 * Catalogue des exercices : consultable par tous, modifiable par l'admin.
 */
public class CatalogueService {

    private final ExerciceDao exerciceDao = new ExerciceDao();

    public List<Exercice> exercices() {
        Session.getUtilisateur();
        return exerciceDao.trouverTous();
    }

    public List<Exercice> rechercher(String texte) {
        Session.getUtilisateur();
        return texte == null || texte.isBlank() ? exerciceDao.trouverTous() : exerciceDao.rechercher(texte);
    }

    public Exercice ajouter(Exercice e) {
        Session.exiger(Role.ADMIN);
        valider(e);
        return exerciceDao.ajouter(e);
    }

    public void modifier(Exercice e) {
        Session.exiger(Role.ADMIN);
        valider(e);
        exerciceDao.modifier(e);
    }

    public void supprimer(int id) {
        Session.exiger(Role.ADMIN);
        exerciceDao.supprimer(id);
    }

    private void valider(Exercice e) {
        if (e.getNom() == null || e.getNom().trim().length() < 2) {
            throw new ValidationException("Le nom de l'exercice est obligatoire.");
        }
        if (e.getGroupeMusculaire() == null || e.getGroupeMusculaire().isBlank()) {
            throw new ValidationException("Le groupe musculaire est obligatoire.");
        }
        if (e.getType() == null) {
            throw new ValidationException("Choisissez un type d'exercice.");
        }
        e.setNom(e.getNom().trim());
        e.setGroupeMusculaire(e.getGroupeMusculaire().trim());
        boolean doublon = exerciceDao.trouverTous().stream()
                .anyMatch(x -> x.getId() != e.getId() && x.getNom().equalsIgnoreCase(e.getNom()));
        if (doublon) {
            throw new ValidationException("Un exercice porte déjà ce nom.");
        }
    }
}
