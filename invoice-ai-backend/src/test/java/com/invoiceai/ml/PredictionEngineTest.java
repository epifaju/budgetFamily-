package com.invoiceai.ml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.invoiceai.domain.Invoice;
import com.invoiceai.domain.Item;
import com.invoiceai.domain.enums.CategoryType;
import com.invoiceai.dto.response.BudgetPredictionResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PredictionEngineTest {

    private final PredictionEngine engine = new PredictionEngine();

    @Test
    void projectsEndOfMonthFromCurrentRunRate() {
        LocalDate today = LocalDate.now();
        LocalDate dayFive = today.withDayOfMonth(Math.min(5, today.lengthOfMonth()));

        Invoice invoice = invoiceWithItems(
            "Super U",
            dayFive,
            new BigDecimal("100.00"),
            item("Courses", CategoryType.ALIMENTAIRE, new BigDecimal("100.00"))
        );

        BudgetPredictionResponse prediction = engine.predict(List.of(invoice), List.of());

        assertTrue(prediction.getProjectedTotal().compareTo(new BigDecimal("100.00")) > 0);
        assertTrue(prediction.getSummaryMessage().contains("Projection fin de mois"));
    }

    @Test
    void computesProjectedOverBudgetWhenBudgetsExist() {
        LocalDate today = LocalDate.now();
        int day = Math.max(3, Math.min(10, today.getDayOfMonth()));
        LocalDate invoiceDate = today.withDayOfMonth(day);

        Invoice invoice = invoiceWithItems(
            "Super U",
            invoiceDate,
            new BigDecimal("200.00"),
            item("Courses", CategoryType.ALIMENTAIRE, new BigDecimal("200.00"))
        );

        com.invoiceai.domain.Budget budget = com.invoiceai.domain.Budget.builder()
            .category(CategoryType.ALIMENTAIRE)
            .amount(new BigDecimal("250.00"))
            .period(com.invoiceai.domain.enums.BudgetPeriod.MONTHLY)
            .startDate(today.withDayOfMonth(1))
            .build();

        BudgetPredictionResponse prediction = engine.predict(List.of(invoice), List.of(budget));

        assertNotNull(prediction.getTotalBudget());
        assertEquals(new BigDecimal("250.00"), prediction.getTotalBudget());
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
