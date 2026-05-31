package com.invoiceai.service;

import com.invoiceai.dto.response.AnalyticsSummaryResponse;
import com.invoiceai.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int EVOLUTION_MONTHS = 6;
    private static final DateTimeFormatter MONTH_KEY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary(UUID userId) {
        java.util.List<com.invoiceai.domain.Invoice> invoices = invoiceRepository.findAllWithItemsByUserId(userId);

        BigDecimal total = invoices.stream()
            .map(invoice -> invoice.getTotal() != null ? invoice.getTotal() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> byCategory = new java.util.HashMap<>();
        invoices.forEach(invoice -> invoice.getItems().forEach(item -> {
            String key = item.getCategory() != null ? item.getCategory().name() : "AUTRES";
            BigDecimal amount = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
            byCategory.merge(key, amount, BigDecimal::add);
        }));

        Map<String, BigDecimal> evolution = buildEvolution(invoices);

        return AnalyticsSummaryResponse.builder()
            .total(total)
            .byCategory(byCategory)
            .evolution(evolution)
            .build();
    }

    private Map<String, BigDecimal> buildEvolution(java.util.List<com.invoiceai.domain.Invoice> invoices) {
        Map<String, BigDecimal> evolution = new LinkedHashMap<>();
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);

        for (int i = EVOLUTION_MONTHS - 1; i >= 0; i--) {
            String monthKey = currentMonth.minusMonths(i).format(MONTH_KEY_FORMAT);
            evolution.put(monthKey, BigDecimal.ZERO);
        }

        invoices.forEach(invoice -> {
            if (invoice.getDate() == null) {
                return;
            }
            String monthKey = invoice.getDate().withDayOfMonth(1).format(MONTH_KEY_FORMAT);
            if (!evolution.containsKey(monthKey)) {
                return;
            }
            BigDecimal amount = invoice.getTotal() != null ? invoice.getTotal() : BigDecimal.ZERO;
            evolution.merge(monthKey, amount, BigDecimal::add);
        });

        return evolution;
    }
}



