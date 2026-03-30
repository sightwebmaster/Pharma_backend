package com.pharmaApp.adherence.domain.exception;

public class AdherenceRecordNotFoundException extends RuntimeException {
    public AdherenceRecordNotFoundException(String patientUserId, Long traitementId) {
        super("Aucun enregistrement d'observance trouvé pour patient=" + patientUserId
              + " traitement=" + traitementId);
    }
}
