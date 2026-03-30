package com.pharmaApp.treatement.domain.event;

import java.time.LocalDateTime;

public record TraitementModifieEvent(
        String        traitementId,
        String        patientUserId,
        String        ancienStatut,
        String        nouveauStatut,
        String        motif,
        LocalDateTime occurredAt
) {}
