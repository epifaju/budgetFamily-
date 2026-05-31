package com.invoiceai.security;

import com.invoiceai.config.JwtConfig;
import com.invoiceai.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;
    private SecretKey signingKey;

    public JwtTokenProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @PostConstruct
    void init() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getAccessTokenExpiration());
        return Jwts.builder()
            .subject(user.getId().toString())
            .issuedAt(now)
            .expiration(expiry)
            .claim("email", user.getEmail())
            .claim("type", "access")
            .signWith(signingKey)
            .compact();
    }

    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getRefreshTokenExpiration());
        return Jwts.builder()
            .subject(user.getId().toString())
            .issuedAt(now)
            .expiration(expiry)
            .claim("type", "refresh")
            .signWith(signingKey)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public UUID getUserIdFromToken(String token) {
        Jws<Claims> claims = parseClaims(token);
        return UUID.fromString(claims.getPayload().getSubject());
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token);
    }
}



