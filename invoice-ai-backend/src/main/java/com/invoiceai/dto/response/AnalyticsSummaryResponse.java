package com.invoiceai.dto.response;

import java.math.BigDecimal;
import java.util.Map;
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
public class AnalyticsSummaryResponse {

    private BigDecimal total;
    private Map<String, BigDecimal> byCategory;
    private Map<String, BigDecimal> evolution;
}




