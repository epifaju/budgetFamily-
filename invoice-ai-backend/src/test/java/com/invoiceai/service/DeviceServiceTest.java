package com.invoiceai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceai.domain.User;
import com.invoiceai.domain.UserDevice;
import com.invoiceai.dto.request.DeviceInfoRequest;
import com.invoiceai.exception.ForbiddenException;
import com.invoiceai.repository.UserDeviceRepository;
import com.invoiceai.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private DeviceService deviceService;

    private UUID userId;
    private User freeUser;
    private User premiumUser;
    private DeviceInfoRequest deviceRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        freeUser = User.builder().id(userId).email("free@test.com").premium(false).build();
        premiumUser = User.builder().id(userId).email("premium@test.com").premium(true).build();
        deviceRequest = DeviceInfoRequest.builder()
            .deviceId("device-a")
            .deviceName("Pixel Test")
            .platform("android")
            .build();
    }

    @Test
    void registersFirstDeviceForFreeUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(freeUser));
        when(userDeviceRepository.findByUserIdAndDeviceId(userId, "device-a")).thenReturn(Optional.empty());
        when(userDeviceRepository.countByUserIdAndActiveTrue(userId)).thenReturn(0L, 1L);
        when(subscriptionService.isPremium(freeUser)).thenReturn(false);
        when(userDeviceRepository.save(any(UserDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var status = deviceService.registerDevice(userId, deviceRequest);

        assertThat(status.isRegistered()).isTrue();
        assertThat(status.isCanSync()).isTrue();
        assertThat(status.getActiveDeviceCount()).isEqualTo(1);
        verify(userDeviceRepository).save(any(UserDevice.class));
    }

    @Test
    void rejectsSecondDeviceForFreeUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(freeUser));
        when(userDeviceRepository.findByUserIdAndDeviceId(userId, "device-b")).thenReturn(Optional.empty());
        when(userDeviceRepository.countByUserIdAndActiveTrue(userId)).thenReturn(1L);
        when(subscriptionService.isPremium(freeUser)).thenReturn(false);

        DeviceInfoRequest secondDevice = DeviceInfoRequest.builder()
            .deviceId("device-b")
            .platform("android")
            .build();

        assertThatThrownBy(() -> deviceService.registerDevice(userId, secondDevice))
            .isInstanceOf(ForbiddenException.class)
            .extracting("code")
            .isEqualTo(DeviceService.CODE_DEVICE_LIMIT);

        verify(userDeviceRepository, never()).save(any(UserDevice.class));
    }

    @Test
    void allowsSecondDeviceForPremiumUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(premiumUser));
        when(userDeviceRepository.findByUserIdAndDeviceId(userId, "device-b")).thenReturn(Optional.empty());
        when(userDeviceRepository.countByUserIdAndActiveTrue(userId)).thenReturn(1L, 2L);
        when(subscriptionService.isPremium(premiumUser)).thenReturn(true);
        when(userDeviceRepository.save(any(UserDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceInfoRequest secondDevice = DeviceInfoRequest.builder()
            .deviceId("device-b")
            .platform("ios")
            .build();

        var status = deviceService.registerDevice(userId, secondDevice);

        assertThat(status.isRegistered()).isTrue();
        assertThat(status.getActiveDeviceCount()).isEqualTo(2);
    }

    @Test
    void assertSyncAllowedUpdatesLastSeenForRegisteredDevice() {
        UserDevice registered = UserDevice.builder()
            .user(freeUser)
            .deviceId("device-a")
            .lastSeenAt(LocalDateTime.now().minusDays(1))
            .active(true)
            .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(freeUser));
        when(userDeviceRepository.findByUserIdAndDeviceIdAndActiveTrue(userId, "device-a"))
            .thenReturn(Optional.of(registered));
        when(userDeviceRepository.countByUserIdAndActiveTrue(userId)).thenReturn(1L);
        when(subscriptionService.isPremium(freeUser)).thenReturn(false);
        when(userDeviceRepository.save(registered)).thenReturn(registered);

        deviceService.assertSyncAllowed(userId, "device-a");

        verify(userDeviceRepository).save(registered);
    }
}
