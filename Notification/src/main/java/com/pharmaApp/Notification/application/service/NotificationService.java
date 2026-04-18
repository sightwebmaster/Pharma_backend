package com.pharmaApp.Notification.application.service;

import com.pharmaApp.Notification.application.dto.FcmResult;
import com.pharmaApp.Notification.application.dto.ProcheResponse;
import com.pharmaApp.Notification.application.port.out.FcmClientPort;
import com.pharmaApp.Notification.infrastructure.adapter.output.client.UserServiceClient;
import com.pharmaApp.Notification.infrastructure.adapter.output.persistence.entity.NotificationEnvoyeeEntity;
import com.pharmaApp.Notification.infrastructure.adapter.output.persistence.entity.RappelEntity;
import com.pharmaApp.Notification.infrastructure.adapter.output.persistence.entity.TokenFcmEntity;
import com.pharmaApp.Notification.infrastructure.adapter.output.persistence.repository.NotificationEnvoyeeJpaRepository;
import com.pharmaApp.Notification.infrastructure.adapter.output.persistence.repository.RappelJpaRepository;
import com.pharmaApp.Notification.infrastructure.adapter.output.persistence.repository.TokenFcmJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final RappelJpaRepository rappelRepository;
    private final TokenFcmJpaRepository tokenFcmRepository;
    private final NotificationEnvoyeeJpaRepository historiqueRepository;
    private final FcmClientPort fcmClient;
    private final UserServiceClient userServiceClient;

    public void planifierRappels(
            String traitementId,
            String patientUserId,
            List<Map<String, Object>> prises) {

        if (prises == null || prises.isEmpty()) {
            log.warn("planifierRappels - aucune prise pour traitementId={}", traitementId);
            return;
        }

        long existants = rappelRepository.countByTraitementId(traitementId);
        if (existants > 0) {
            log.info("Rappels deja planifies pour traitementId={} ({}) - ignore",
                    traitementId, existants);
            return;
        }

        int count = 0;
        for (Map<String, Object> prise : prises) {
            try {
                String medicamentNom = (String) prise.get("medicamentNom");
                String heureEnvoiStr = (String) prise.get("heureEnvoi");

                RappelEntity rappel = new RappelEntity();
                rappel.setId(UUID.randomUUID().toString());
                rappel.setTraitementId(traitementId);
                rappel.setPatientUserId(patientUserId);
                rappel.setMedicamentNom(medicamentNom != null ? medicamentNom : "Medicament");
                LocalDateTime reference = parseDateTime(heureEnvoiStr);
                rappel.setHeureEnvoi(reference);
                rappel.setHeureReference(reference);
                rappel.setStatut("PLANIFIE");
                rappel.setNbTentatives(0);

                rappelRepository.save(rappel);
                count++;
            } catch (Exception e) {
                log.error("Erreur creation rappel : {}", e.getMessage());
            }
        }

        log.info("planifierRappels - {} rappel(s) crees pour traitementId={}", count, traitementId);
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void envoyerRappelsDus() {
        LocalDateTime maintenant = LocalDateTime.now();
        List<RappelEntity> dus = rappelRepository
                .findByStatutAndHeureEnvoiLessThanEqual("PLANIFIE", maintenant);

        if (dus.isEmpty()) {
            return;
        }

        log.info("Scheduler - {} rappel(s) dus a envoyer", dus.size());

        for (RappelEntity rappel : dus) {
            try {
                envoyerRappelFcm(rappel);
            } catch (Exception e) {
                log.error("Erreur envoi rappel id={} : {}", rappel.getId(), e.getMessage());
            }
        }
    }

    private void envoyerRappelFcm(RappelEntity rappel) {
        LocalDateTime maintenant = LocalDateTime.now();
        if (maintenant.isAfter(rappel.getHeureReference().plusMinutes(30))) {
            rappel.setStatut("EXPIRE");
            rappelRepository.save(rappel);
            log.info("Rappel expire sans confirmation id={} patient={}",
                    rappel.getId(), rappel.getPatientUserId());
            return;
        }

        Optional<TokenFcmEntity> tokenOpt =
                tokenFcmRepository.findFirstByUserIdAndActifTrueOrderByUpdatedAtDesc(
                        rappel.getPatientUserId());

        if (tokenOpt.isEmpty()) {
            log.warn("Token FCM introuvable pour patient={} - rappel id={} marque ECHEC",
                    rappel.getPatientUserId(), rappel.getId());
            rappel.setStatut("ECHEC");
            rappelRepository.save(rappel);
            return;
        }

        String titre = "Rappel medicament";
        String corps = "Il est l'heure de prendre votre " + rappel.getMedicamentNom();

        FcmResult result = fcmClient.envoyer(tokenOpt.get().getDeviceToken(), titre, corps);

        if (result.success()) {
            persisterHistorique(rappel.getPatientUserId(), "RAPPEL_PRISE",
                    titre, corps, "ENVOYE", result.messageId());
            rappel.setNbTentatives(rappel.getNbTentatives() + 1);
            LocalDateTime prochaineTentative = maintenant.plusMinutes(1);
            if (prochaineTentative.isAfter(rappel.getHeureReference().plusMinutes(30))) {
                rappel.setStatut("TERMINE");
            } else {
                rappel.setHeureEnvoi(prochaineTentative);
                rappel.setStatut("PLANIFIE");
            }
        } else {
            rappel.setNbTentatives(rappel.getNbTentatives() + 1);
            if (rappel.getNbTentatives() >= 3) {
                rappel.setStatut("ECHEC");
                log.error("Rappel id={} - 3 tentatives echouees", rappel.getId());
            } else {
                rappel.setHeureEnvoi(maintenant.plusMinutes(1));
            }
        }

        rappelRepository.save(rappel);
    }

    public void annulerRappelPrise(
            String patientUserId,
            String medicamentNom,
            LocalDateTime heurePrevue) {
        LocalDateTime windowStart = heurePrevue.minusMinutes(1);
        LocalDateTime windowEnd = heurePrevue.plusMinutes(30);
        int updated = rappelRepository.annulerPriseActive(
                patientUserId,
                medicamentNom,
                windowStart,
                windowEnd);
        log.info("annulerRappelPrise - patient={} medicament={} heure={} updated={}",
                patientUserId, medicamentNom, heurePrevue, updated);
    }

    public void alerterProcheManquee(
            String patientUserId,
            String medicamentNom,
            String priseId,
            LocalDateTime heurePrevue) {

        log.warn("alerterProcheManquee - patient={} medicament={} priseId={}",
                patientUserId, medicamentNom, priseId);

        annulerRappelPrise(patientUserId, medicamentNom, heurePrevue);

        List<ProcheResponse> followers = userServiceClient.getFollowers(patientUserId);
        if (followers.isEmpty()) {
            log.info("Aucun proche suiveur a alerter pour patient={}", patientUserId);
            return;
        }

        String titre = "Alerte prise manquee";
        String corps = "Une prise de " + medicamentNom
                + " n'a pas ete confirmee depuis plus de 30 minutes.";

        for (ProcheResponse follower : followers) {
            String followerUserId = follower.getPatientUserId();
            if (followerUserId == null || followerUserId.isBlank()) {
                continue;
            }

            Optional<TokenFcmEntity> tokenOpt =
                    tokenFcmRepository.findFirstByUserIdAndActifTrueOrderByUpdatedAtDesc(
                            followerUserId);

            if (tokenOpt.isEmpty()) {
                log.warn("Token FCM introuvable pour le proche suiveur={} du patient={}",
                        followerUserId, patientUserId);
                persisterHistorique(followerUserId, "ALERTE_PROCHE",
                        titre, corps, "ECHEC", null);
                continue;
            }

            FcmResult result = fcmClient.envoyer(tokenOpt.get().getDeviceToken(), titre, corps);
            if (result.success()) {
                persisterHistorique(followerUserId, "ALERTE_PROCHE",
                        titre, corps, "ENVOYE", result.messageId());
                log.info("Alerte proche envoyee a follower={} pour patient={}",
                        followerUserId, patientUserId);
            } else {
                persisterHistorique(followerUserId, "ALERTE_PROCHE",
                        titre, corps, "ECHEC", null);
                log.error("Echec envoi alerte proche a follower={} pour patient={}",
                        followerUserId, patientUserId);
            }
        }
    }

    public void enregistrerToken(String userId, String deviceToken, String plateforme) {
        if (deviceToken == null || deviceToken.isBlank()) {
            log.warn("Token FCM vide ignore pour userId={}", userId);
            return;
        }

        List<TokenFcmEntity> activeDeviceAssignments =
                tokenFcmRepository.findAllByDeviceTokenAndActifTrue(deviceToken);
        for (TokenFcmEntity assignment : activeDeviceAssignments) {
            if (!userId.equals(assignment.getUserId())) {
                assignment.setActif(false);
                assignment.setUpdatedAt(LocalDateTime.now());
                tokenFcmRepository.save(assignment);
                log.info("Token FCM transfere deviceToken={} ancienUserId={} nouveauUserId={}",
                        abbreviateToken(deviceToken), assignment.getUserId(), userId);
            }
        }

        Optional<TokenFcmEntity> existing =
                tokenFcmRepository.findByUserIdAndDeviceToken(userId, deviceToken);

        TokenFcmEntity token;
        if (existing.isPresent()) {
            token = existing.get();
            token.setActif(true);
            token.setUpdatedAt(LocalDateTime.now());
        } else {
            List<TokenFcmEntity> activeTokens =
                    tokenFcmRepository.findAllByUserIdAndActifTrue(userId);
            for (TokenFcmEntity old : activeTokens) {
                old.setActif(false);
                tokenFcmRepository.save(old);
            }

            token = new TokenFcmEntity();
            token.setId(UUID.randomUUID().toString());
            token.setUserId(userId);
            token.setDeviceToken(deviceToken);
            token.setPlateforme(plateforme != null ? plateforme : "ANDROID");
            token.setActif(true);
        }

        tokenFcmRepository.save(token);
        log.info("Token FCM enregistre pour userId={}", userId);
    }

    @Transactional(readOnly = true)
    public List<NotificationEnvoyeeEntity> getHistorique(String userId) {
        return historiqueRepository.findByUserIdOrderByEnvoyeADesc(
                userId, PageRequest.of(0, 50));
    }

    private void persisterHistorique(
            String userId,
            String type,
            String titre,
            String corps,
            String statut,
            String fcmMessageId) {
        NotificationEnvoyeeEntity notif = new NotificationEnvoyeeEntity();
        notif.setId(UUID.randomUUID().toString());
        notif.setUserId(userId);
        notif.setType(type);
        notif.setTitre(titre);
        notif.setCorps(corps);
        notif.setStatut(statut);
        notif.setFcmMessageId(fcmMessageId);
        notif.setEnvoyeA(LocalDateTime.now());
        historiqueRepository.save(notif);
    }

    private LocalDateTime parseDateTime(String str) {
        if (str == null) {
            return LocalDateTime.now().plusHours(1);
        }
        try {
            return LocalDateTime.parse(str.length() > 19 ? str.substring(0, 19) : str);
        } catch (Exception e) {
            return LocalDateTime.now().plusHours(1);
        }
    }

    private String abbreviateToken(String deviceToken) {
        if (deviceToken == null || deviceToken.length() <= 16) {
            return deviceToken;
        }
        return deviceToken.substring(0, 16) + "...";
    }
}
