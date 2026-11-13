-- Schéma PlanSécu (MySQL 8)

CREATE TABLE agent (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom                  VARCHAR(100) NOT NULL,
    prenom               VARCHAR(100) NOT NULL,
    telephone            VARCHAR(20)  NOT NULL,
    email                VARCHAR(150),
    carte_pro_numero     VARCHAR(50),
    carte_pro_expiration DATE,
    couleur              VARCHAR(7)   NOT NULL DEFAULT '#3b82f6',
    actif                BOOLEAN      NOT NULL DEFAULT TRUE,
    notif_sms            BOOLEAN      NOT NULL DEFAULT TRUE,
    notif_email          BOOLEAN      NOT NULL DEFAULT TRUE,
    recoit_hebdo         BOOLEAN      NOT NULL DEFAULT TRUE,
    recoit_mensuel       BOOLEAN      NOT NULL DEFAULT FALSE,
    calendrier_token     VARCHAR(40)  NOT NULL,
    cree_le              DATETIME     NOT NULL,
    CONSTRAINT uk_agent_token UNIQUE (calendrier_token)
);

CREATE TABLE site (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom               VARCHAR(150) NOT NULL,
    adresse           VARCHAR(255) NOT NULL,
    code_postal       VARCHAR(10),
    ville             VARCHAR(100),
    client            VARCHAR(150),
    contact_nom       VARCHAR(150),
    contact_telephone VARCHAR(20),
    consignes         VARCHAR(2000),
    actif             BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE utilisateur (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    role     VARCHAR(20)  NOT NULL,
    actif    BOOLEAN      NOT NULL DEFAULT TRUE,
    agent_id BIGINT,
    CONSTRAINT uk_utilisateur_username UNIQUE (username),
    CONSTRAINT uk_utilisateur_agent UNIQUE (agent_id),
    CONSTRAINT fk_utilisateur_agent FOREIGN KEY (agent_id) REFERENCES agent (id) ON DELETE CASCADE
);

CREATE TABLE mission (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    date_mission DATE        NOT NULL,
    agent_id     BIGINT      NOT NULL,
    site_id      BIGINT      NOT NULL,
    statut       VARCHAR(20) NOT NULL,
    commentaire  VARCHAR(500),
    cree_le      DATETIME    NOT NULL,
    modifie_le   DATETIME    NOT NULL,
    CONSTRAINT fk_mission_agent FOREIGN KEY (agent_id) REFERENCES agent (id),
    CONSTRAINT fk_mission_site FOREIGN KEY (site_id) REFERENCES site (id)
);
CREATE INDEX idx_mission_date ON mission (date_mission);
CREATE INDEX idx_mission_agent_date ON mission (agent_id, date_mission);

-- Un créneau = une plage horaire. Une mission discontinue (9h-12h puis 15h-18h) a 2 créneaux.
-- heure_fin <= heure_debut signifie que le créneau finit le lendemain (ex. 21h-6h).
CREATE TABLE creneau (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    mission_id  BIGINT NOT NULL,
    heure_debut TIME   NOT NULL,
    heure_fin   TIME   NOT NULL,
    CONSTRAINT fk_creneau_mission FOREIGN KEY (mission_id) REFERENCES mission (id) ON DELETE CASCADE
);

CREATE TABLE absence (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id   BIGINT      NOT NULL,
    date_debut DATE        NOT NULL,
    date_fin   DATE        NOT NULL,
    type       VARCHAR(20) NOT NULL,
    motif      VARCHAR(500),
    CONSTRAINT fk_absence_agent FOREIGN KEY (agent_id) REFERENCES agent (id) ON DELETE CASCADE
);
CREATE INDEX idx_absence_dates ON absence (date_debut, date_fin);

CREATE TABLE notification (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id     BIGINT,
    destinataire VARCHAR(150) NOT NULL,
    canal        VARCHAR(10)  NOT NULL,
    sujet        VARCHAR(200),
    message      VARCHAR(4000) NOT NULL,
    statut       VARCHAR(20)  NOT NULL,
    erreur       VARCHAR(500),
    envoye_le    DATETIME     NOT NULL,
    CONSTRAINT fk_notification_agent FOREIGN KEY (agent_id) REFERENCES agent (id) ON DELETE SET NULL
);
CREATE INDEX idx_notification_date ON notification (envoye_le);
