package com.invoiceai.service;

import com.invoiceai.domain.User;
import com.invoiceai.dto.request.ChangePasswordRequest;
import com.invoiceai.dto.request.UpdateProfileRequest;
import com.invoiceai.dto.response.UserResponse;
import com.invoiceai.exception.ResourceNotFoundException;
import com.invoiceai.exception.UnauthorizedException;
import com.invoiceai.repository.UserRepository;
import com.invoiceai.security.SecurityUtils;
import com.invoiceai.validation.PasswordPolicy;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        return toResponse(requireCurrentUser());
    }

    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = requireCurrentUser();
        user.setName(request.getName().trim());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = requireCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Mot de passe actuel incorrect");
        }

        PasswordPolicy.validate(request.getNewPassword());
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void deleteCurrentUser() {
        UUID userId = requireCurrentUserId();
        userRepository.deleteById(userId);
    }

    private User requireCurrentUser() {
        UUID userId = requireCurrentUserId();
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    private UUID requireCurrentUserId() {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResourceNotFoundException("Utilisateur introuvable");
        }
        return userId;
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .name(user.getName())
            .createdAt(user.getCreatedAt())
            .build();
    }
}




