package com.pharmaApp.treatement.domain.event;

import lombok.Getter;

import java.time.LocalDateTime;



public record PriseConfirmeeEvent(
        String        priseId,
        String        traitementId,
        String        patientUserId,
        String        pharmacienUserId,   // ✅ ajouter
        String        medicamentNom,
        String        dosage,              // ✅ ajouter
        LocalDateTime heurePrevue,         // ✅ ajouter
        LocalDateTime heureReelle,
        LocalDateTime occurredAt
) {}