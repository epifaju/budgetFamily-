package com.invoiceai.repository;

import com.invoiceai.domain.PushNotificationLog;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushNotificationLogRepository extends JpaRepository<PushNotificationLog, UUID> {

    @Query("""
        SELECT COUNT(log) > 0 FROM PushNotificationLog log
        WHERE log.user.id = :userId
          AND log.alertKey = :alertKey
          AND log.sentAt >= :since
        """)
    boolean existsRecent(
        @Param("userId") UUID userId,
        @Param("alertKey") String alertKey,
        @Param("since") LocalDateTime since
    );
}
