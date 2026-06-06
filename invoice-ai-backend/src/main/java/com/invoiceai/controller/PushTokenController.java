package com.invoiceai.controller;

import com.invoiceai.config.SubscriptionProperties;
import com.invoiceai.dto.request.RegisterPushTokenRequest;
import com.invoiceai.security.SecurityUtils;
import com.invoiceai.service.AlertPushService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
public class PushTokenController {

    private final AlertPushService alertPushService;
    private final SubscriptionProperties subscriptionProperties;

    @PostMapping("/tokens")
    public ResponseEntity<Void> registerToken(@Valid @RequestBody RegisterPushTokenRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        alertPushService.registerToken(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTestNotification() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(alertPushService.sendTestNotification(userId));
    }

    @PostMapping("/evaluate")
    public ResponseEntity<Map<String, Object>> evaluateAlerts(
        @RequestParam(defaultValue = "false") boolean ignoreCooldown
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        boolean allowIgnoreCooldown = ignoreCooldown && subscriptionProperties.isDevTrustClientPremium();
        return ResponseEntity.ok(alertPushService.evaluateAndNotify(userId, allowIgnoreCooldown));
    }
}
