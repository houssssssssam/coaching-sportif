-- =====================================================================
-- Coaching sportif personnalisé - Script de création de la base
-- SGBD : MySQL 8.x
-- =====================================================================

SET NAMES utf8mb4;
USE coaching_sportif;

DROP TABLE IF EXISTS repas;
DROP TABLE IF EXISTS aliment;
DROP TABLE IF EXISTS mesure;
DROP TABLE IF EXISTS seance;
DROP TABLE IF EXISTS affectation;
DROP TABLE IF EXISTS programme_exercice;
DROP TABLE IF EXISTS programme;
DROP TABLE IF EXISTS exercice;
DROP TABLE IF EXISTS profil_sportif;
DROP TABLE IF EXISTS utilisateur;

-- ---------------------------------------------------------------------
-- Utilisateurs (3 rôles : ADMIN, COACH, SPORTIF)
-- ---------------------------------------------------------------------
CREATE TABLE utilisateur (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    nom               VARCHAR(60)  NOT NULL,
    prenom            VARCHAR(60)  NOT NULL,
    email             VARCHAR(120) NOT NULL UNIQUE,
    mot_de_passe_hash VARCHAR(255) NOT NULL,
    role              ENUM('ADMIN', 'COACH', 'SPORTIF') NOT NULL,
    actif             BOOLEAN      NOT NULL DEFAULT TRUE,
    date_creation     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Profil du sportif (données nécessaires au calcul des besoins caloriques)
-- ---------------------------------------------------------------------
CREATE TABLE profil_sportif (
    utilisateur_id  INT PRIMARY KEY,
    coach_id        INT NULL,
    sexe            ENUM('H', 'F') NOT NULL,
    date_naissance  DATE NOT NULL,
    taille_cm       DECIMAL(5, 1) NOT NULL,
    objectif        ENUM('PERTE_POIDS', 'MAINTIEN', 'PRISE_MASSE') NOT NULL,
    niveau_activite ENUM('SEDENTAIRE', 'LEGER', 'MODERE', 'ACTIF', 'TRES_ACTIF') NOT NULL,
    CONSTRAINT fk_profil_utilisateur FOREIGN KEY (utilisateur_id)
        REFERENCES utilisateur (id) ON DELETE CASCADE,
    CONSTRAINT fk_profil_coach FOREIGN KEY (coach_id)
        REFERENCES utilisateur (id) ON DELETE SET NULL,
    CONSTRAINT chk_taille CHECK (taille_cm BETWEEN 100 AND 250)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Catalogue des exercices
-- ---------------------------------------------------------------------
CREATE TABLE exercice (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    nom               VARCHAR(100) NOT NULL UNIQUE,
    groupe_musculaire VARCHAR(50)  NOT NULL,
    type              ENUM('FORCE', 'CARDIO', 'MOBILITE') NOT NULL,
    description       TEXT NULL
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Programmes d'entraînement (créés par un coach)
-- ---------------------------------------------------------------------
CREATE TABLE programme (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    nom           VARCHAR(100) NOT NULL,
    description   TEXT NULL,
    objectif      ENUM('PERTE_POIDS', 'MAINTIEN', 'PRISE_MASSE') NOT NULL,
    niveau        ENUM('DEBUTANT', 'INTERMEDIAIRE', 'AVANCE') NOT NULL,
    coach_id      INT NOT NULL,
    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_programme_coach FOREIGN KEY (coach_id)
        REFERENCES utilisateur (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Contenu d'un programme : quels exercices, quel jour, combien
-- ---------------------------------------------------------------------
CREATE TABLE programme_exercice (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    programme_id INT NOT NULL,
    exercice_id  INT NOT NULL,
    jour         TINYINT NOT NULL,
    ordre        TINYINT NOT NULL DEFAULT 1,
    series       TINYINT NULL,
    repetitions  SMALLINT NULL,
    charge_kg    DECIMAL(5, 1) NULL,
    duree_min    SMALLINT NULL,
    consigne     VARCHAR(255) NULL,
    CONSTRAINT fk_pe_programme FOREIGN KEY (programme_id)
        REFERENCES programme (id) ON DELETE CASCADE,
    CONSTRAINT fk_pe_exercice FOREIGN KEY (exercice_id)
        REFERENCES exercice (id),
    CONSTRAINT chk_jour CHECK (jour BETWEEN 1 AND 7)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Affectation d'un programme à un sportif
-- Règle : un seul programme ACTIF par sportif (garanti aussi par la base
-- grâce à la colonne générée sportif_actif + index unique)
-- ---------------------------------------------------------------------
CREATE TABLE affectation (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    sportif_id     INT NOT NULL,
    programme_id   INT NOT NULL,
    date_debut     DATE NOT NULL,
    date_fin       DATE NULL,
    statut         ENUM('ACTIF', 'TERMINE') NOT NULL DEFAULT 'ACTIF',
    sportif_actif  INT GENERATED ALWAYS AS (IF(statut = 'ACTIF', sportif_id, NULL)) STORED,
    -- Pas de ON DELETE CASCADE ici : MySQL l'interdit sur une colonne
    -- utilisée par une colonne générée (sportif_actif). On désactive les
    -- comptes (utilisateur.actif) plutôt que de les supprimer.
    CONSTRAINT fk_aff_sportif FOREIGN KEY (sportif_id)
        REFERENCES utilisateur (id),
    CONSTRAINT fk_aff_programme FOREIGN KEY (programme_id)
        REFERENCES programme (id),
    CONSTRAINT uq_un_programme_actif UNIQUE (sportif_actif)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Séances réalisées par le sportif
-- ---------------------------------------------------------------------
CREATE TABLE seance (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    sportif_id   INT NOT NULL,
    programme_id INT NULL,
    date_seance  DATE NOT NULL,
    duree_min    SMALLINT NOT NULL,
    ressenti     TINYINT NOT NULL,
    commentaire  VARCHAR(255) NULL,
    CONSTRAINT fk_seance_sportif FOREIGN KEY (sportif_id)
        REFERENCES utilisateur (id) ON DELETE CASCADE,
    CONSTRAINT fk_seance_programme FOREIGN KEY (programme_id)
        REFERENCES programme (id) ON DELETE SET NULL,
    CONSTRAINT chk_ressenti CHECK (ressenti BETWEEN 1 AND 5),
    CONSTRAINT chk_duree CHECK (duree_min > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Mesures corporelles (une par jour maximum)
-- ---------------------------------------------------------------------
CREATE TABLE mesure (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    sportif_id        INT NOT NULL,
    date_mesure       DATE NOT NULL,
    poids_kg          DECIMAL(5, 1) NOT NULL,
    tour_taille_cm    DECIMAL(5, 1) NULL,
    masse_grasse_pct  DECIMAL(4, 1) NULL,
    CONSTRAINT fk_mesure_sportif FOREIGN KEY (sportif_id)
        REFERENCES utilisateur (id) ON DELETE CASCADE,
    CONSTRAINT uq_mesure_jour UNIQUE (sportif_id, date_mesure),
    CONSTRAINT chk_poids CHECK (poids_kg BETWEEN 30 AND 300)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Catalogue des aliments (valeurs pour 100 g)
-- cree_par NULL  = aliment du catalogue officiel (géré par l'admin)
-- cree_par <> NULL = aliment personnalisé créé par un sportif
-- ---------------------------------------------------------------------
CREATE TABLE aliment (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    nom            VARCHAR(100) NOT NULL,
    categorie      VARCHAR(40)  NOT NULL,
    calories_100g  DECIMAL(6, 1) NOT NULL,
    proteines_100g DECIMAL(5, 1) NOT NULL,
    glucides_100g  DECIMAL(5, 1) NOT NULL,
    lipides_100g   DECIMAL(5, 1) NOT NULL,
    cree_par       INT NULL,
    CONSTRAINT fk_aliment_createur FOREIGN KEY (cree_par)
        REFERENCES utilisateur (id) ON DELETE CASCADE,
    CONSTRAINT chk_valeurs CHECK (calories_100g >= 0 AND proteines_100g >= 0
                                  AND glucides_100g >= 0 AND lipides_100g >= 0),
    INDEX idx_aliment_nom (nom)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Journal alimentaire : une ligne = un aliment consommé à un repas
-- ---------------------------------------------------------------------
CREATE TABLE repas (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    sportif_id  INT NOT NULL,
    date_repas  DATE NOT NULL,
    type_repas  ENUM('PETIT_DEJEUNER', 'DEJEUNER', 'DINER', 'COLLATION') NOT NULL,
    aliment_id  INT NOT NULL,
    quantite_g  DECIMAL(6, 1) NOT NULL,
    CONSTRAINT fk_repas_sportif FOREIGN KEY (sportif_id)
        REFERENCES utilisateur (id) ON DELETE CASCADE,
    CONSTRAINT fk_repas_aliment FOREIGN KEY (aliment_id)
        REFERENCES aliment (id),
    CONSTRAINT chk_quantite CHECK (quantite_g > 0),
    INDEX idx_repas_jour (sportif_id, date_repas)
) ENGINE = InnoDB;
