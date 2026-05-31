package com.invoiceai.dto.request;

import com.invoiceai.domain.enums.CategoryType;
import com.invoiceai.domain.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class CreateInvoiceRequest {

    @NotBlank
    private String merchant;

    @NotNull
    private LocalDate date;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal total;

    private BigDecimal subtotal;

    private BigDecimal tax;

    private PaymentMethod paymentMethod;

    private String imageUrl;

    private BigDecimal confidenceScore;

    @Valid
    @NotEmpty
    private List<ItemRequest> items;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemRequest {

        @NotBlank
        @Size(max = 500)
        private String name;

        @NotNull
        private BigDecimal quantity;

        private BigDecimal unitPrice;

        @NotNull
        private BigDecimal totalPrice;

        @NotNull
        private CategoryType category;

        private BigDecimal confidenceScore;

        private Boolean isCorrected;
    }
}



