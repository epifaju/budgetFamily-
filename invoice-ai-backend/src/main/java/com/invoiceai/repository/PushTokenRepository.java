package com.invoiceai.repository;

import com.invoiceai.domain.PushToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushTokenRepository extends JpaRepository<PushToken, UUID> {

    List<PushToken> findByUserId(UUID userId);

    Optional<PushToken> findByUserIdAndToken(UUID userId, String token);
}
