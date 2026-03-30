package com.pharmaApp.adherence.domain.model;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Aggregate Root — Observance
 *
 * Représente l'état d'observance d'un patient pour un traitement donné.
 * Contient l'historique de toutes ses prises + le taux calculé.
 *
 * Règle DDD : toute modification passe par cet agrégat.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdherenceRecord {

    private Long id;

    /** Identifiant du patient (userId depuis auth-service) */
    private String patientUserId;

    /** Identifiant du traitement (treatment-service) */
    private Long traitementId;

    /** Identifiant du pharmacien responsable */
    private String pharmacienUserId;

    /** Toutes les entrées d'historique (prises confirmées + manquées) */
    private List<HistoriqueEntry> entries;

    // ── Taux calculés ─────────────────────────────────────────

    /** Taux sur les 7 derniers jours (%) */
    private double taux7j;

    /** Taux sur les 30 derniers jours (%) */
    private double taux30j;

    /** Taux sur les 90 derniers jours (%) */
    private double taux90j;

    /** Taux global depuis le début du traitement */
    private double tauxGlobal;

    /** Nombre de prises manquées consécutives (règle métier clé) */
    private int consecutiveMissed;

    /** Date du dernier calcul */
    private LocalDate lastCalculated;

    // ── Règles métier ─────────────────────────────────────────

    /**
     * Calcule les taux d'observance sur toutes les fenêtres temporelles.
     * Appelée après chaque ajout d'entrée.
     */
    public void recalculateTaux() {
        LocalDate today = LocalDate.now();

        this.taux7j     = calculerTaux(today.minusDays(7),  today);
        this.taux30j    = calculerTaux(today.minusDays(30), today);
        this.taux90j    = calculerTaux(today.minusDays(90), today);
        this.tauxGlobal = calculerTauxGlobal();
        this.consecutiveMissed = compterConsecutifManques();
        this.lastCalculated    = today;
    }

    /**
     * Calcule le taux d'observance sur une fenêtre donnée.
     * Taux = (prises confirmées / total prises attendues) * 100
     */
    private double calculerTaux(LocalDate debut, LocalDate fin) {
        if (entries == null || entries.isEmpty()) return 0.0;

        long total = entries.stream()
                .filter(e -> !e.getDatePrise().isBefore(debut)
                          && !e.getDatePrise().isAfter(fin))
                .count();

        if (total == 0) return 100.0; // pas de prises attendues = 100%

        long confirmees = entries.stream()
                .filter(e -> !e.getDatePrise().isBefore(debut)
                          && !e.getDatePrise().isAfter(fin)
                          && e.getStatut() == StatutPrise.CONFIRME)
                .count();

        return Math.round((confirmees * 100.0 / total) * 10.0) / 10.0;
    }

    private double calculerTauxGlobal() {
        if (entries == null || entries.isEmpty()) return 100.0;
        long total      = entries.size();
        long confirmees = entries.stream()
                .filter(e -> e.getStatut() == StatutPrise.CONFIRME)
                .count();
        return Math.round((confirmees * 100.0 / total) * 10.0) / 10.0;
    }

    /**
     * Règle métier clé :
     * Compte les prises manquées consécutives les plus récentes.
     * Si >= 3 → déclenche AlerteConsecutiveMissed.
     */
    private int compterConsecutifManques() {
        if (entries == null || entries.isEmpty()) return 0;
        int count = 0;
        // Parcourir du plus récent au plus ancien
        List<HistoriqueEntry> sorted = entries.stream()
                .sorted((a, b) -> b.getDatePrise().compareTo(a.getDatePrise()))
                .toList();
        for (HistoriqueEntry entry : sorted) {
            if (entry.getStatut() == StatutPrise.MANQUE) {
                count++;
            } else {
                break; // chaîne interrompue
            }
        }
        return count;
    }

    /** Vérifie si le taux d'observance est sous le seuil critique */
    public boolean isTauxCritique(double seuil) {
        // On évalue sur 7j en priorité, sinon global
        double taux = entries != null && entries.size() >= 5 ? taux7j : tauxGlobal;
        return taux < seuil && entries != null && entries.size() >= 3;
    }
}
