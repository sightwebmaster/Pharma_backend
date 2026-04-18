-- =============================================================
-- PharmaCare — notification-service
-- V1__create_notification_tables.sql
-- =============================================================

CREATE TABLE IF NOT EXISTS rappels (
                                       id              VARCHAR(36)     NOT NULL PRIMARY KEY,
    traitement_id   VARCHAR(36)     NOT NULL,
    patient_user_id VARCHAR(36)     NOT NULL,
    medicament_nom  VARCHAR(200)    NOT NULL,
    heure_envoi     DATETIME        NOT NULL,
    statut          VARCHAR(20)     NOT NULL DEFAULT 'PLANIFIE',
    nb_tentatives   TINYINT         NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_rappel_prise UNIQUE (traitement_id, heure_envoi, patient_user_id, medicament_nom)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_rappel_statut_heure ON rappels (statut, heure_envoi);
CREATE INDEX idx_rappel_patient      ON rappels (patient_user_id);
CREATE INDEX idx_rappel_traitement   ON rappels (traitement_id);

CREATE TABLE IF NOT EXISTS tokens_fcm (
                                          id              VARCHAR(36)        NOT NULL PRIMARY KEY,
    user_id         VARCHAR(36)        NOT NULL,
    device_token    VARCHAR(500)    NOT NULL,
    plateforme      VARCHAR(10)     NOT NULL,
    actif           TINYINT(1)      NOT NULL DEFAULT 1,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_token UNIQUE (user_id, device_token)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_token_user ON tokens_fcm (user_id, actif);

CREATE TABLE IF NOT EXISTS notifications_envoyees (
                                                      id              VARCHAR(36)       NOT NULL PRIMARY KEY,
    user_id         VARCHAR(36)       NOT NULL,
    type            VARCHAR(30)     NOT NULL,
    titre           VARCHAR(200)    NOT NULL,
    corps           VARCHAR(500)    NOT NULL,
    statut          VARCHAR(10)     NOT NULL,
    fcm_message_id  VARCHAR(200),
    envoye_a        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_notif_user_date ON notifications_envoyees (user_id, envoye_a DESC);
