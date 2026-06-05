package com.invoiceai.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class OcrAiPropertiesBindingTest {

    @Autowired
    private OcrAiProperties properties;

    @Test
    void loadsOcrAiSettingsFromLocalProfile() {
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getApiKey()).isNotBlank();
        assertThat(properties.isConfigured()).isTrue();
    }
}
