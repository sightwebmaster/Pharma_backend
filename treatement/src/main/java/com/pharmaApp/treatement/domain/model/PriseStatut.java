package com.pharmaApp.treatement.domain.model;

/**
 * Statuts possibles d'une prise planifiée.
 *
 * Cycle de vie :
 *   PLANIFIEE → CONFIRMEE  (patient confirme dans l'app)
 *   PLANIFIEE → MANQUEE    (scheduler détecte dépassement de 30 min)
 *
 * CONFIRMEE et MANQUEE sont des états finaux :
 * une prise ne peut pas revenir à PLANIFIEE.
 */
public enum PriseStatut {
    PLANIFIEE,
    CONFIRMEE,
    MANQUEE
}