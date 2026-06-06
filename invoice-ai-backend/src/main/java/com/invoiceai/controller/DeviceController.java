package com.invoiceai.controller;

import com.invoiceai.dto.request.DeviceInfoRequest;
import com.invoiceai.dto.response.DeviceStatusResponse;
import com.invoiceai.security.SecurityUtils;
import com.invoiceai.service.DeviceService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping("/register")
    public ResponseEntity<DeviceStatusResponse> registerDevice(
        @Valid @RequestBody DeviceInfoRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(deviceService.registerDevice(userId, request));
    }

    @GetMapping("/me")
    public ResponseEntity<DeviceStatusResponse> getMyDeviceStatus(
        @RequestHeader(name = DeviceService.HEADER_DEVICE_ID, required = false) String deviceId
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(deviceService.getDeviceStatus(userId, deviceId));
    }
}
