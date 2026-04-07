package com.pharmaApp.treatement.application.dto;

/**
 * Commande de modification du statut d'un traitement.
 *
 * acteurId   : ID du pharmacien (extrait du JWT)
 * acteurRole : "PHARMACIEN" (extrait du JWT) — R1 vérifiée par le domaine
 * nouveauStatut : "SUSPENDU" | "ACTIF" | "TERMINE" — R3 vérifiée par le domaine
 */
public record ModifierTraitementCommand(
        String traitementId,
        String acteurId,
        String acteurRole,
        String nouveauStatut,
        String motif
) {}