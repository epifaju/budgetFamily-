package com.invoiceai.controller;

import com.invoiceai.dto.request.SyncBatchRequest;
import com.invoiceai.dto.response.SyncResponse;
import com.invoiceai.security.SecurityUtils;
import com.invoiceai.service.DeviceService;
import com.invoiceai.service.SyncService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;
    private final DeviceService deviceService;

    @PostMapping
    public ResponseEntity<SyncResponse> sync(
        @RequestHeader(name = DeviceService.HEADER_DEVICE_ID, required = false) String deviceId,
        @Valid @RequestBody SyncBatchRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        deviceService.assertSyncAllowed(userId, deviceId);
        SyncResponse response = syncService.syncInvoices(request, userId);
        return ResponseEntity.ok(response);
    }
}

