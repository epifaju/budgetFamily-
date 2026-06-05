package com.invoiceai.ml;

import com.invoiceai.domain.Invoice;
import com.invoiceai.domain.Item;
import com.invoiceai.domain.enums.AnomalySeverity;
import com.invoiceai.domain.enums.AnomalyType;
import com.invoiceai.domain.enums.CategoryType;
import com.invoiceai.dto.response.AnomalyResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AnomalyDetector {

    private static final BigDecimal TOTAL_MISMATCH_TOLERANCE = new BigDecimal("0.50");
    private static final BigDecimal TOTAL_MISMATCH_PERCENT = new BigDecimal("0.01");
    private static final BigDecimal LOW_CONFIDENCE_THRESHOLD = new BigDecimal("0.70");
    private static final BigDecimal MIN_CATEGORY_SPIKE_AMOUNT = new BigDecimal("50.00");
    private static final BigDecimal MIN_LARGE_EXPENSE_AMOUNT = new BigDecimal("30.00");
    private static final BigDecimal MIN_MONTHLY_SPIKE_AMOUNT = new BigDecimal("100.00");
    private static final double SPIKE_RATIO = 1.5;
    private static final double LARGE_EXPENSE_MULTIPLIER = 2.0;
    private static final int TREND_MONTHS = 3;
    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");

    private static final Map<CategoryType, String> CATEGORY_LABELS = Map.of(
        CategoryType.ALIMENTAIRE, "Alimentaire",
        CategoryType.SANTE, "Santé",
        CategoryType.TRANSPORT, "Transport",
        CategoryType.VETEMENTS, "Vêtements",
        CategoryType.AUTRES, "Autres"
    );

    public List<AnomalyResponse> detect(List<Invoice> invoices) {
        List<AnomalyResponse> anomalies = new ArrayList<>();
        LocalDateTime detectedAt = LocalDateTime.now();

        for (Invoice invoice : invoices) {
            detectInvoiceAnomalies(invoice, detectedAt, anomalies);
        }

        detectCategorySpikes(invoices, detectedAt, anomalies);
        detectLargeExpenses(invoices, detectedAt, anomalies);
        detectMonthlySpike(invoices, detectedAt, anomalies);

        anomalies.sort(anomalyComparator());
        return anomalies;
    }

    private void detectInvoiceAnomalies(Invoice invoice, LocalDateTime detectedAt, List<AnomalyResponse> anomalies) {
        detectTotalMismatch(invoice, detectedAt, anomalies);
        detectLowConfidence(invoice, detectedAt, anomalies);
    }

    private void detectTotalMismatch(Invoice invoice, LocalDateTime detectedAt, List<AnomalyResponse> anomalies) {
        if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
            return;
        }

        BigDecimal itemsSum = invoice.getItems().stream()
            .map(item -> item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = invoice.getTotal() != null ? invoice.getTotal() : BigDecimal.ZERO;
        BigDecimal diff = itemsSum.subtract(total).abs();
        BigDecimal tolerance = total.multiply(TOTAL_MISMATCH_PERCENT).max(TOTAL_MISMATCH_TOLERANCE);

        if (diff.compareTo(tolerance) <= 0) {
            return;
        }

        String merchant = safeMerchant(invoice);
        anomalies.add(AnomalyResponse.builder()
            .type(AnomalyType.TOTAL_MISMATCH)
            .severity(AnomalySeverity.HIGH)
            .title("Incohérence de total")
            .message(String.format(
                "Total facture (%s €) différent de la somme des articles (%s €) — %s",
                formatAmount(total),
                formatAmount(itemsSum),
                merchant
            ))
            .amount(diff)
            .invoiceId(invoice.getId())
            .merchant(merchant)
            .detectedAt(detectedAt)
            .build());
    }

    private void detectLowConfidence(Invoice invoice, LocalDateTime detectedAt, List<AnomalyResponse> anomalies) {
        if (invoice.getConfidenceScore() == null) {
            return;
        }
        if (invoice.getConfidenceScore().compareTo(LOW_CONFIDENCE_THRESHOLD) >= 0) {
            return;
        }

        int percent = invoice.getConfidenceScore()
            .multiply(BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.HALF_UP)
            .intValue();
        String merchant = safeMerchant(invoice);

        anomalies.add(AnomalyResponse.builder()
            .type(AnomalyType.LOW_CONFIDENCE)
            .severity(AnomalySeverity.MEDIUM)
            .title("OCR peu fiable")
            .message(String.format("Confiance OCR %d %% — vérifiez la facture %s", percent, merchant))
            .amount(invoice.getTotal())
            .invoiceId(invoice.getId())
            .merchant(merchant)
            .detectedAt(detectedAt)
            .build());
    }

    private void detectCategorySpikes(List<Invoice> invoices, LocalDateTime detectedAt, List<AnomalyResponse> anomalies) {
        LocalDate today = LocalDate.now();
        String currentMonthKey = today.withDayOfMonth(1).format(MONTH_KEY);
        Map<CategoryType, BigDecimal> currentByCategory = new EnumMap<>(CategoryType.class);
        Map<CategoryType, List<BigDecimal>> historyByCategory = new EnumMap<>(CategoryType.class);

        for (CategoryType category : CategoryType.values()) {
            historyByCategory.put(category, new ArrayList<>());
        }

        for (int offset = 1; offset <= TREND_MONTHS; offset += 1) {
            String monthKey = today.withDayOfMonth(1).minusMonths(offset).format(MONTH_KEY);
            Map<CategoryType, BigDecimal> monthTotals = sumByCategoryForMonth(invoices, monthKey);
            for (CategoryType category : CategoryType.values()) {
                historyByCategory.get(category).add(monthTotals.getOrDefault(category, BigDecimal.ZERO));
            }
        }

        Map<CategoryType, BigDecimal> currentTotals = sumByCategoryForMonth(invoices, currentMonthKey);
        currentByCategory.putAll(currentTotals);

        for (CategoryType category : CategoryType.values()) {
            BigDecimal current = currentByCategory.getOrDefault(category, BigDecimal.ZERO);
            if (current.compareTo(MIN_CATEGORY_SPIKE_AMOUNT) < 0) {
                continue;
            }

            List<BigDecimal> history = historyByCategory.get(category);
            long monthsWithData = history.stream().filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0).count();
            if (monthsWithData < 2) {
                continue;
            }

            BigDecimal average = history.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(TREND_MONTHS), 2, RoundingMode.HALF_UP);

            if (average.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal ratio = current.divide(average, 2, RoundingMode.HALF_UP);
            if (ratio.compareTo(BigDecimal.valueOf(SPIKE_RATIO)) < 0) {
                continue;
            }

            int percentIncrease = current.subtract(average)
                .multiply(BigDecimal.valueOf(100))
                .divide(average, 0, RoundingMode.HALF_UP)
                .intValue();
            String label = CATEGORY_LABELS.getOrDefault(category, category.name());

            anomalies.add(AnomalyResponse.builder()
                .type(AnomalyType.CATEGORY_SPIKE)
                .severity(percentIncrease >= 100 ? AnomalySeverity.HIGH : AnomalySeverity.MEDIUM)
                .title("Pic de dépenses")
                .message(String.format(
                    "Dépense inhabituelle de %s € en %s ce mois (+%d %% vs moyenne)",
                    formatAmount(current),
                    label,
                    percentIncrease
                ))
                .category(category)
                .amount(current)
                .detectedAt(detectedAt)
                .build());
        }
    }

    private void detectLargeExpenses(List<Invoice> invoices, LocalDateTime detectedAt, List<AnomalyResponse> anomalies) {
        LocalDate today = LocalDate.now();
        String currentMonthKey = today.withDayOfMonth(1).format(MONTH_KEY);
        Map<CategoryType, List<BigDecimal>> amountsByCategory = new EnumMap<>(CategoryType.class);

        for (CategoryType category : CategoryType.values()) {
            amountsByCategory.put(category, new ArrayList<>());
        }

        for (Invoice invoice : invoices) {
            if (invoice.getItems() == null) {
                continue;
            }
            for (Item item : invoice.getItems()) {
                CategoryType category = item.getCategory() != null ? item.getCategory() : CategoryType.AUTRES;
                BigDecimal amount = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    amountsByCategory.get(category).add(amount);
                }
            }
        }

        for (Invoice invoice : invoices) {
            if (invoice.getDate() == null || !monthKey(invoice.getDate()).equals(currentMonthKey)) {
                continue;
            }
            if (invoice.getItems() == null) {
                continue;
            }

            for (Item item : invoice.getItems()) {
                CategoryType category = item.getCategory() != null ? item.getCategory() : CategoryType.AUTRES;
                BigDecimal amount = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
                if (amount.compareTo(MIN_LARGE_EXPENSE_AMOUNT) < 0) {
                    continue;
                }

                BigDecimal median = median(amountsByCategory.get(category));
                if (median.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal threshold = median.multiply(BigDecimal.valueOf(LARGE_EXPENSE_MULTIPLIER));
                if (amount.compareTo(threshold) < 0) {
                    continue;
                }

                String label = CATEGORY_LABELS.getOrDefault(category, category.name());
                String merchant = safeMerchant(invoice);

                anomalies.add(AnomalyResponse.builder()
                    .type(AnomalyType.LARGE_EXPENSE)
                    .severity(AnomalySeverity.MEDIUM)
                    .title("Achat atypique")
                    .message(String.format(
                        "Achat atypique de %s € en %s (%s) — %s",
                        formatAmount(amount),
                        label,
                        truncateItemName(item.getName()),
                        merchant
                    ))
                    .category(category)
                    .amount(amount)
                    .invoiceId(invoice.getId())
                    .merchant(merchant)
                    .detectedAt(detectedAt)
                    .build());
            }
        }
    }

    private void detectMonthlySpike(List<Invoice> invoices, LocalDateTime detectedAt, List<AnomalyResponse> anomalies) {
        LocalDate today = LocalDate.now();
        String currentMonthKey = today.withDayOfMonth(1).format(MONTH_KEY);
        BigDecimal currentTotal = sumInvoiceTotalsForMonth(invoices, currentMonthKey);

        if (currentTotal.compareTo(MIN_MONTHLY_SPIKE_AMOUNT) < 0) {
            return;
        }

        List<BigDecimal> history = new ArrayList<>();
        for (int offset = 1; offset <= TREND_MONTHS; offset += 1) {
            String monthKey = today.withDayOfMonth(1).minusMonths(offset).format(MONTH_KEY);
            history.add(sumInvoiceTotalsForMonth(invoices, monthKey));
        }

        long monthsWithData = history.stream().filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0).count();
        if (monthsWithData < 2) {
            return;
        }

        BigDecimal average = history.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(TREND_MONTHS), 2, RoundingMode.HALF_UP);

        if (average.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal ratio = currentTotal.divide(average, 2, RoundingMode.HALF_UP);
        if (ratio.compareTo(BigDecimal.valueOf(SPIKE_RATIO)) < 0) {
            return;
        }

        int percentIncrease = currentTotal.subtract(average)
            .multiply(BigDecimal.valueOf(100))
            .divide(average, 0, RoundingMode.HALF_UP)
            .intValue();

        anomalies.add(AnomalyResponse.builder()
            .type(AnomalyType.MONTHLY_SPIKE)
            .severity(percentIncrease >= 80 ? AnomalySeverity.HIGH : AnomalySeverity.MEDIUM)
            .title("Mois en hausse")
            .message(String.format(
                "Dépenses du mois en hausse de %d %% vs vos %d derniers mois (%s €)",
                percentIncrease,
                TREND_MONTHS,
                formatAmount(currentTotal)
            ))
            .amount(currentTotal)
            .detectedAt(detectedAt)
            .build());
    }

    private Map<CategoryType, BigDecimal> sumByCategoryForMonth(List<Invoice> invoices, String monthKey) {
        Map<CategoryType, BigDecimal> totals = new EnumMap<>(CategoryType.class);
        for (CategoryType category : CategoryType.values()) {
            totals.put(category, BigDecimal.ZERO);
        }

        for (Invoice invoice : invoices) {
            if (invoice.getDate() == null || !monthKey(invoice.getDate()).equals(monthKey)) {
                continue;
            }
            if (invoice.getItems() == null) {
                continue;
            }
            for (Item item : invoice.getItems()) {
                CategoryType category = item.getCategory() != null ? item.getCategory() : CategoryType.AUTRES;
                BigDecimal amount = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
                totals.merge(category, amount, BigDecimal::add);
            }
        }

        return totals;
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

    private String monthKey(LocalDate date) {
        return date.withDayOfMonth(1).format(MONTH_KEY);
    }

    private BigDecimal median(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<BigDecimal> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 0) {
            return sorted.get(middle - 1).add(sorted.get(middle))
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }
        return sorted.get(middle);
    }

    private Comparator<AnomalyResponse> anomalyComparator() {
        return Comparator
            .comparing((AnomalyResponse anomaly) -> severityRank(anomaly.getSeverity()))
            .thenComparing(
                anomaly -> anomaly.getAmount() != null ? anomaly.getAmount() : BigDecimal.ZERO,
                Comparator.reverseOrder()
            );
    }

    private int severityRank(AnomalySeverity severity) {
        if (severity == AnomalySeverity.HIGH) {
            return 0;
        }
        if (severity == AnomalySeverity.MEDIUM) {
            return 1;
        }
        return 2;
    }

    private String safeMerchant(Invoice invoice) {
        if (invoice.getMerchant() == null || invoice.getMerchant().isBlank()) {
            return "Marchand inconnu";
        }
        return invoice.getMerchant().trim();
    }

    private String truncateItemName(String name) {
        if (name == null || name.isBlank()) {
            return "Article";
        }
        String trimmed = name.trim();
        return trimmed.length() > 40 ? trimmed.substring(0, 37) + "..." : trimmed;
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
