package com.pharmaApp.treatement.domain.exception;

import lombok.Getter;

/**
 * Exception — Conflit de traitement (Règle R4)
 *
 * Levée par Traitement.creer() dans deux cas :
 *
 * Cas 1 — Conflit interne (même traitement) :
 *   Deux lignes dans le même traitement partagent le même principe actif.
 *   Ex : Doliprane (paracétamol) + Efferalgan (paracétamol) → conflit.
 *
 * Cas 2 — Conflit externe (avec un traitement actif existant) :
 *   Un traitement actif du même patient contient déjà ce principe actif.
 *   Ex : Patient a déjà Ibuprofène actif → nouveau traitement Ibuprofène → conflit.
 */
@Getter
public class ConflitTraitementException extends RuntimeException {

    private final String principeActifEnConflit;
    private final String contexte; // "INTERNE" ou ID du traitement existant

    public ConflitTraitementException(String principeActif, String contexte) {
        super("Conflit détecté pour le principe actif [" + principeActif + "] — " + contexte);
        this.principeActifEnConflit = principeActif;
        this.contexte               = contexte;
    }

}