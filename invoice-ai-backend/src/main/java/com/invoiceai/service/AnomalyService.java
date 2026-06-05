package com.invoiceai.service;

import com.invoiceai.dto.response.AnomaliesResponse;
import com.invoiceai.dto.response.AnomalyResponse;
import com.invoiceai.ml.AnomalyDetector;
import com.invoiceai.repository.InvoiceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnomalyService {

    private final InvoiceRepository invoiceRepository;
    private final AnomalyDetector anomalyDetector;

    @Transactional(readOnly = true)
    public AnomaliesResponse getAnomalies(UUID userId) {
        List<AnomalyResponse> anomalies = anomalyDetector.detect(
            invoiceRepository.findAllWithItemsByUserId(userId)
        );

        return AnomaliesResponse.builder()
            .anomalies(anomalies)
            .generatedAt(LocalDateTime.now())
            .build();
    }
}
