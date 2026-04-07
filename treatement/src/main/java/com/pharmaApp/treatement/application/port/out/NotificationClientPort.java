package com.pharmaApp.treatement.application.port.out;

public interface NotificationClientPort {

    /**
     * Planifie les rappels FCM pour un traitement créé.
     * Appelé après TraitementCreeEvent.
     */
    void planifierRappels(String traitementId, String patientUserId, int nombrePrises);

    /**
     * Envoie une alerte au proche du patient (prise manquée + taux critique).
     * Appelé après PriseManqueeEvent si taux < 70%.
     */
    void envoyerAlerteProcheManquee(String patientUserId, String medicamentNom);

    /**
     * Met à jour ou annule les rappels existants après modification du traitement.
     * Appelé après TraitementModifieEvent.
     */
    void mettreAJourRappels(String traitementId, String nouveauStatut);
}