package com.pharmaApp.adherence.domain.model;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdherenceRecord {

    private Long id;

    private String patientUserId;

    // ✅ String UUID — aligné avec treatment-service
    private String traitementId;

    private String pharmacienUserId;

    private List<HistoriqueEntry> entries;

    private double taux7j;
    private double taux30j;
    private double taux90j;
    private double tauxGlobal;
    private int consecutiveMissed;
    private LocalDate lastCalculated;

    // ── Règles métier ─────────────────────────────────────────

    public void recalculateTaux() {
        LocalDate today = LocalDate.now();
        this.taux7j     = calculerTaux(today.minusDays(7),  today);
        this.taux30j    = calculerTaux(today.minusDays(30), today);
        this.taux90j    = calculerTaux(today.minusDays(90), today);
        this.tauxGlobal = calculerTauxGlobal();
        this.consecutiveMissed = compterConsecutifManques();
        this.lastCalculated    = today;
    }

    private double calculerTaux(LocalDate debut, LocalDate fin) {
        if (entries == null || entries.isEmpty()) return 100.0;
        long total = entries.stream()
                .filter(e -> !e.getDatePrise().isBefore(debut)
                        && !e.getDatePrise().isAfter(fin))
                .count();
        if (total == 0) return 100.0;
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

    private int compterConsecutifManques() {
        if (entries == null || entries.isEmpty()) return 0;
        int count = 0;
        List<HistoriqueEntry> sorted = entries.stream()
                .sorted((a, b) -> b.getDatePrise().compareTo(a.getDatePrise()))
                .toList();
        for (HistoriqueEntry entry : sorted) {
            if (entry.getStatut() == StatutPrise.MANQUE) count++;
            else break;
        }
        return count;
    }

    public boolean isTauxCritique(double seuil) {
        double taux = entries != null && entries.size() >= 5 ? taux7j : tauxGlobal;
        return taux < seuil && entries != null && entries.size() >= 3;
    }
}