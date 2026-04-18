ALTER TABLE rappels
    ADD COLUMN heure_reference DATETIME NULL AFTER heure_envoi;

UPDATE rappels
SET heure_reference = heure_envoi
WHERE heure_reference IS NULL;

ALTER TABLE rappels
    MODIFY heure_reference DATETIME NOT NULL;

CREATE INDEX idx_rappel_reference ON rappels (patient_user_id, medicament_nom, heure_reference);
