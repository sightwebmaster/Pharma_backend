package com.pharmaApp.treatement.application.dto;

/**
 * Commande de confirmation d'une prise.
 *
 * patientId : extrait du JWT par le Controller.
 * La règle R2 (seul le bon patient confirme) est vérifiée
 * dans PrisePlanifiee.confirmer(patientId).
 */
public record ConfirmerPriseCommand(
        String traitementId,
        String priseId,
        String patientId
) {}