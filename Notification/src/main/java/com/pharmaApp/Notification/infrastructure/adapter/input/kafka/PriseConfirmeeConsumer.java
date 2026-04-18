// ─────────────────────────────────────────────────────────────
// FICHIER 2 : PriseConfirmeeConsumer.java
// ─────────────────────────────────────────────────────────────
package com.pharmaApp.Notification.infrastructure.adapter.input.kafka;

import com.pharmaApp.Notification.application.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Consomme prise.confirmee → annule le rappel correspondant.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriseConfirmeeConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "prise.confirmee",
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPriseConfirmee(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Kafka ← [{}] offset={}", topic, offset);

        try {
            String patientUserId = (String) payload.get("patientUserId");
            String medicamentNom = (String) payload.get("medicamentNom");
            LocalDateTime heurePrevue = parseDateTime(payload.get("heurePrevue"));
            notificationService.annulerRappelPrise(
                    patientUserId,
                    medicamentNom,
                    heurePrevue);
        } catch (Exception e) {
            log.error("Erreur PriseConfirmeeConsumer : {}", e.getMessage(), e);
        }
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return LocalDateTime.now();
        }
        String str = value.toString();
        if (str.length() > 19) {
            str = str.substring(0, 19);
        }
        return LocalDateTime.parse(str);
    }
}
