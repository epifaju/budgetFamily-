package com.invoiceai.dto.response;

import com.invoiceai.domain.enums.BudgetPeriod;
import com.invoiceai.domain.enums.CategoryType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
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
public class BudgetStatusResponse {

    private UUID id;
    private CategoryType category;
    private BudgetPeriod period;
    private BigDecimal amount;
    private BigDecimal spent;
    private int percentUsed;
    private String alertLevel;
    private LocalDate periodStart;
    private LocalDate periodEnd;
}
