package com.invoiceai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.firebase")
public class FirebaseProperties {

    /** ID projet Firebase (ex. budget-family-47eec) — requis pour FCM HTTP v1. */
    private String projectId = "";

    /**
     * Chemin vers le JSON compte de service Firebase (Paramètres → Comptes de service).
     * Recommandé : remplace la clé serveur legacy.
     */
    private String serviceAccountPath = "";

    /** Clé serveur FCM legacy (dépréciée). Utilisée seulement si serviceAccountPath est vide. */
    private String serverKey = "";
}
