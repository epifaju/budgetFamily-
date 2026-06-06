package com.invoiceai.service;

import com.invoiceai.config.SubscriptionProperties;
import com.invoiceai.domain.User;
import com.invoiceai.dto.request.SyncPremiumRequest;
import com.invoiceai.dto.response.SubscriptionStatusResponse;
import com.invoiceai.exception.ResourceNotFoundException;
import com.invoiceai.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final UserRepository userRepository;
    private final SubscriptionProperties subscriptionProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isPremium(User user) {
        return user != null && user.isPremium();
    }

    @Transactional
    public SubscriptionStatusResponse syncPremiumStatus(UUID userId, SyncPremiumRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        String source = "database";
        boolean premium = user.isPremium();

        if (hasRevenueCatSecret()) {
            premium = fetchPremiumFromRevenueCat(userId);
            source = "revenuecat";
        } else if (subscriptionProperties.isDevTrustClientPremium()
            && request != null
            && Boolean.TRUE.equals(request.getDevPremium())) {
            premium = true;
            source = "dev_client";
        }

        user.setPremium(premium);
        user.setPremiumSyncedAt(LocalDateTime.now());
        userRepository.save(user);

        return SubscriptionStatusResponse.builder()
            .premium(premium)
            .source(source)
            .build();
    }

    private boolean hasRevenueCatSecret() {
        String key = subscriptionProperties.getRevenuecatSecretApiKey();
        return key != null && !key.isBlank();
    }

    @SuppressWarnings("unchecked")
    private boolean fetchPremiumFromRevenueCat(UUID userId) {
        String apiKey = subscriptionProperties.getRevenuecatSecretApiKey().trim();
        String entitlement = subscriptionProperties.getRevenuecatEntitlement();
        String url = "https://api.revenuecat.com/v1/subscribers/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) {
                return false;
            }

            Object subscriberObj = body.get("subscriber");
            if (!(subscriberObj instanceof Map<?, ?> subscriber)) {
                return false;
            }

            Object entitlementsObj = subscriber.get("entitlements");
            if (!(entitlementsObj instanceof Map<?, ?> entitlements)) {
                return false;
            }

            Object premiumObj = entitlements.get(entitlement);
            if (!(premiumObj instanceof Map<?, ?> premiumEntitlement)) {
                return false;
            }

            Object expiresDate = premiumEntitlement.get("expires_date");
            if (expiresDate == null) {
                return true;
            }

            return !expiresDate.toString().startsWith("1970");
        } catch (RestClientException error) {
            log.warn("[subscription] RevenueCat lookup failed for user {}: {}", userId, error.getMessage());
            return false;
        }
    }
}
