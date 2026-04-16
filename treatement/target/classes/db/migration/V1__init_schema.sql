CREATE TABLE IF NOT EXISTS traitement (
                                          id                  VARCHAR(36)  NOT NULL,
    patient_user_id     VARCHAR(36)  NOT NULL,
    pharmacien_user_id  VARCHAR(36)  NOT NULL,
    statut              VARCHAR(20)  NOT NULL DEFAULT 'ACTIF',
    date_debut          DATE         NOT NULL,
    date_fin            DATE         NOT NULL,
    motif               VARCHAR(500),
    notes_pharmacien    TEXT,
    version             INT          DEFAULT 0,
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME,
    PRIMARY KEY (id),
    INDEX idx_traitement_patient_statut (patient_user_id, statut),
    INDEX idx_traitement_pharmacien     (pharmacien_user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ✅ Clé primaire SIMPLE id — compatible avec LigneMedicamentEntity.id
CREATE TABLE IF NOT EXISTS ligne_medicament (
                                                id                  VARCHAR(36)  NOT NULL,
    traitement_id       VARCHAR(36)  NOT NULL,
    medicament_id       VARCHAR(36)  NULL,
    medicament_nom      VARCHAR(200) NOT NULL,
    principe_actif      VARCHAR(200),
    dosage              VARCHAR(100) NOT NULL,
    frequence_par_jour  INT          NOT NULL,
    duree_jours         INT          NOT NULL,
    instructions        TEXT,
    created_at          DATETIME     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_ligne_traitement_id (traitement_id),
    CONSTRAINT fk_ligne_traitement
    FOREIGN KEY (traitement_id) REFERENCES traitement(id)
    ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ✅ FK simple sur ligne_id
CREATE TABLE IF NOT EXISTS ligne_heures_prise (
                                                  ligne_id    VARCHAR(36) NOT NULL,
    heure_prise TIME        NOT NULL,
    PRIMARY KEY (ligne_id, heure_prise),
    CONSTRAINT fk_ligne_heures
    FOREIGN KEY (ligne_id) REFERENCES ligne_medicament(id)
    ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ✅ FK simple sur ligne_medicament_id
-- ✅ AJOUT des colonnes: dosage, duree_jours, medicament_nom
CREATE TABLE IF NOT EXISTS prise_planifiee (
                                               id                  VARCHAR(36)  NOT NULL,
    treaitement_id      VARCHAR(36)  NOT NULL,
    ligne_medicament_id VARCHAR(36)  NOT NULL,
    patient_user_id     VARCHAR(36)  NOT NULL,
    heure_prevue        DATETIME     NOT NULL,
    heure_reelle        DATETIME,
    statut              VARCHAR(20)  NOT NULL DEFAULT 'PLANIFIEE',
    source_confirmation VARCHAR(20),
    dosage              VARCHAR(100),  -- ✅ NOUVELLE COLONNE
    duree_jours         INT,           -- ✅ NOUVELLE COLONNE
    medicament_nom      VARCHAR(200),  -- ✅ NOUVELLE COLONNE
    instructions        TEXT,                              -- ✅ NOUVELLE COLONNE
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME,
    PRIMARY KEY (id),
    INDEX idx_prise_statut_heure   (statut, heure_prevue),
    INDEX idx_prise_patient_statut (patient_user_id, statut),
    CONSTRAINT fk_prise_traitement
    FOREIGN KEY (treaitement_id) REFERENCES traitement(id)
    ON DELETE CASCADE,
    CONSTRAINT fk_prise_ligne
    FOREIGN KEY (ligne_medicament_id) REFERENCES ligne_medicament(id)
    ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS treaitement_audit (
                                                 id               VARCHAR(36) NOT NULL,
    treaitement_id   VARCHAR(36) NOT NULL,
    ancien_statut    VARCHAR(50),
    nouveau_statut   VARCHAR(50) NOT NULL,
    modifie_par_id   VARCHAR(36) NOT NULL,
    modifie_par_role VARCHAR(20) NOT NULL,
    motif_changement TEXT,
    created_at       DATETIME    NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_audit_traitement_id (treaitement_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS outbox_events (
                                             id             VARCHAR(36)  NOT NULL,
    aggregate_id   VARCHAR(36)  NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSON         NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at     DATETIME     NOT NULL,
    processed_at   DATETIME,
    retry_count    INT          DEFAULT 0,
    error_message  TEXT,
    PRIMARY KEY (id),
    INDEX idx_outbox_status  (status),
    INDEX idx_outbox_created (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;