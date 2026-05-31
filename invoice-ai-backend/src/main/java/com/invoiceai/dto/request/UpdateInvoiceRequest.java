package com.invoiceai.dto.request;

import com.invoiceai.domain.enums.CategoryType;
import com.invoiceai.domain.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class UpdateInvoiceRequest {

    private String merchant;

    private LocalDate date;

    @DecimalMin("0.0")
    private BigDecimal total;

    private BigDecimal subtotal;

    private BigDecimal tax;

    private PaymentMethod paymentMethod;

    private String imageUrl;

    private BigDecimal confidenceScore;

    @Valid
    private List<ItemUpdateRequest> items;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemUpdateRequest {

        private String id;

        @Size(max = 500)
        private String name;

        private BigDecimal quantity;

        private BigDecimal unitPrice;

        private BigDecimal totalPrice;

        private CategoryType category;

        private BigDecimal confidenceScore;

        private Boolean isCorrected;
    }
}



