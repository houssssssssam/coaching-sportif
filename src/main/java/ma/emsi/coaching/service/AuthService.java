package ma.emsi.coaching.service;

import ma.emsi.coaching.dao.UtilisateurDao;
import ma.emsi.coaching.model.Utilisateur;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Authentification et gestion des mots de passe (hachage BCrypt).
 */
public class AuthService {

    private final UtilisateurDao utilisateurDao = new UtilisateurDao();

    /**
     * Vérifie les identifiants et ouvre la session.
     * Le même message est renvoyé si l'email ou le mot de passe est faux,
     * pour ne pas indiquer quels emails existent.
     */
    public Utilisateur connecter(String email, String motDePasse) {
        if (email == null || email.isBlank() || motDePasse == null || motDePasse.isEmpty()) {
            throw new ValidationException("Veuillez saisir votre email et votre mot de passe.");
        }
        Utilisateur u = utilisateurDao.trouverParEmail(email.trim().toLowerCase())
                .filter(x -> BCrypt.checkpw(motDePasse, x.getMotDePasseHash()))
                .orElseThrow(() -> new ValidationException("Email ou mot de passe incorrect."));
        if (!u.isActif()) {
            throw new ValidationException("Ce compte est désactivé. Contactez l'administrateur.");
        }
        Session.ouvrir(u);
        return u;
    }

    public void deconnecter() {
        Session.fermer();
    }

    public void changerMotDePasse(String ancien, String nouveau, String confirmation) {
        Utilisateur u = Session.getUtilisateur();
        if (!BCrypt.checkpw(ancien, u.getMotDePasseHash())) {
            throw new ValidationException("L'ancien mot de passe est incorrect.");
        }
        if (!nouveau.equals(confirmation)) {
            throw new ValidationException("La confirmation ne correspond pas au nouveau mot de passe.");
        }
        verifierRobustesse(nouveau);
        String hash = hacher(nouveau);
        utilisateurDao.modifierMotDePasse(u.getId(), hash);
        u.setMotDePasseHash(hash);
    }

    public static String hacher(String motDePasse) {
        return BCrypt.hashpw(motDePasse, BCrypt.gensalt(10));
    }

    /** Au moins 8 caractères, une lettre et un chiffre. */
    public static void verifierRobustesse(String motDePasse) {
        if (motDePasse == null || motDePasse.length() < 8
                || !motDePasse.matches(".*[A-Za-z].*") || !motDePasse.matches(".*\\d.*")) {
            throw new ValidationException("Le mot de passe doit contenir au moins 8 caractères, "
                    + "dont une lettre et un chiffre.");
        }
    }
}
