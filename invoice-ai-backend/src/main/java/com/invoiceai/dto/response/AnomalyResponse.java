package com.invoiceai.dto.response;

import com.invoiceai.domain.enums.AnomalySeverity;
import com.invoiceai.domain.enums.AnomalyType;
import com.invoiceai.domain.enums.CategoryType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class AnomalyResponse {

    private AnomalyType type;
    private AnomalySeverity severity;
    private String title;
    private String message;
    private CategoryType category;
    private BigDecimal amount;
    private UUID invoiceId;
    private String merchant;
    private LocalDateTime detectedAt;
}
