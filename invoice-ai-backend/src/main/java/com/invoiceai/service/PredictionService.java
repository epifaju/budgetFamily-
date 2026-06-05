package com.invoiceai.service;

import com.invoiceai.dto.response.BudgetPredictionResponse;
import com.invoiceai.ml.PredictionEngine;
import com.invoiceai.repository.BudgetRepository;
import com.invoiceai.repository.InvoiceRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PredictionService {

    private final InvoiceRepository invoiceRepository;
    private final BudgetRepository budgetRepository;
    private final PredictionEngine predictionEngine;

    @Transactional(readOnly = true)
    public BudgetPredictionResponse getPredictions(UUID userId) {
        return predictionEngine.predict(
            invoiceRepository.findAllWithItemsByUserId(userId),
            budgetRepository.findByUserIdOrderByCategoryAsc(userId)
        );
    }
}
