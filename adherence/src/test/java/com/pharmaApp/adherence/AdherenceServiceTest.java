package com.pharmaApp.adherence;

import com.pharmaApp.adherence.application.dto.request.EnregistrerPriseRequest;
import com.pharmaApp.adherence.application.dto.response.AdherenceRecordResponse;
import com.pharmaApp.adherence.application.mapper.AdherenceMapper;
import com.pharmaApp.adherence.application.service.AdherenceService;
import com.pharmaApp.adherence.domain.model.*;
import com.pharmaApp.adherence.domain.port.output.AdherenceRepository;
import com.pharmaApp.adherence.domain.port.output.EventPublisher;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires — AdherenceService (logique métier pure).
 * Pas de BDD, pas de Kafka — tout est mocké.
 */
@ExtendWith(MockitoExtension.class)
class AdherenceServiceTest {

    @Mock private AdherenceRepository adherenceRepository;
    @Mock private EventPublisher       eventPublisher;
    @Mock private AdherenceMapper      mapper;

    @InjectMocks private AdherenceService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "criticalThreshold",    70.0);
        ReflectionTestUtils.setField(service, "consecutiveMissedLimit", 3);
    }

    // ── UC1 ───────────────────────────────────────────────────

    @Test
    @DisplayName("UC1 — Enregistrer une prise CONFIRME crée un nouvel AdherenceRecord")
    void enregistrerPrise_confirme_createsRecord() {
        // GIVEN
        EnregistrerPriseRequest req = buildRequest("CONFIRME");
        when(adherenceRepository.findByPatientAndTraitement(any(), any()))
                .thenReturn(Optional.empty());
        when(adherenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(new AdherenceRecordResponse());

        // WHEN
        AdherenceRecordResponse result = service.enregistrerPrise(req);

        // THEN
        assertThat(result).isNotNull();
        verify(adherenceRepository).save(any(AdherenceRecord.class));
        verify(eventPublisher).publish(any(
                com.pharmaApp.adherence.domain.event.HistoriqueEnregistreEvent.class));
    }

    @Test
    @DisplayName("UC1 — 3 prises MANQUE consécutives publie AlerteConsecutiveMissed")
    void enregistrerPrise_threeMissed_publishesAlerte() {
        // GIVEN
        AdherenceRecord existingRecord = buildRecordWithMissed(2); // 2 déjà manquées
        EnregistrerPriseRequest req    = buildRequest("MANQUE");

        when(adherenceRepository.findByPatientAndTraitement(any(), any()))
                .thenReturn(Optional.of(existingRecord));
        when(adherenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(new AdherenceRecordResponse());

        // WHEN
        service.enregistrerPrise(req);

        // THEN — AlerteConsecutiveMissed doit être publiée
        verify(eventPublisher).publish(any(
                com.pharmaApp.adherence.domain.event.AlerteConsecutiveMissedEvent.class));
    }

    // ── Logique domaine ───────────────────────────────────────

    @Test
    @DisplayName("Domaine — taux 100% si toutes les prises sont confirmées")
    void domain_allConfirmed_taux100() {
        AdherenceRecord record = buildRecordAllConfirmed(10);
        record.recalculateTaux();
        assertThat(record.getTauxGlobal()).isEqualTo(100.0);
        assertThat(record.getConsecutiveMissed()).isEqualTo(0);
    }

    @Test
    @DisplayName("Domaine — taux 50% si moitié confirmées moitié manquées")
    void domain_halfMissed_taux50() {
        AdherenceRecord record = buildRecordHalfMissed(10);
        record.recalculateTaux();
        assertThat(record.getTauxGlobal()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Domaine — isTauxCritique retourne true si taux < 70%")
    void domain_criticalThreshold() {
        AdherenceRecord record = buildRecordHalfMissed(10);
        record.recalculateTaux();
        assertThat(record.isTauxCritique(70.0)).isTrue();
    }

    @Test
    @DisplayName("Domaine — consecutiveMissed = 3 quand 3 dernières manquées")
    void domain_consecutiveMissed() {
        AdherenceRecord record = buildRecordWithMissed(3);
        record.recalculateTaux();
        assertThat(record.getConsecutiveMissed()).isEqualTo(3);
    }

    // ── Builders de test ──────────────────────────────────────

    private EnregistrerPriseRequest buildRequest(String statut) {
        return EnregistrerPriseRequest.builder()
                .priseMedicamentId(1L)
                .traitementId(10L)
                .patientUserId("patient-1")
                .pharmacienUserId("pharmacien-1")
                .medicamentNom("Paracétamol 500mg")
                .dosage("500mg")
                .datePrise(LocalDate.now())
                .heurePrise(LocalTime.of(8, 0))
                .heureConfirmation("CONFIRME".equals(statut) ? LocalTime.of(8, 5) : null)
                .statut(statut)
                .build();
    }

    private AdherenceRecord buildRecordWithMissed(int nbMissed) {
        List<HistoriqueEntry> entries = new ArrayList<>();
        // D'abord quelques confirmées
        for (int i = 0; i < 5; i++) {
            entries.add(HistoriqueEntry.builder()
                    .datePrise(LocalDate.now().minusDays(10 + i))
                    .heurePrise(LocalTime.of(8, 0))
                    .statut(StatutPrise.CONFIRME).build());
        }
        // Puis les manquées récentes (les plus récentes)
        for (int i = 0; i < nbMissed; i++) {
            entries.add(HistoriqueEntry.builder()
                    .datePrise(LocalDate.now().minusDays(i))
                    .heurePrise(LocalTime.of(8, 0))
                    .statut(StatutPrise.MANQUE).build());
        }
        return AdherenceRecord.builder()
                .patientUserId("patient-1").traitementId(10L)
                .pharmacienUserId("pharma-1").entries(entries).build();
    }

    private AdherenceRecord buildRecordAllConfirmed(int nb) {
        List<HistoriqueEntry> entries = new ArrayList<>();
        for (int i = 0; i < nb; i++) {
            entries.add(HistoriqueEntry.builder()
                    .datePrise(LocalDate.now().minusDays(i))
                    .heurePrise(LocalTime.of(8, 0))
                    .statut(StatutPrise.CONFIRME).build());
        }
        return AdherenceRecord.builder()
                .patientUserId("patient-1").traitementId(10L)
                .pharmacienUserId("pharma-1").entries(entries).build();
    }

    private AdherenceRecord buildRecordHalfMissed(int total) {
        List<HistoriqueEntry> entries = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            entries.add(HistoriqueEntry.builder()
                    .datePrise(LocalDate.now().minusDays(i))
                    .heurePrise(LocalTime.of(8, 0))
                    .statut(i % 2 == 0 ? StatutPrise.CONFIRME : StatutPrise.MANQUE)
                    .build());
        }
        return AdherenceRecord.builder()
                .patientUserId("patient-1").traitementId(10L)
                .pharmacienUserId("pharma-1").entries(entries).build();
    }
}
