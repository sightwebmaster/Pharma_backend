package com.pharmaApp.adherence.domain.exception;

public class AdherenceRecordNotFoundException extends RuntimeException {
    public AdherenceRecordNotFoundException(String patientUserId, String traitementId) {
        super("Aucun enregistrement d'observance trouvé pour patient=" + patientUserId
                + " traitement=" + traitementId);
    }
}
