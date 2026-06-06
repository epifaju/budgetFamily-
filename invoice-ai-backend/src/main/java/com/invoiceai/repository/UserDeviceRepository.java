package com.invoiceai.repository;

import com.invoiceai.domain.UserDevice;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {

    Optional<UserDevice> findByUserIdAndDeviceId(UUID userId, String deviceId);

    Optional<UserDevice> findByUserIdAndDeviceIdAndActiveTrue(UUID userId, String deviceId);

    long countByUserIdAndActiveTrue(UUID userId);

    List<UserDevice> findByUserIdAndActiveTrueOrderByLastSeenAtDesc(UUID userId);
}
