package com.pharmaApp.treatement.application.port.out;

import com.pharmaApp.treatement.domain.model.PrisePlanifiee;

import java.time.LocalDateTime;
import java.util.List;

public interface PriseRepositoryPort {

    /** Sauvegarde l'état d'une prise (CONFIRMEE ou MANQUEE) */
    PrisePlanifiee save(PrisePlanifiee prise);

    List<PrisePlanifiee> findPrisesAujourdhui(String patientUserId);

    List<PrisePlanifiee> findToutesLesPrises(String patientUserId);

    /**
     * Requête du Scheduler — retourne toutes les prises PLANIFIÉES
     * dont l'heure prévue est antérieure à la deadline.
     * deadline = now() - toleranceMinutes
     */
    List<PrisePlanifiee> findPrisesNonConfirmees(LocalDateTime deadline);
}
