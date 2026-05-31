package com.invoiceai.controller;

import com.invoiceai.dto.response.AnalyticsSummaryResponse;
import com.invoiceai.security.SecurityUtils;
import com.invoiceai.service.AnalyticsService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryResponse> getSummary() {
        UUID userId = SecurityUtils.getCurrentUserId();
        AnalyticsSummaryResponse response = analyticsService.getSummary(userId);
        return ResponseEntity.ok(response);
    }
}




