package com.pharmaApp.treatement.domain.event;

import java.time.LocalDateTime;

// =============================================================
// PharmaCare — Domain Events du treatment-service
//
// Ces records sont des faits immuables : quelque chose qui
// s'est passé dans le domaine. Ils sont produits par
// l'Aggregate Root (Traitement) et consommés par le
// TraitementService pour déclencher les appels aux autres
// services (Notification, Adherence).
//
// Un record Java = immuable + equals/hashCode/toString gratuits.
// Aucune annotation Spring — domaine pur.
// =============================================================

/**
 * Publié quand un pharmacien crée un nouveau traitement.
 * → TraitementService l'utilise pour appeler NotificationClient
 *   afin de planifier les rappels FCM.
 *
 * @param traitementId      ID du traitement créé
 * @param patientUserId     ID du patient concerné
 * @param pharmacienUserId  ID du pharmacien prescripteur
 * @param nombrePrises      Nombre total de prises planifiées (pour info notification)
 * @param occurredAt        Moment de l'événement
 */
public record TraitementCreeEvent(
        String        traitementId,
        String        patientUserId,
        String        pharmacienUserId,
        int           nombrePrises,
        LocalDateTime occurredAt
) {}
