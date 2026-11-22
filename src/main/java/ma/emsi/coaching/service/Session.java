package ma.emsi.coaching.service;

import ma.emsi.coaching.model.Role;
import ma.emsi.coaching.model.Utilisateur;

import java.util.Arrays;

/**
 * Utilisateur actuellement connecté à l'application (application desktop mono-utilisateur).
 */
public final class Session {

    private static Utilisateur utilisateur;

    private Session() {
    }

    public static void ouvrir(Utilisateur u) {
        utilisateur = u;
    }

    public static void fermer() {
        utilisateur = null;
    }

    public static Utilisateur getUtilisateur() {
        if (utilisateur == null) {
            throw new AccesRefuseException("Aucun utilisateur connecté.");
        }
        return utilisateur;
    }

    public static boolean estConnecte() {
        return utilisateur != null;
    }

    /** Vérifie que l'utilisateur connecté a l'un des rôles demandés. */
    public static Utilisateur exiger(Role... roles) {
        Utilisateur u = getUtilisateur();
        if (Arrays.stream(roles).noneMatch(r -> r == u.getRole())) {
            throw new AccesRefuseException("Action réservée au rôle : " + Arrays.toString(roles));
        }
        return u;
    }
}
