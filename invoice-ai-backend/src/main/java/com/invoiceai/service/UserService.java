package com.invoiceai.service;

import com.invoiceai.dto.response.UserResponse;
import com.invoiceai.exception.ResourceNotFoundException;
import com.invoiceai.repository.UserRepository;
import com.invoiceai.security.SecurityUtils;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        UUID userId = requireCurrentUserId();
        return userRepository.findById(userId)
            .map(user -> UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .createdAt(user.getCreatedAt())
                .build())
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    @Transactional
    public void deleteCurrentUser() {
        UUID userId = requireCurrentUserId();
        userRepository.deleteById(userId);
    }

    private UUID requireCurrentUserId() {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResourceNotFoundException("Utilisateur introuvable");
        }
        return userId;
    }
}




