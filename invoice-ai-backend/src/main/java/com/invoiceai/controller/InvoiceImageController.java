package com.invoiceai.controller;

import com.invoiceai.dto.response.ImageUploadResponse;
import com.invoiceai.security.SecurityUtils;
import com.invoiceai.service.DeviceService;
import com.invoiceai.service.ReceiptImageStorageService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceImageController {

    private final ReceiptImageStorageService receiptImageStorageService;
    private final DeviceService deviceService;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadReceiptImage(
        @RequestHeader(name = DeviceService.HEADER_DEVICE_ID, required = false) String deviceId,
        @RequestParam("file") MultipartFile file,
        HttpServletRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        deviceService.assertSyncAllowed(userId, deviceId);
        String imageUrl = receiptImageStorageService.store(userId, file, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ImageUploadResponse.builder().imageUrl(imageUrl).build()
        );
    }
}
