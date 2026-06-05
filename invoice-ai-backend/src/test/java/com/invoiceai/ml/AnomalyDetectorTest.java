package com.invoiceai.ml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.invoiceai.domain.Invoice;
import com.invoiceai.domain.Item;
import com.invoiceai.domain.enums.AnomalyType;
import com.invoiceai.domain.enums.CategoryType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnomalyDetectorTest {

    private final AnomalyDetector detector = new AnomalyDetector();

    @Test
    void detectTotalMismatchWhenItemsSumDiffersFromTotal() {
        Invoice invoice = invoiceWithItems(
            "Carrefour",
            LocalDate.now(),
            new BigDecimal("50.00"),
            item("Pain", CategoryType.ALIMENTAIRE, new BigDecimal("10.00")),
            item("Lait", CategoryType.ALIMENTAIRE, new BigDecimal("15.00"))
        );

        List<com.invoiceai.dto.response.AnomalyResponse> anomalies = detector.detect(List.of(invoice));

        assertTrue(anomalies.stream().anyMatch(a -> a.getType() == AnomalyType.TOTAL_MISMATCH));
    }

    @Test
    void detectLowConfidenceWhenScoreBelowThreshold() {
        Invoice invoice = invoiceWithItems(
            "Pharmacie",
            LocalDate.now(),
            new BigDecimal("25.00"),
            item("Médicament", CategoryType.SANTE, new BigDecimal("25.00"))
        );
        invoice.setConfidenceScore(new BigDecimal("0.55"));

        List<com.invoiceai.dto.response.AnomalyResponse> anomalies = detector.detect(List.of(invoice));

        assertEquals(1, anomalies.stream().filter(a -> a.getType() == AnomalyType.LOW_CONFIDENCE).count());
    }

    private Invoice invoiceWithItems(String merchant, LocalDate date, BigDecimal total, Item... items) {
        Invoice invoice = Invoice.builder()
            .merchant(merchant)
            .date(date)
            .total(total)
            .items(new java.util.HashSet<>())
            .build();
        for (Item item : items) {
            item.setInvoice(invoice);
            invoice.getItems().add(item);
        }
        return invoice;
    }

    private Item item(String name, CategoryType category, BigDecimal totalPrice) {
        return Item.builder()
            .name(name)
            .category(category)
            .quantity(BigDecimal.ONE)
            .totalPrice(totalPrice)
            .build();
    }
}
