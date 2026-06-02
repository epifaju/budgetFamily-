package com.invoiceai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceai.config.OcrAiProperties;
import com.invoiceai.domain.enums.CategoryType;
import com.invoiceai.dto.response.ParsedOcrItemResponse;
import com.invoiceai.dto.response.ParsedOcrResponse;
import com.invoiceai.exception.ServiceUnavailableException;
import com.invoiceai.exception.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class OcrAiService {

    private static final Logger log = LoggerFactory.getLogger(OcrAiService.class);
    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private static final String SYSTEM_PROMPT = """
        Tu es un assistant qui structure des tickets de caisse français.
        À partir du texte OCR fourni, extrais les données en JSON strict (sans markdown).
        Schéma attendu :
        {
          "merchant": "string",
          "date": "YYYY-MM-DD",
          "items": [
            {
              "name": "string",
              "quantity": number,
              "unitPrice": number|null,
              "totalPrice": number,
              "category": "ALIMENTAIRE|SANTE|TRANSPORT|VETEMENTS|AUTRES"
            }
          ],
          "subtotal": number,
          "tax": number,
          "total": number,
          "confidenceScore": number
        }
        Règles :
        - Ignore les lignes caissier, adresse, TVA détaillée, mentions légales.
        - Les montants sont des nombres décimaux (point, pas virgule).
        - Si la date est ambiguë, utilise le format ISO le plus probable.
        - category : devine la catégorie de dépense pour chaque article.
        - confidenceScore entre 0 et 1 selon la qualité du texte OCR.
        - Si un champ est introuvable, merchant="Marchand inconnu", items=[], total=0.
        """;

    private final OcrAiProperties properties;
    private final ObjectMapper objectMapper;
    private final ClassificationService classificationService;

    public ParsedOcrResponse parseReceiptText(String rawText) {
        if (!properties.isConfigured()) {
            throw new ServiceUnavailableException("Analyse IA non configurée sur le serveur");
        }

        String trimmed = rawText == null ? "" : rawText.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException("Le texte OCR est vide");
        }
        if (trimmed.length() > properties.getMaxTextLength()) {
            trimmed = trimmed.substring(0, properties.getMaxTextLength());
        }

        try {
            RestClient client = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

            Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "temperature", 0.1,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                    Map.of("role", "user", "content", trimmed)
                )
            );

            String responseBody = client.post()
                .uri("/chat/completions")
                .body(body)
                .retrieve()
                .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new ValidationException("Réponse IA vide");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new ValidationException("Contenu IA introuvable");
            }

            ParsedOcrResponse parsed = objectMapper.readValue(extractJson(content), ParsedOcrResponse.class);
            return normalize(parsed);
        } catch (RestClientException exception) {
            log.warn("OCR AI provider call failed: {}", exception.getMessage());
            throw new ServiceUnavailableException("Service IA temporairement indisponible");
        } catch (ValidationException | ServiceUnavailableException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("OCR AI parsing failed", exception);
            throw new ValidationException("Impossible d'analyser le ticket avec l'IA");
        }
    }

    private ParsedOcrResponse normalize(ParsedOcrResponse parsed) {
        if (parsed.getItems() == null) {
            parsed.setItems(new ArrayList<>());
        }

        List<ParsedOcrItemResponse> normalizedItems = new ArrayList<>();
        for (ParsedOcrItemResponse item : parsed.getItems()) {
            if (item == null || item.getName() == null || item.getName().isBlank()) {
                continue;
            }
            CategoryType category = item.getCategory();
            if (category == null) {
                category = classificationService.classify(item.getName());
            }
            BigDecimal quantity = item.getQuantity() != null && item.getQuantity().compareTo(BigDecimal.ZERO) > 0
                ? item.getQuantity()
                : BigDecimal.ONE;
            BigDecimal totalPrice = nonNegative(item.getTotalPrice());
            normalizedItems.add(ParsedOcrItemResponse.builder()
                .name(item.getName().trim())
                .quantity(quantity)
                .unitPrice(item.getUnitPrice())
                .totalPrice(totalPrice)
                .category(category)
                .build());
        }
        parsed.setItems(normalizedItems);

        if (parsed.getMerchant() == null || parsed.getMerchant().isBlank()) {
            parsed.setMerchant("Marchand inconnu");
        } else {
            parsed.setMerchant(parsed.getMerchant().trim());
        }

        if (parsed.getDate() == null || parsed.getDate().isBlank()) {
            parsed.setDate(java.time.LocalDate.now().toString());
        }

        BigDecimal total = nonNegative(parsed.getTotal());
        BigDecimal subtotal = parsed.getSubtotal() != null ? nonNegative(parsed.getSubtotal()) : total;
        BigDecimal tax = parsed.getTax() != null ? nonNegative(parsed.getTax()) : BigDecimal.ZERO;

        if (total.compareTo(BigDecimal.ZERO) == 0 && !normalizedItems.isEmpty()) {
            total = normalizedItems.stream()
                .map(ParsedOcrItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
            subtotal = total;
        }

        parsed.setTotal(total);
        parsed.setSubtotal(subtotal);
        parsed.setTax(tax);
        parsed.setConfidenceScore(clampConfidence(parsed.getConfidenceScore()));
        parsed.setSource("AI");
        return parsed;
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private double clampConfidence(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.75;
        }
        return Math.max(0.1, Math.min(value, 0.99));
    }

    private String extractJson(String content) {
        Matcher matcher = JSON_BLOCK.matcher(content.trim());
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return content.trim();
    }
}
