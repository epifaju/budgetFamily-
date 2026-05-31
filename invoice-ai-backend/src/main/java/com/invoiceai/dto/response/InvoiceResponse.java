package com.invoiceai.dto.response;

import com.invoiceai.domain.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
public class InvoiceResponse {

    private UUID id;
    private String merchant;
    private LocalDate date;
    private BigDecimal total;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private PaymentMethod paymentMethod;
    private String imageUrl;
    private BigDecimal confidenceScore;
    private List<ItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}




