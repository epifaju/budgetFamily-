package com.invoiceai.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.uploads")
public class ReceiptUploadProperties {

    private String directory = "./uploads";
    /** URL publique optionnelle (ex. http://192.168.1.10:8085/uploads pour téléphone WiFi). */
    private String publicBaseUrl = "";

    public Path getUploadRoot() {
        return Paths.get(directory).toAbsolutePath().normalize();
    }
}
