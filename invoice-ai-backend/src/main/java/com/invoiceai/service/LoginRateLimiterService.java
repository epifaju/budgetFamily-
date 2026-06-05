package com.invoiceai.service;

import com.invoiceai.exception.ValidationException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class LoginRateLimiterService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> attemptsByEmail = new ConcurrentHashMap<>();

    public void checkAllowed(String email) {
        String key = normalize(email);
        Deque<Instant> attempts = attemptsByEmail.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            pruneOldAttempts(attempts);
            if (attempts.size() >= MAX_ATTEMPTS) {
                throw new ValidationException(
                    "Trop de tentatives de connexion. Réessayez dans 15 minutes."
                );
            }
        }
    }

    public void recordFailure(String email) {
        String key = normalize(email);
        Deque<Instant> attempts = attemptsByEmail.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            pruneOldAttempts(attempts);
            attempts.addLast(Instant.now());
        }
    }

    public void clearFailures(String email) {
        attemptsByEmail.remove(normalize(email));
    }

    private void pruneOldAttempts(Deque<Instant> attempts) {
        Instant threshold = Instant.now().minus(WINDOW);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(threshold)) {
            attempts.removeFirst();
        }
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
