package com.invoiceai.service;

import com.invoiceai.config.ReceiptUploadProperties;
import com.invoiceai.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    );

    private final ReceiptUploadProperties properties;

    public String store(UUID userId, MultipartFile file, HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Fichier image vide");
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ValidationException("Format d'image non supporté");
        }

        String extension = resolveExtension(file.getOriginalFilename(), contentType);
        String filename = UUID.randomUUID() + extension;
        Path userDir = properties.getUploadRoot().resolve("invoices").resolve(userId.toString());
        Path destination = userDir.resolve(filename);

        try {
            Files.createDirectories(userDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            log.error("Failed to store receipt image for user {} at {}", userId, destination, exception);
            throw new ValidationException("Impossible d'enregistrer l'image");
        }

        String relativePath = "/uploads/invoices/" + userId + "/" + filename;
        return buildPublicUrl(relativePath, request);
    }

    private String buildPublicUrl(String relativePath, HttpServletRequest request) {
        if (StringUtils.hasText(properties.getPublicBaseUrl())) {
            String base = properties.getPublicBaseUrl().trim();
            if (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            return base + relativePath;
        }
        return ServletUriComponentsBuilder.fromRequestUri(request)
            .replacePath(relativePath)
            .build()
            .toUriString();
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            if (ext.matches("\\.(jpg|jpeg|png|webp)")) {
                return ext;
            }
        }
        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }
        return ".jpg";
    }
}
