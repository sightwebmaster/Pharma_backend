
// ─────────────────────────────────────────────────────────────
// FICHIER 2 : FcmClientPort.java — Port sortant
// ─────────────────────────────────────────────────────────────
package com.pharmaApp.Notification.application.port.out;

import com.pharmaApp.Notification.application.dto.FcmResult;

/**
 * Port sortant — envoi de notifications FCM.
 * Deux implémentations :
 *   - FcmClientAdapter (Firebase réel — profil prod)
 *   - MockFcmClientAdapter (mock — profil dev/test)
 */
public interface FcmClientPort {
    FcmResult envoyer(String deviceToken, String titre, String corps);
}