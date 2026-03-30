package com.pharmaApp.adherence.domain.model;

/**
 * Statut d'une prise médicamenteuse.
 * Publié par treatment-service via événement Kafka.
 */
public enum StatutPrise {
    /** Patient a confirmé avoir pris le médicament */
    CONFIRME,
    /** Heure dépassée, prise non confirmée (détectée par le Scheduler du treatment-service) */
    MANQUE
}
