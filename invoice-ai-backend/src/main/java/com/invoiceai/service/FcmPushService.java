package com.invoiceai.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.invoiceai.config.FirebaseProperties;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class FcmPushService {

    private static final Logger log = LoggerFactory.getLogger(FcmPushService.class);
    private static final String LEGACY_FCM_URL = "https://fcm.googleapis.com/fcm/send";
    private static final String MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    private final FirebaseProperties firebaseProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isConfigured() {
        return isV1Configured() || isLegacyConfigured();
    }

    public boolean sendNotification(String token, String title, String body, Map<String, String> data) {
        if (!isConfigured()) {
            log.warn("[fcm] Not configured — skip push");
            return false;
        }

        try {
            if (isV1Configured()) {
                sendV1(token, title, body, data);
            } else {
                sendLegacy(token, title, body, data);
            }
            log.info("[fcm] Push sent to token {}…", token.substring(0, Math.min(12, token.length())));
            return true;
        } catch (IOException error) {
            log.warn("[fcm] Auth failed: {}", error.getMessage());
        } catch (RestClientException error) {
            log.warn("[fcm] Push failed: {}", error.getMessage());
        }
        return false;
    }

    private boolean isV1Configured() {
        return hasText(firebaseProperties.getProjectId())
            && hasText(firebaseProperties.getServiceAccountPath());
    }

    private boolean isLegacyConfigured() {
        return hasText(firebaseProperties.getServerKey());
    }

    private void sendV1(String token, String title, String body, Map<String, String> data) throws IOException {
        String projectId = firebaseProperties.getProjectId().trim();
        String url = "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send";

        Map<String, Object> message = new HashMap<>();
        message.put("token", token);
        message.put("notification", Map.of("title", title, "body", body));
        if (data != null && !data.isEmpty()) {
            message.put("data", data);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAccessToken());

        restTemplate.postForEntity(url, new HttpEntity<>(Map.of("message", message), headers), String.class);
    }

    private void sendLegacy(String token, String title, String body, Map<String, String> data) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "key=" + firebaseProperties.getServerKey().trim());

        Map<String, Object> payload = new HashMap<>();
        payload.put("to", token);
        payload.put("notification", Map.of("title", title, "body", body));
        if (data != null && !data.isEmpty()) {
            payload.put("data", data);
        }

        restTemplate.postForEntity(LEGACY_FCM_URL, new HttpEntity<>(payload, headers), String.class);
    }

    private String getAccessToken() throws IOException {
        String path = firebaseProperties.getServiceAccountPath().trim();
        try (InputStream stream = new FileInputStream(path)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream)
                .createScoped(MESSAGING_SCOPE);
            credentials.refreshIfExpired();
            if (credentials.getAccessToken() == null) {
                throw new IOException("Impossible d'obtenir un token OAuth FCM.");
            }
            return credentials.getAccessToken().getTokenValue();
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
