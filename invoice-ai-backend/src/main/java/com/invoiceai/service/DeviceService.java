package com.invoiceai.service;

import com.invoiceai.domain.User;
import com.invoiceai.domain.UserDevice;
import com.invoiceai.dto.request.DeviceInfoRequest;
import com.invoiceai.dto.response.DeviceStatusResponse;
import com.invoiceai.exception.ForbiddenException;
import com.invoiceai.exception.ResourceNotFoundException;
import com.invoiceai.repository.UserDeviceRepository;
import com.invoiceai.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceService {

    public static final String HEADER_DEVICE_ID = "X-Device-Id";
    public static final String CODE_DEVICE_REQUIRED = "DEVICE_REQUIRED";
    public static final String CODE_DEVICE_NOT_REGISTERED = "DEVICE_NOT_REGISTERED";
    public static final String CODE_DEVICE_LIMIT = "DEVICE_LIMIT";

    private static final int FREE_TIER_DEVICE_LIMIT = 1;

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final SubscriptionService subscriptionService;

    @Transactional
    public DeviceStatusResponse registerDevice(UUID userId, DeviceInfoRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        String deviceId = normalizeDeviceId(request.getDeviceId());
        LocalDateTime now = LocalDateTime.now();

        UserDevice existing = userDeviceRepository.findByUserIdAndDeviceId(userId, deviceId).orElse(null);
        if (existing != null) {
            existing.setActive(true);
            existing.setLastSeenAt(now);
            if (request.getDeviceName() != null) {
                existing.setDeviceName(request.getDeviceName());
            }
            if (request.getPlatform() != null) {
                existing.setPlatform(request.getPlatform());
            }
            userDeviceRepository.save(existing);
            return buildStatus(user, deviceId, existing);
        }

        long activeCount = userDeviceRepository.countByUserIdAndActiveTrue(userId);
        if (!subscriptionService.isPremium(user) && activeCount >= FREE_TIER_DEVICE_LIMIT) {
            throw new ForbiddenException(
                CODE_DEVICE_LIMIT,
                "Version gratuite limitée à un appareil. Passez à Premium pour synchroniser sur plusieurs appareils."
            );
        }

        UserDevice created = UserDevice.builder()
            .user(user)
            .deviceId(deviceId)
            .deviceName(request.getDeviceName())
            .platform(request.getPlatform())
            .lastSeenAt(now)
            .active(true)
            .build();
        userDeviceRepository.save(created);

        return buildStatus(user, deviceId, created);
    }

    @Transactional(readOnly = true)
    public DeviceStatusResponse getDeviceStatus(UUID userId, String deviceIdHeader) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        if (deviceIdHeader == null || deviceIdHeader.isBlank()) {
            return DeviceStatusResponse.builder()
                .deviceId(null)
                .registered(false)
                .canSync(false)
                .premium(subscriptionService.isPremium(user))
                .activeDeviceCount((int) userDeviceRepository.countByUserIdAndActiveTrue(userId))
                .build();
        }

        String deviceId = normalizeDeviceId(deviceIdHeader);
        UserDevice device = userDeviceRepository.findByUserIdAndDeviceIdAndActiveTrue(userId, deviceId).orElse(null);
        return buildStatus(user, deviceId, device);
    }

    @Transactional
    public void assertSyncAllowed(UUID userId, String deviceIdHeader) {
        if (deviceIdHeader == null || deviceIdHeader.isBlank()) {
            throw new ForbiddenException(
                CODE_DEVICE_REQUIRED,
                "Identifiant appareil requis. Mettez à jour l'application."
            );
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        String deviceId = normalizeDeviceId(deviceIdHeader);
        UserDevice device = userDeviceRepository.findByUserIdAndDeviceIdAndActiveTrue(userId, deviceId)
            .orElseThrow(() -> new ForbiddenException(
                CODE_DEVICE_NOT_REGISTERED,
                "Cet appareil n'est pas enregistré pour la synchronisation. Reconnectez-vous."
            ));

        device.setLastSeenAt(LocalDateTime.now());
        userDeviceRepository.save(device);

        long activeCount = userDeviceRepository.countByUserIdAndActiveTrue(userId);
        if (!subscriptionService.isPremium(user) && activeCount > FREE_TIER_DEVICE_LIMIT) {
            throw new ForbiddenException(
                CODE_DEVICE_LIMIT,
                "Version gratuite limitée à un appareil. Passez à Premium pour synchroniser sur plusieurs appareils."
            );
        }
    }

    private DeviceStatusResponse buildStatus(User user, String deviceId, UserDevice device) {
        int activeCount = (int) userDeviceRepository.countByUserIdAndActiveTrue(user.getId());
        boolean registered = device != null && device.isActive();
        boolean canSync = registered && (subscriptionService.isPremium(user) || activeCount <= FREE_TIER_DEVICE_LIMIT);

        return DeviceStatusResponse.builder()
            .deviceId(deviceId)
            .registered(registered)
            .canSync(canSync)
            .premium(subscriptionService.isPremium(user))
            .activeDeviceCount(activeCount)
            .lastSeenAt(device != null ? device.getLastSeenAt() : null)
            .build();
    }

    private String normalizeDeviceId(String deviceId) {
        return deviceId.trim();
    }
}
