package com.invoiceai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PasswordResetEmailService {

    @Value("${app.password-reset.from-email:noreply@invoiceai.local}")
    private String fromEmail;

    public void sendPasswordResetEmail(String toEmail, String rawToken) {
        String message = String.format(
            "Réinitialisation InvoiceAI — utilisez ce code dans l'application (valide 1 h) : %s",
            rawToken
        );

        // MVP dev : le token est journalisé ; brancher SMTP en prod via spring.mail.*
        log.info(
            "PASSWORD_RESET email={} from={} token={} (collez le code dans l'écran « Nouveau mot de passe »)",
            toEmail,
            fromEmail,
            rawToken
        );
        log.debug("PASSWORD_RESET body: {}", message);
    }
}
