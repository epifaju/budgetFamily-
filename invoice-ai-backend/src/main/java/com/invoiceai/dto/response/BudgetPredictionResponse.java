package com.invoiceai.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetPredictionResponse {

    private String monthKey;
    private String monthLabel;
    private int daysElapsed;
    private int daysInMonth;
    private BigDecimal currentSpent;
    private BigDecimal projectedTotal;
    private BigDecimal dailyAverage;
    private BigDecimal totalBudget;
    private BigDecimal projectedOverBudget;
    private String confidence;
    private String summaryMessage;
    private List<CategoryPredictionResponse> byCategory;
    private LocalDateTime generatedAt;
}
