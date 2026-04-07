package com.pharmaApp.treatement.application.port.out;


import com.pharmaApp.treatement.domain.model.Traitement;

import java.util.List;
import java.util.Optional;
import java.util.Set;


public interface TraitementRepositoryPort {

    /** Persiste un nouveau traitement + ses prises générées */
    Traitement save(Traitement traitement);

    /** Charge un traitement par son ID — lance NoSuchElementException si absent */
    Optional<Traitement> findById(String traitementId);

    /**
     * Retourne les principes actifs de tous les traitements ACTIFS du patient.
     * Utilisé par TraitementService pour la vérification R4 externe
     * avant d'appeler Traitement.creer().
     */
    Set<String> findPrincipesActifsActifs(String patientUserId);

    /** Tous les traitements actifs d'un patient (pour consultation) */
    List<Traitement> findActifsByPatient(String patientUserId);
}