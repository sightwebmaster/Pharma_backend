package com.pharmaApp.treatement.domain.event;

import java.time.LocalDateTime;

public record PriseManqueeEvent(
        String        priseId,
        String        traitementId,
        String        patientUserId,
        String        medicamentNom,
        LocalDateTime heurePrevue,
        LocalDateTime occurredAt
) {}