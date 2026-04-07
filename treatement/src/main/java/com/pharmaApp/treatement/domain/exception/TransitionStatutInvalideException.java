package com.pharmaApp.treatement.domain.exception;

import com.pharmaApp.treatement.domain.model.TraitementStatut;

/**
 * Exception — Transition de statut invalide (Règle R3)
 *
 * Levée par Traitement.changerStatut() si la transition demandée
 * n'est pas autorisée par le domaine.
 * Ex : tenter de repasser un traitement TERMINE à ACTIF.
 */
public class TransitionStatutInvalideException extends RuntimeException {

    public TransitionStatutInvalideException(
            TraitementStatut actuel,
            TraitementStatut cible) {
        super("Transition interdite : ["
                + actuel + "] → [" + cible + "]. "
                + "Vérifiez les transitions autorisées dans TraitementStatut.");
    }
}