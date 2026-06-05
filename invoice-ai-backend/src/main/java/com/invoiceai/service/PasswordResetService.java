package com.invoiceai.service;

import com.invoiceai.domain.PasswordResetToken;
import com.invoiceai.domain.User;
import com.invoiceai.exception.ValidationException;
import com.invoiceai.repository.PasswordResetTokenRepository;
import com.invoiceai.repository.UserRepository;
import com.invoiceai.validation.PasswordPolicy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetEmailService emailService;

    @Transactional
    public void requestReset(String email) {
        String normalized = email.trim().toLowerCase();
        userRepository.findByEmail(normalized).ifPresent(user -> {
            String rawToken = UUID.randomUUID().toString().replace("-", "");
            PasswordResetToken entity = PasswordResetToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
            tokenRepository.save(entity);
            emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordPolicy.validate(newPassword);

        PasswordResetToken token = tokenRepository
            .findByTokenHashAndUsedAtIsNull(hashToken(rawToken.trim()))
            .orElseThrow(() -> new ValidationException("Code de réinitialisation invalide ou expiré"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Code de réinitialisation expiré");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
