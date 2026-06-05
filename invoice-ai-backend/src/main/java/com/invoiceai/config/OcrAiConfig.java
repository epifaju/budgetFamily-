package com.invoiceai.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OcrAiConfig {

    private static final Logger log = LoggerFactory.getLogger(OcrAiConfig.class);

    private final OcrAiProperties properties;

    public OcrAiConfig(OcrAiProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void logConfigurationStatus() {
        if (properties.isConfigured()) {
            log.info("OCR IA activée (modèle={}, baseUrl={})", properties.getModel(), properties.getBaseUrl());
        } else {
            log.warn(
                "OCR IA non configurée (enabled={}, apiKeyPresent={}). "
                    + "Ajoutez ocr.ai dans application-local.yml ou les variables OCR_AI_*.",
                properties.isEnabled(),
                properties.getApiKey() != null && !properties.getApiKey().isBlank()
            );
        }
    }
}
