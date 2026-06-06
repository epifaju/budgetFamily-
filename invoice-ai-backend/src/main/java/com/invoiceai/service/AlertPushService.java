package com.invoiceai.service;

import com.invoiceai.domain.PushNotificationLog;
import com.invoiceai.domain.PushToken;
import com.invoiceai.domain.User;
import com.invoiceai.domain.enums.AnomalySeverity;
import com.invoiceai.dto.request.RegisterPushTokenRequest;
import com.invoiceai.dto.response.AnomalyResponse;
import com.invoiceai.dto.response.BudgetStatusResponse;
import com.invoiceai.exception.ResourceNotFoundException;
import com.invoiceai.repository.PushNotificationLogRepository;
import com.invoiceai.repository.PushTokenRepository;
import com.invoiceai.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertPushService {

    private static final Logger log = LoggerFactory.getLogger(AlertPushService.class);
    private static final long COOLDOWN_HOURS = 24;

    private final UserRepository userRepository;
    private final PushTokenRepository pushTokenRepository;
    private final PushNotificationLogRepository pushNotificationLogRepository;
    private final BudgetService budgetService;
    private final AnomalyService anomalyService;
    private final SubscriptionService subscriptionService;
    private final FcmPushService fcmPushService;

    @Transactional
    public void registerToken(UUID userId, RegisterPushTokenRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        PushToken existing = pushTokenRepository.findByUserIdAndToken(userId, request.getToken()).orElse(null);
        if (existing != null) {
            existing.setPlatform(request.getPlatform());
            existing.setDeviceId(request.getDeviceId());
            pushTokenRepository.save(existing);
            return;
        }

        pushTokenRepository.save(PushToken.builder()
            .user(user)
            .token(request.getToken())
            .platform(request.getPlatform())
            .deviceId(request.getDeviceId())
            .build());
    }

    @Transactional
    public void evaluateAndNotify(UUID userId) {
        evaluateAndNotify(userId, false);
    }

    @Transactional
    public Map<String, Object> evaluateAndNotify(UUID userId, boolean ignoreCooldown) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !subscriptionService.isPremium(user)) {
            return Map.of(
                "sentCount", 0,
                "premium", false,
                "tokenCount", 0,
                "message", "Premium requis côté serveur (sync Premium dev ou abonnement)"
            );
        }

        List<PushToken> tokens = pushTokenRepository.findByUserId(userId);
        if (tokens.isEmpty() || !fcmPushService.isConfigured()) {
            return Map.of(
                "sentCount", 0,
                "premium", true,
                "tokenCount", tokens.size(),
                "configured", fcmPushService.isConfigured(),
                "message", tokens.isEmpty()
                    ? "Aucun token FCM — Premium dev + notifications ON dans Profil"
                    : "FCM non configuré sur le backend"
            );
        }

        LocalDateTime cooldownSince = LocalDateTime.now().minusHours(COOLDOWN_HOURS);
        int sent = 0;

        for (BudgetStatusResponse status : budgetService.getStatus(userId)) {
            if ("OK".equals(status.getAlertLevel())) {
                continue;
            }
            String alertKey = "budget:" + status.getId() + ":" + status.getAlertLevel() + ":" + status.getPeriodStart();
            if (!ignoreCooldown && pushNotificationLogRepository.existsRecent(userId, alertKey, cooldownSince)) {
                continue;
            }

            String title = "EXCEEDED".equals(status.getAlertLevel())
                ? "Budget " + status.getCategory() + " dépassé"
                : "Budget " + status.getCategory() + " à " + status.getPercentUsed() + " %";
            String body = "EXCEEDED".equals(status.getAlertLevel())
                ? "Plafond dépassé pour la catégorie " + status.getCategory()
                : "Seuil de 80 % atteint pour " + status.getCategory();

            dispatch(user, tokens, alertKey, title, body, Map.of(
                "type", "budget",
                "budgetId", status.getId().toString(),
                "alertLevel", status.getAlertLevel()
            ));
            sent += 1;
        }

        for (AnomalyResponse anomaly : anomalyService.getAnomalies(userId).getAnomalies()) {
            if (anomaly.getSeverity() != AnomalySeverity.HIGH) {
                continue;
            }
            String suffix = anomaly.getInvoiceId() != null
                ? anomaly.getInvoiceId().toString()
                : anomaly.getType().name();
            String alertKey = "anomaly:HIGH:" + suffix;
            if (!ignoreCooldown && pushNotificationLogRepository.existsRecent(userId, alertKey, cooldownSince)) {
                continue;
            }

            dispatch(user, tokens, alertKey, anomaly.getTitle(), anomaly.getMessage(), Map.of(
                "type", "anomaly",
                "anomalyType", anomaly.getType().name(),
                "severity", anomaly.getSeverity().name()
            ));
            sent += 1;
        }

        if (sent > 0) {
            log.info("[alertPush] Sent {} notification groups for user {}", sent, userId);
        }

        return Map.of(
            "sentCount", sent,
            "premium", true,
            "tokenCount", tokens.size(),
            "configured", true,
            "ignoreCooldown", ignoreCooldown,
            "message", sent > 0 ? "Alertes push envoyées" : "Aucune alerte éligible (budget OK ou cooldown 24 h)"
        );
    }

    /** Envoie une notification de test à tous les tokens de l'utilisateur (sans cooldown). */
    @Transactional(readOnly = true)
    public Map<String, Object> sendTestNotification(UUID userId) {
        if (!fcmPushService.isConfigured()) {
            return Map.of(
                "configured", false,
                "tokenCount", 0,
                "sentCount", 0,
                "message", "FCM non configuré (project-id + service-account-path)"
            );
        }

        List<PushToken> tokens = pushTokenRepository.findByUserId(userId);
        if (tokens.isEmpty()) {
            return Map.of(
                "configured", true,
                "tokenCount", 0,
                "sentCount", 0,
                "message", "Aucun token enregistré — ouvrez l'app avec notifications activées"
            );
        }

        int sentCount = 0;
        for (PushToken pushToken : tokens) {
            if (fcmPushService.sendNotification(
                pushToken.getToken(),
                "InvoiceAI — test push",
                "Notification de test depuis le backend (FCM v1)",
                Map.of("type", "test")
            )) {
                sentCount += 1;
            }
        }

        return Map.of(
            "configured", true,
            "tokenCount", tokens.size(),
            "sentCount", sentCount,
            "message", sentCount > 0 ? "Push envoyé" : "Échec envoi — voir logs backend [fcm]"
        );
    }

    private void dispatch(
        User user,
        List<PushToken> tokens,
        String alertKey,
        String title,
        String body,
        Map<String, String> data
    ) {
        Map<String, String> payload = new HashMap<>(data);
        payload.put("alertKey", alertKey);

        for (PushToken pushToken : tokens) {
            fcmPushService.sendNotification(pushToken.getToken(), title, body, payload);
        }

        pushNotificationLogRepository.save(PushNotificationLog.builder()
            .user(user)
            .alertKey(alertKey)
            .build());
    }
}
