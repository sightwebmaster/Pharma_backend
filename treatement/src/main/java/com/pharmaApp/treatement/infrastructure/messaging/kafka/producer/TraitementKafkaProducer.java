package com.pharmaApp.treatement.infrastructure.messaging.kafka.producer;

import com.pharmaApp.treatement.domain.event.PriseConfirmeeEvent;
import com.pharmaApp.treatement.domain.event.PriseManqueeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TraitementKafkaProducer {

    private static final String TOPIC_CONFIRMEE = "treatment.prise-confirmee";
    private static final String TOPIC_MANQUEE   = "treatment.prise-manquee";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Appelé quand le patient confirme avoir pris son médicament
    public void publierPriseConfirmee(PriseConfirmeeEvent event) {
        PriseStatusEventDto dto = PriseStatusEventDto.builder()
                .priseMedicamentId(toLong(event.priseId()))
                .traitementId(toLong(event.traitementId()))
                .patientUserId(event.patientUserId())
                .medicamentNom(event.medicamentNom())
                .datePrise(LocalDate.now())
                .heurePrise(event.heureReelle() != null
                        ? event.heureReelle().toLocalTime() : LocalTime.now())
                .heureConfirmation(event.heureReelle() != null
                        ? event.heureReelle().toLocalTime() : null)
                .statut("CONFIRME")
                .build();

        kafkaTemplate.send(TOPIC_CONFIRMEE, event.patientUserId(), dto);
        log.info("Kafka envoyé → {} | patient={}", TOPIC_CONFIRMEE, event.patientUserId());
    }

    // Appelé quand le scheduler détecte une prise manquée
    public void publierPriseManquee(PriseManqueeEvent event) {
        PriseStatusEventDto dto = PriseStatusEventDto.builder()
                .priseMedicamentId(toLong(event.priseId()))
                .traitementId(toLong(event.traitementId()))
                .patientUserId(event.patientUserId())
                .medicamentNom(event.medicamentNom())
                .datePrise(event.heurePrevue() != null
                        ? event.heurePrevue().toLocalDate() : LocalDate.now())
                .heurePrise(event.heurePrevue() != null
                        ? event.heurePrevue().toLocalTime() : LocalTime.now())
                .statut("MANQUE")
                .build();

        kafkaTemplate.send(TOPIC_MANQUEE, event.patientUserId(), dto);
        log.warn("Kafka envoyé → {} | patient={}", TOPIC_MANQUEE, event.patientUserId());
    }

    private Long toLong(String id) {
        try { return Long.parseLong(id); }
        catch (Exception e) { return 0L; }
    }

    // DTO — doit correspondre exactement au PriseStatusEvent de adherence-service
    @lombok.Builder
    @lombok.Getter
    public static class PriseStatusEventDto {
        private Long      priseMedicamentId;
        private Long      traitementId;
        private String    patientUserId;
        private String    pharmacienUserId;
        private String    medicamentNom;
        private String    dosage;
        private LocalDate datePrise;
        private LocalTime heurePrise;
        private LocalTime heureConfirmation;
        private String    statut;           // "CONFIRME" ou "MANQUE"
        private String    notePatient;
    }
}