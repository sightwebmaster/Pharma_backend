package com.pharmaApp.treatement.domain.event;

import lombok.Getter;

import java.time.LocalDateTime;



public record PriseConfirmeeEvent(
        String        priseId,
        String        traitementId,
        String        patientUserId,
        String        medicamentNom,
        LocalDateTime heureReelle,
        LocalDateTime occurredAt
) {}