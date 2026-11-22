package ma.emsi.coaching.dao;

import java.util.List;
import java.util.Optional;

/**
 * Opérations CRUD communes à toutes les entités.
 *
 * @param <T> type de l'entité
 */
public interface Dao<T> {

    /** Insère l'entité et lui affecte l'identifiant généré. */
    T ajouter(T entite);

    void modifier(T entite);

    void supprimer(int id);

    Optional<T> trouverParId(int id);

    List<T> trouverTous();
}
