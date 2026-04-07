package com.pharmaApp.treatement.application.port.out;

public interface AdherenceClientPort {

    /**
     * Enregistre une entrée d'historique dans adherence-service.
     * statut : "CONFIRMEE" | "MANQUEE"
     * Retourne le taux d'observance recalculé (0-100).
     */
    int enregistrerEntree(String patientUserId, String priseId,
                          String medicamentNom, String statut);
}

