package com.pharmaApp.treatement.infrastructure.messaging.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmaApp.treatement.domain.event.PriseConfirmeeEvent;
import com.pharmaApp.treatement.domain.event.PriseManqueeEvent;
import com.pharmaApp.treatement.domain.event.TraitementCreeEvent;
import com.pharmaApp.treatement.domain.event.TraitementModifieEvent;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.entity.OutboxEventEntity;
import com.pharmaApp.treatement.infrastructure.adapter.out.persistence.repository.OutboxEventJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TraitementKafkaProducer {

    public static final String TOPIC_PRISE_CONFIRMEE = "prise.confirmee";
    public static final String TOPIC_PRISE_MANQUEE = "prise.manquee";
    public static final String TOPIC_TRAITEMENT_CREE = "traitement.cree";
    public static final String TOPIC_TRAITEMENT_MODIFIE = "traitement.modifie";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxEventJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveToOutbox(PriseConfirmeeEvent event) {
        outboxRepository.save(OutboxEventEntity.of(
                event.traitementId(),
                "Traitement",
                "PriseConfirmee",
                toJson(event)
        ));
        log.debug("Outbox <- PriseConfirmee priseId={}", event.priseId());
    }

    @Transactional
    public void saveToOutbox(PriseManqueeEvent event) {
        outboxRepository.save(OutboxEventEntity.of(
                event.traitementId(),
                "Traitement",
                "PriseManquee",
                toJson(event)
        ));
        log.debug("Outbox <- PriseManquee priseId={}", event.priseId());
    }

    @Transactional
    public void saveToOutbox(TraitementCreeEvent event) {
        outboxRepository.save(OutboxEventEntity.of(
                event.traitementId(),
                "Traitement",
                "TraitementCree",
                toJson(event)
        ));
        log.debug("Outbox <- TraitementCree traitementId={}", event.traitementId());
    }

    @Transactional
    public void saveToOutbox(TraitementModifieEvent event) {
        outboxRepository.save(OutboxEventEntity.of(
                event.traitementId(),
                "Traitement",
                "TraitementModifie",
                toJson(event)
        ));
        log.debug("Outbox <- TraitementModifie traitementId={}", event.traitementId());
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventEntity> pending =
                outboxRepository.findTop50ByStatusOrderByCreatedAtAsc(
                        OutboxEventEntity.OutboxStatus.PENDING
                );

        if (pending.isEmpty()) {
            return;
        }

        log.debug("Outbox -> {} events a publier", pending.size());

        for (OutboxEventEntity event : pending) {
            String topic = resolveTopic(event.getEventType());
            try {
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);
                event.setStatus(OutboxEventEntity.OutboxStatus.PROCESSED);
                event.setProcessedAt(LocalDateTime.now());
                log.debug("Event {} publie sur {}", event.getId(), topic);
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 3) {
                    event.setStatus(OutboxEventEntity.OutboxStatus.FAILED);
                    event.setErrorMessage(e.getMessage());
                }
                log.error("Publication echouee eventId={} retry={} : {}",
                        event.getId(), event.getRetryCount(), e.getMessage());
            }
            outboxRepository.save(event);
        }
    }

    private String resolveTopic(String eventType) {
        return switch (eventType) {
            case "PriseConfirmee" -> TOPIC_PRISE_CONFIRMEE;
            case "PriseManquee" -> TOPIC_PRISE_MANQUEE;
            case "TraitementCree" -> TOPIC_TRAITEMENT_CREE;
            case "TraitementModifie" -> TOPIC_TRAITEMENT_MODIFIE;
            default -> throw new IllegalArgumentException("Event type inconnu : " + eventType);
        };
    }

    private String toJson(Object event) {
        try {
            return objectMapper.copy()
                    .findAndRegisterModules()
                    .writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Serialisation JSON echouee", e);
        }
    }
}
