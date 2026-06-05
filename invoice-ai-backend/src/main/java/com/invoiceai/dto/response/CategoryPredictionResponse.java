package com.invoiceai.dto.response;

import com.invoiceai.domain.enums.CategoryType;
import java.math.BigDecimal;
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
public class CategoryPredictionResponse {

    private CategoryType category;
    private BigDecimal currentSpent;
    private BigDecimal projectedTotal;
    private BigDecimal budgetAmount;
    private BigDecimal projectedOverBudget;
}
