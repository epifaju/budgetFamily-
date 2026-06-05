package com.invoiceai.ml;

import com.invoiceai.domain.Budget;
import com.invoiceai.domain.Invoice;
import com.invoiceai.domain.Item;
import com.invoiceai.domain.enums.BudgetPeriod;
import com.invoiceai.domain.enums.CategoryType;
import com.invoiceai.dto.response.BudgetPredictionResponse;
import com.invoiceai.dto.response.CategoryPredictionResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PredictionEngine {

    private static final int HISTORY_MONTHS = 3;
    private static final int MIN_DAYS_FOR_RUN_RATE = 3;
    private static final double RUN_RATE_WEIGHT = 0.65;
    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String[] MONTH_NAMES = {
        "janvier", "février", "mars", "avril", "mai", "juin",
        "juillet", "août", "septembre", "octobre", "novembre", "décembre"
    };

    private static final Map<CategoryType, String> CATEGORY_LABELS = Map.of(
        CategoryType.ALIMENTAIRE, "Alimentaire",
        CategoryType.SANTE, "Santé",
        CategoryType.TRANSPORT, "Transport",
        CategoryType.VETEMENTS, "Vêtements",
        CategoryType.AUTRES, "Autres"
    );

    public BudgetPredictionResponse predict(List<Invoice> invoices, List<Budget> budgets) {
        LocalDate today = LocalDate.now();
        String monthKey = today.withDayOfMonth(1).format(MONTH_KEY);
        int daysElapsed = today.getDayOfMonth();
        int daysInMonth = today.lengthOfMonth();

        BigDecimal currentSpent = sumInvoiceTotalsForMonth(invoices, monthKey);
        BigDecimal historicalAverage = averageMonthlyInvoiceTotal(invoices, today);
        BigDecimal projectedTotal = projectEndOfMonth(currentSpent, daysElapsed, daysInMonth, historicalAverage);
        BigDecimal dailyAverage = daysElapsed > 0
            ? currentSpent.divide(BigDecimal.valueOf(daysElapsed), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        BigDecimal totalBudget = sumMonthlyBudgets(budgets);
        BigDecimal projectedOverBudget = computeOverBudget(projectedTotal, totalBudget);
        String confidence = resolveConfidence(invoices, monthKey, daysElapsed, historicalAverage);

        List<CategoryPredictionResponse> byCategory = buildCategoryPredictions(
            invoices,
            budgets,
            today,
            daysElapsed,
            daysInMonth
        );

        return BudgetPredictionResponse.builder()
            .monthKey(monthKey)
            .monthLabel(formatMonthLabel(monthKey))
            .daysElapsed(daysElapsed)
            .daysInMonth(daysInMonth)
            .currentSpent(currentSpent)
            .projectedTotal(projectedTotal)
            .dailyAverage(dailyAverage)
            .totalBudget(totalBudget.compareTo(BigDecimal.ZERO) > 0 ? totalBudget : null)
            .projectedOverBudget(projectedOverBudget)
            .confidence(confidence)
            .summaryMessage(buildSummaryMessage(projectedTotal, projectedOverBudget, totalBudget))
            .byCategory(byCategory)
            .generatedAt(LocalDateTime.now())
            .build();
    }

    private List<CategoryPredictionResponse> buildCategoryPredictions(
        List<Invoice> invoices,
        List<Budget> budgets,
        LocalDate today,
        int daysElapsed,
        int daysInMonth
    ) {
        String monthKey = today.withDayOfMonth(1).format(MONTH_KEY);
        Map<CategoryType, BigDecimal> budgetByCategory = monthlyBudgetMap(budgets);
        List<CategoryPredictionResponse> predictions = new ArrayList<>();

        for (CategoryType category : CategoryType.values()) {
            BigDecimal current = sumCategoryItemsForMonth(invoices, monthKey, category);
            BigDecimal historicalAverage = averageMonthlyCategorySpend(invoices, today, category);
            BigDecimal projected = projectEndOfMonth(current, daysElapsed, daysInMonth, historicalAverage);
            BigDecimal budgetAmount = budgetByCategory.getOrDefault(category, BigDecimal.ZERO);

            if (current.compareTo(BigDecimal.ZERO) <= 0
                && projected.compareTo(BigDecimal.ZERO) <= 0
                && budgetAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            predictions.add(CategoryPredictionResponse.builder()
                .category(category)
                .currentSpent(current)
                .projectedTotal(projected)
                .budgetAmount(budgetAmount.compareTo(BigDecimal.ZERO) > 0 ? budgetAmount : null)
                .projectedOverBudget(computeOverBudget(projected, budgetAmount))
                .build());
        }

        predictions.sort((left, right) -> {
            BigDecimal leftOver = left.getProjectedOverBudget() != null ? left.getProjectedOverBudget() : BigDecimal.ZERO;
            BigDecimal rightOver = right.getProjectedOverBudget() != null ? right.getProjectedOverBudget() : BigDecimal.ZERO;
            return rightOver.compareTo(leftOver);
        });

        return predictions;
    }

    private BigDecimal projectEndOfMonth(
        BigDecimal currentSpent,
        int daysElapsed,
        int daysInMonth,
        BigDecimal historicalAverage
    ) {
        BigDecimal runRateProjection = BigDecimal.ZERO;
        if (daysElapsed >= MIN_DAYS_FOR_RUN_RATE && currentSpent.compareTo(BigDecimal.ZERO) > 0) {
            runRateProjection = currentSpent
                .multiply(BigDecimal.valueOf(daysInMonth))
                .divide(BigDecimal.valueOf(daysElapsed), 2, RoundingMode.HALF_UP);
        }

        if (runRateProjection.compareTo(BigDecimal.ZERO) > 0 && historicalAverage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal weighted = runRateProjection.multiply(BigDecimal.valueOf(RUN_RATE_WEIGHT))
                .add(historicalAverage.multiply(BigDecimal.valueOf(1 - RUN_RATE_WEIGHT)));
            return weighted.setScale(2, RoundingMode.HALF_UP);
        }

        if (runRateProjection.compareTo(BigDecimal.ZERO) > 0) {
            return runRateProjection;
        }

        if (historicalAverage.compareTo(BigDecimal.ZERO) > 0) {
            return historicalAverage.setScale(2, RoundingMode.HALF_UP);
        }

        return currentSpent.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal averageMonthlyInvoiceTotal(List<Invoice> invoices, LocalDate today) {
        List<BigDecimal> totals = new ArrayList<>();
        for (int offset = 1; offset <= HISTORY_MONTHS; offset += 1) {
            String monthKey = today.withDayOfMonth(1).minusMonths(offset).format(MONTH_KEY);
            totals.add(sumInvoiceTotalsForMonth(invoices, monthKey));
        }
        return average(totals);
    }

    private BigDecimal averageMonthlyCategorySpend(List<Invoice> invoices, LocalDate today, CategoryType category) {
        List<BigDecimal> totals = new ArrayList<>();
        for (int offset = 1; offset <= HISTORY_MONTHS; offset += 1) {
            String monthKey = today.withDayOfMonth(1).minusMonths(offset).format(MONTH_KEY);
            totals.add(sumCategoryItemsForMonth(invoices, monthKey, category));
        }
        return average(totals);
    }

    private BigDecimal sumInvoiceTotalsForMonth(List<Invoice> invoices, String monthKey) {
        BigDecimal total = BigDecimal.ZERO;
        for (Invoice invoice : invoices) {
            if (invoice.getDate() == null || !monthKey(invoice.getDate()).equals(monthKey)) {
                continue;
            }
            total = total.add(invoice.getTotal() != null ? invoice.getTotal() : BigDecimal.ZERO);
        }
        return total;
    }

    private BigDecimal sumCategoryItemsForMonth(List<Invoice> invoices, String monthKey, CategoryType category) {
        BigDecimal total = BigDecimal.ZERO;
        for (Invoice invoice : invoices) {
            if (invoice.getDate() == null || !monthKey(invoice.getDate()).equals(monthKey)) {
                continue;
            }
            if (invoice.getItems() == null) {
                continue;
            }
            for (Item item : invoice.getItems()) {
                CategoryType itemCategory = item.getCategory() != null ? item.getCategory() : CategoryType.AUTRES;
                if (itemCategory != category) {
                    continue;
                }
                total = total.add(item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO);
            }
        }
        return total;
    }

    private Map<CategoryType, BigDecimal> monthlyBudgetMap(List<Budget> budgets) {
        Map<CategoryType, BigDecimal> map = new EnumMap<>(CategoryType.class);
        for (Budget budget : budgets) {
            if (budget.getPeriod() != BudgetPeriod.MONTHLY) {
                continue;
            }
            map.put(budget.getCategory(), budget.getAmount() != null ? budget.getAmount() : BigDecimal.ZERO);
        }
        return map;
    }

    private BigDecimal sumMonthlyBudgets(List<Budget> budgets) {
        return budgets.stream()
            .filter(budget -> budget.getPeriod() == BudgetPeriod.MONTHLY)
            .map(budget -> budget.getAmount() != null ? budget.getAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal computeOverBudget(BigDecimal projected, BigDecimal budget) {
        if (budget == null || budget.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal over = projected.subtract(budget);
        return over.compareTo(BigDecimal.ZERO) > 0 ? over.setScale(2, RoundingMode.HALF_UP) : null;
    }

    private String resolveConfidence(
        List<Invoice> invoices,
        String monthKey,
        int daysElapsed,
        BigDecimal historicalAverage
    ) {
        long invoicesThisMonth = invoices.stream()
            .filter(invoice -> invoice.getDate() != null && monthKey(invoice.getDate()).equals(monthKey))
            .count();

        if (daysElapsed >= 10 && invoicesThisMonth >= 5) {
            return "HIGH";
        }
        if (daysElapsed >= 5 || historicalAverage.compareTo(BigDecimal.ZERO) > 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String buildSummaryMessage(
        BigDecimal projectedTotal,
        BigDecimal projectedOverBudget,
        BigDecimal totalBudget
    ) {
        String projection = formatAmount(projectedTotal);
        if (projectedOverBudget != null && totalBudget.compareTo(BigDecimal.ZERO) > 0) {
            return String.format(
                Locale.FRENCH,
                "Projection fin de mois : %s € (dépassement %s €)",
                projection,
                formatAmount(projectedOverBudget)
            );
        }
        return String.format(Locale.FRENCH, "Projection fin de mois : %s €", projection);
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private String monthKey(LocalDate date) {
        return date.withDayOfMonth(1).format(MONTH_KEY);
    }

    private String formatMonthLabel(String monthKey) {
        String[] parts = monthKey.split("-");
        if (parts.length != 2) {
            return monthKey;
        }
        int monthIndex = Integer.parseInt(parts[1]) - 1;
        if (monthIndex < 0 || monthIndex > 11) {
            return monthKey;
        }
        return MONTH_NAMES[monthIndex] + " " + parts[0];
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public String categoryLabel(CategoryType category) {
        return CATEGORY_LABELS.getOrDefault(category, category.name());
    }
}
