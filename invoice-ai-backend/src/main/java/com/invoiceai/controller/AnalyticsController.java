package com.invoiceai.controller;

import com.invoiceai.dto.response.AnomaliesResponse;
import com.invoiceai.dto.response.AnalyticsSummaryResponse;
import com.invoiceai.dto.response.BudgetPredictionResponse;
import com.invoiceai.security.SecurityUtils;
import com.invoiceai.service.AnalyticsService;
import com.invoiceai.service.AnomalyService;
import com.invoiceai.service.PredictionService;
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
    private final AnomalyService anomalyService;
    private final PredictionService predictionService;

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryResponse> getSummary() {
        UUID userId = SecurityUtils.getCurrentUserId();
        AnalyticsSummaryResponse response = analyticsService.getSummary(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/anomalies")
    public ResponseEntity<AnomaliesResponse> getAnomalies() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(anomalyService.getAnomalies(userId));
    }

    @GetMapping("/predictions")
    public ResponseEntity<BudgetPredictionResponse> getPredictions() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(predictionService.getPredictions(userId));
    }
}




