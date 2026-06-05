package com.invoiceai.service;

import com.invoiceai.domain.User;
import com.invoiceai.dto.request.LoginRequest;
import com.invoiceai.dto.request.RefreshTokenRequest;
import com.invoiceai.dto.request.RegisterRequest;
import com.invoiceai.dto.response.AuthResponse;
import com.invoiceai.dto.response.UserResponse;
import com.invoiceai.exception.ResourceNotFoundException;
import com.invoiceai.exception.UnauthorizedException;
import com.invoiceai.exception.ValidationException;
import com.invoiceai.repository.UserRepository;
import com.invoiceai.security.JwtTokenProvider;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.invoiceai.validation.PasswordPolicy;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginRateLimiterService loginRateLimiterService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email déjà utilisé");
        }

        PasswordPolicy.validate(request.getPassword());

        User user = User.builder()
            .email(request.getEmail().toLowerCase())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .name(request.getName())
            .privacyConsentAt(LocalDateTime.now())
            .build();

        User savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser, jwtTokenProvider.generateAccessToken(savedUser),
            jwtTokenProvider.generateRefreshToken(savedUser));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase();
        loginRateLimiterService.checkAllowed(email);

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("Email ou mot de passe incorrect"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginRateLimiterService.recordFailure(email);
            throw new UnauthorizedException("Email ou mot de passe incorrect");
        }

        loginRateLimiterService.clearFailures(email);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return buildAuthResponse(user, jwtTokenProvider.generateAccessToken(user),
            jwtTokenProvider.generateRefreshToken(user));
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (!jwtTokenProvider.validateToken(token)) {
            throw new UnauthorizedException("Refresh token invalide");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(token);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        return buildAuthResponse(user, newAccessToken, token);
    }

    public void logout(String token) {
        // Stateless JWT: token blacklist can be implemented later if needed.
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        UserResponse userResponse = UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .name(user.getName())
            .createdAt(user.getCreatedAt())
            .build();

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .user(userResponse)
            .build();
    }
}




