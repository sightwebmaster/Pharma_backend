// ─────────────────────────────────────────────────────────────
// FICHIER 1 : FcmResult.java — DTO résultat envoi FCM
// ─────────────────────────────────────────────────────────────
package com.pharmaApp.Notification.application.dto;

/**
 * Résultat d'un envoi FCM.
 */
public record FcmResult(
        boolean success,
        String  messageId,   // null si échec
        String  errorCode    // null si succès
) {
    public static FcmResult ok(String messageId) {
        return new FcmResult(true, messageId, null);
    }

    public static FcmResult error(String errorCode) {
        return new FcmResult(false, null, errorCode);
    }
}