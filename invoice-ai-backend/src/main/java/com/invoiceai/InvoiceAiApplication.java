package com.invoiceai;

import com.invoiceai.config.OcrAiProperties;
import com.invoiceai.config.ReceiptUploadProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ OcrAiProperties.class, ReceiptUploadProperties.class })
public class InvoiceAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvoiceAiApplication.class, args);
    }
}




