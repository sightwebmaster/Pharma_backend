package com.pharmaApp.treatement.domain.model;

/**
 * Statuts possibles d'un traitement.
 *
 * Transitions autorisées (Règle R3) :
 *   ACTIF  → SUSPENDU   (pharmacien suspend temporairement)
 *   ACTIF  → TERMINE    (fin normale ou arrêt)
 *   SUSPENDU → ACTIF    (reprise du traitement)
 *   SUSPENDU → TERMINE  (arrêt définitif depuis suspension)
 *
 * Transitions interdites :
 *   TERMINE → tout autre statut  (état final irréversible)
 */
public enum TraitementStatut {
    ACTIF,
    SUSPENDU,
    TERMINE;

    /**
     * Vérifie si la transition vers le statut cible est autorisée.
     * Appelée dans Traitement.changerStatut() — règle R3.
     */
    public boolean peutTransitionnerVers(TraitementStatut cible) {
        return switch (this) {
            case ACTIF     -> cible == SUSPENDU || cible == TERMINE;
            case SUSPENDU  -> cible == ACTIF    || cible == TERMINE;
            case TERMINE   -> false; // état final, aucune transition possible
        };
    }
}