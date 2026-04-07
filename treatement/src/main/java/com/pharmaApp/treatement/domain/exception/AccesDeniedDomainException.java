package com.pharmaApp.treatement.domain.exception;

/**
 * Exception — Accès refusé par le domaine (Règle R2)
 *
 * Levée quand un acteur tente une action qui ne lui appartient pas.
 * Ex : un patient essaie de confirmer la prise d'un autre patient.
 *
 * Distincte de l'exception Spring Security (401/403) :
 * celle-ci vient du métier, pas de l'infrastructure.
 */
public class AccesDeniedDomainException extends RuntimeException {

    public AccesDeniedDomainException(String message) {
        super(message);
    }
}