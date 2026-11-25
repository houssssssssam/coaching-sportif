package ma.emsi.coaching.service;

import ma.emsi.coaching.dao.MesureDao;
import ma.emsi.coaching.dao.ProfilSportifDao;
import ma.emsi.coaching.dao.UtilisateurDao;
import ma.emsi.coaching.model.Mesure;
import ma.emsi.coaching.model.ProfilSportif;
import ma.emsi.coaching.model.Role;
import ma.emsi.coaching.model.Utilisateur;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Gestion des comptes (réservée à l'administrateur, sauf la consultation de son propre profil).
 */
public class UtilisateurService {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private final UtilisateurDao utilisateurDao = new UtilisateurDao();
    private final ProfilSportifDao profilDao = new ProfilSportifDao();
    private final MesureDao mesureDao = new MesureDao();
    private static final Logger LOG = Logger.getLogger(UtilisateurService.class.getName());

    public List<Utilisateur> listerTous() {
        Session.exiger(Role.ADMIN);
        return utilisateurDao.trouverTous();
    }

    public List<Utilisateur> listerParRole(Role role) {
        Session.exiger(Role.ADMIN, Role.COACH);
        return utilisateurDao.trouverParRole(role);
    }

    /** Crée un administrateur ou un coach. */
    public Utilisateur creer(Utilisateur u, String motDePasse) {
        Session.exiger(Role.ADMIN);
        if (u.getRole() == Role.SPORTIF) {
            throw new ValidationException("Utilisez la création de sportif pour renseigner son profil.");
        }
        preparer(u, motDePasse);
        return utilisateurDao.ajouter(u);
    }

    /** Crée un sportif et son profil physique en une seule opération. */
    public Utilisateur creerSportif(Utilisateur u, String motDePasse, ProfilSportif profil) {
        Session.exiger(Role.ADMIN);
        u.setRole(Role.SPORTIF);
        preparer(u, motDePasse);
        validerProfil(profil);
        return utilisateurDao.ajouterSportif(u, profil);
    }

    /**
     * Inscription autonome d'un sportif, sans être connecté.
     * Le compte est actif immédiatement mais sans coach : le sportif peut suivre sa nutrition,
     * ses mesures et ses séances ; l'administrateur lui attribuera un coach plus tard.
     * Une première pesée est enregistrée pour pouvoir calculer l'objectif calorique.
     */
    public Utilisateur inscrire(Utilisateur u, String motDePasse, String confirmation,
                                ProfilSportif profil, double poidsKg) {
        if (motDePasse == null || !motDePasse.equals(confirmation)) {
            throw new ValidationException("La confirmation ne correspond pas au mot de passe.");
        }
        if (poidsKg < 30 || poidsKg > 300) {
            throw new ValidationException("Le poids doit être compris entre 30 et 300 kg.");
        }
        u.setId(0);
        u.setRole(Role.SPORTIF); // une inscription ne peut créer qu'un sportif
        profil.setCoachId(null);
        preparer(u, motDePasse);
        validerProfil(profil);
        utilisateurDao.ajouterSportif(u, profil);

        Mesure premiere = new Mesure();
        premiere.setSportifId(u.getId());
        premiere.setDateMesure(LocalDate.now());
        premiere.setPoidsKg(poidsKg);
        mesureDao.ajouter(premiere);
        LOG.info(() -> "Nouvelle inscription : " + u.getEmail());
        return u;
    }

    /** Sportifs inscrits seuls, en attente d'un coach. */
    public List<Utilisateur> listerSansCoach() {
        Session.exiger(Role.ADMIN);
        return utilisateurDao.trouverSportifsSansCoach();
    }

    public void modifier(Utilisateur u) {
        Session.exiger(Role.ADMIN);
        validerIdentite(u);
        utilisateurDao.modifier(u);
    }

    public void modifierProfil(ProfilSportif profil) {
        Utilisateur courant = Session.exiger(Role.ADMIN, Role.SPORTIF);
        if (courant.getRole() == Role.SPORTIF) {
            if (courant.getId() != profil.getUtilisateurId()) {
                throw new AccesRefuseException("Vous ne pouvez modifier que votre propre profil.");
            }
            // Un sportif ne choisit pas lui-même son coach
            profil.setCoachId(profilDao.trouverParSportif(courant.getId())
                    .map(ProfilSportif::getCoachId).orElse(null));
        }
        validerProfil(profil);
        profilDao.modifier(profil);
    }

    public Optional<ProfilSportif> trouverProfil(int sportifId) {
        Session.getUtilisateur();
        return profilDao.trouverParSportif(sportifId);
    }

    /** Coach assigné à un sportif (consultable par le sportif lui-même, son coach ou l'admin). */
    public Optional<Utilisateur> coachDe(int sportifId) {
        new AccesService().verifierAccesSportif(sportifId);
        return profilDao.trouverParSportif(sportifId)
                .map(ProfilSportif::getCoachId)
                .flatMap(utilisateurDao::trouverParId);
    }

    /** On désactive au lieu de supprimer pour conserver l'historique. */
    public void changerStatut(int id, boolean actif) {
        Utilisateur admin = Session.exiger(Role.ADMIN);
        if (admin.getId() == id && !actif) {
            throw new ValidationException("Vous ne pouvez pas désactiver votre propre compte.");
        }
        utilisateurDao.changerStatut(id, actif);
    }

    public void reinitialiserMotDePasse(int id, String nouveau) {
        Session.exiger(Role.ADMIN);
        AuthService.verifierRobustesse(nouveau);
        utilisateurDao.modifierMotDePasse(id, AuthService.hacher(nouveau));
    }

    private void preparer(Utilisateur u, String motDePasse) {
        validerIdentite(u);
        AuthService.verifierRobustesse(motDePasse);
        u.setMotDePasseHash(AuthService.hacher(motDePasse));
        u.setActif(true);
    }

    private void validerIdentite(Utilisateur u) {
        if (u.getNom() == null || u.getNom().isBlank() || u.getPrenom() == null || u.getPrenom().isBlank()) {
            throw new ValidationException("Le nom et le prénom sont obligatoires.");
        }
        if (u.getEmail() == null || !EMAIL.matcher(u.getEmail().trim()).matches()) {
            throw new ValidationException("L'adresse email n'est pas valide.");
        }
        u.setNom(u.getNom().trim());
        u.setPrenom(u.getPrenom().trim());
        u.setEmail(u.getEmail().trim().toLowerCase());
        if (utilisateurDao.emailExiste(u.getEmail(), u.getId())) {
            throw new ValidationException("Cette adresse email est déjà utilisée.");
        }
    }

    private void validerProfil(ProfilSportif p) {
        if (p.getSexe() == null || p.getObjectif() == null || p.getNiveauActivite() == null
                || p.getDateNaissance() == null) {
            throw new ValidationException("Tous les champs du profil sont obligatoires.");
        }
        int age = Period.between(p.getDateNaissance(), LocalDate.now()).getYears();
        if (age < 14 || age > 100) {
            throw new ValidationException("L'âge doit être compris entre 14 et 100 ans.");
        }
        if (p.getTailleCm() < 100 || p.getTailleCm() > 250) {
            throw new ValidationException("La taille doit être comprise entre 100 et 250 cm.");
        }
        if (p.getCoachId() != null) {
            Utilisateur coach = utilisateurDao.trouverParId(p.getCoachId())
                    .orElseThrow(() -> new ValidationException("Coach introuvable."));
            if (coach.getRole() != Role.COACH || !coach.isActif()) {
                throw new ValidationException("Le coach choisi n'est pas un coach actif.");
            }
        }
    }
}
