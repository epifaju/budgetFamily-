package com.invoiceai.dto.response;

import com.invoiceai.domain.enums.CategoryType;
import java.math.BigDecimal;
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
public class ItemResponse {

    private UUID id;
    private String name;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private CategoryType category;
    private BigDecimal confidenceScore;
    private Boolean isCorrected;
}



