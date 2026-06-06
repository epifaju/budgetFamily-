package com.invoiceai.controller;

import com.invoiceai.dto.request.SyncPremiumRequest;
import com.invoiceai.dto.response.SubscriptionStatusResponse;
import com.invoiceai.security.SecurityUtils;
import com.invoiceai.service.SubscriptionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/sync")
    public ResponseEntity<SubscriptionStatusResponse> syncPremiumStatus(
        @RequestBody(required = false) SyncPremiumRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(subscriptionService.syncPremiumStatus(userId, request));
    }
}
