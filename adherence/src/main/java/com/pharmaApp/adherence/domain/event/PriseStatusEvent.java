package com.pharmaApp.adherence.domain.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
        String traitementId,   // ✅ String (UUID) — pas Long
        String patientUserId,
        String medicamentNom,
        String statut           // "CONFIRMEE" | "MANQUEE"
) {}