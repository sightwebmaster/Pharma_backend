

package com.pharmaApp.treatement.application.port.out;


import java.util.List;
import java.util.Optional;

public interface MedicationClientPort {

    /**
     * Retourne les contre-indications d'un médicament depuis medication-service.
     *
     * Comportement en cas d'indisponibilité (Fail-Open) :
     * → L'implémentation logue un WARNING et retourne une liste vide.
     * → Le traitement est créé sans vérification de contre-indications.
     * → Ce comportement est documenté et délibéré (choix métier).
     */
    List<String> getContreIndications(String medicamentId);
    Optional<String> getMedicamentIdByNom(String nom);
}