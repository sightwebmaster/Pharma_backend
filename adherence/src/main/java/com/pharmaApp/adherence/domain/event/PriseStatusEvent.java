package com.pharmaApp.adherence.domain.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * PriseStatusEvent — event consommé depuis treatment-service
 *
 * ⚠️ traitementId est String (UUID) — aligné avec treatment-service.
 * L'ancien code avait Long — corrigé ici.
 *
 * Topics consommés :
 *   - prise.confirmee  → statut = "CONFIRMEE"
 *   - prise.manquee    → statut = "MANQUEE"
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PriseStatusEvent(
        String priseId,
        String traitementId,
        String patientUserId,
        String pharmacienUserId,
        String medicamentNom,
        String dosage,
        LocalDateTime heurePrevue,
        LocalDateTime heureReelle,
        LocalDateTime occurredAt
) {}