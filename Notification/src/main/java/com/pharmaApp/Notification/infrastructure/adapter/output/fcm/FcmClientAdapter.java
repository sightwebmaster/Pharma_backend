
// ─────────────────────────────────────────────────────────────
// FICHIER 4 : FcmClientAdapter.java — Firebase réel (prod)
// ─────────────────────────────────────────────────────────────
package com.pharmaApp.Notification.infrastructure.adapter.output.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.pharmaApp.Notification.application.dto.FcmResult;
import com.pharmaApp.Notification.application.port.out.FcmClientPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * FcmClientAdapter — implémentation Firebase Admin SDK.
 * Activé uniquement sur le profil "prod".
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "pharmaApp.fcm.enabled", havingValue = "true", matchIfMissing = true)
public class FcmClientAdapter implements FcmClientPort {

    @Override
    public FcmResult envoyer(String deviceToken, String titre, String corps) {
        try {
            Message message = Message.builder()
                    .setToken(deviceToken)
                    .setNotification(Notification.builder()
                            .setTitle(titre)
                            .setBody(corps)
                            .build())
                    .putData("titre", titre)
                    .putData("corps", corps)
                    .build();

            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("FCM envoyé — messageId={} token={}...",
                    messageId, deviceToken.substring(0, Math.min(20, deviceToken.length())));

            return FcmResult.ok(messageId);

        } catch (FirebaseMessagingException e) {
            log.error("FCM échec — token={}... erreur={}",
                    deviceToken.substring(0, Math.min(20, deviceToken.length())),
                    e.getMessagingErrorCode());
            return FcmResult.error(e.getMessagingErrorCode() != null
                    ? e.getMessagingErrorCode().name() : "UNKNOWN");
        }
    }
}
 
