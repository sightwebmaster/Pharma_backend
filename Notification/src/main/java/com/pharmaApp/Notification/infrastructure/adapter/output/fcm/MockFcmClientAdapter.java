
// ─────────────────────────────────────────────────────────────
// FICHIER 5 : MockFcmClientAdapter.java — Mock (dev/test)
// ─────────────────────────────────────────────────────────────
package com.pharmaApp.Notification.infrastructure.adapter.output.fcm;

import com.pharmaApp.Notification.application.dto.FcmResult;
import com.pharmaApp.Notification.application.port.out.FcmClientPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * MockFcmClientAdapter — simule l'envoi FCM en dev/test.
 * Retourne toujours succès avec un messageId fictif.
 * Activé sur les profils "default" et "dev".
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "pharmaApp.fcm.enabled", havingValue = "false")
public class MockFcmClientAdapter implements FcmClientPort {

    @Override
    public FcmResult envoyer(String deviceToken, String titre, String corps) {
        String mockMessageId = "mock-" + UUID.randomUUID();
        log.info("🔔 [MOCK FCM] → token={}... titre='{}' corps='{}' → messageId={}",
                deviceToken.substring(0, Math.min(15, deviceToken.length())),
                titre, corps, mockMessageId);
        return FcmResult.ok(mockMessageId);
    }
}
