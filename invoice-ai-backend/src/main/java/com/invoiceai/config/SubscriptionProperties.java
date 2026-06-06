package com.invoiceai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.subscription")
public class SubscriptionProperties {

    /** Clé secrète RevenueCat (API v1) — vide = pas de vérification distante. */
    private String revenuecatSecretApiKey = "";

    private String revenuecatEntitlement = "premium";

    /**
     * Profil dev : accepter {@code devPremium} depuis le client mobile.
     * Désactivé par défaut — activer uniquement en local.
     */
    private boolean devTrustClientPremium = false;
}
