// ─────────────────────────────────────────────────────────────
// FICHIER 1 : MedicationClientAdapter.java
// ─────────────────────────────────────────────────────────────
package com.pharmaApp.treatement.infrastructure.adapter.out.client;

import com.pharmaApp.treatement.application.port.out.AdherenceClientPort;
import com.pharmaApp.treatement.application.port.out.MedicationClientPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stub temporaire — medication-service pas encore développé.
 * Fail-Open : retourne toujours une liste vide (aucune contre-indication).
 * À remplacer par un Feign Client quand medication-service sera prêt.
 */
@Slf4j
@Component
public class MedicationClientAdapter implements MedicationClientPort {

    @Override
    public List<String> getContreIndications(String medicamentId) {
        log.warn("MedicationClient STUB — medicamentId={} — retourne [] (Fail-Open)", medicamentId);
        return List.of();
    }
}

