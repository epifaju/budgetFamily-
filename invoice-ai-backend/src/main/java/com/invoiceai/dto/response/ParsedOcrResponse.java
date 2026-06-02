package com.invoiceai.dto.response;

import java.math.BigDecimal;
import java.util.ArrayList;
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
public class ParsedOcrResponse {

    private String merchant;
    private String date;
    @Builder.Default
    private List<ParsedOcrItemResponse> items = new ArrayList<>();
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;
    private double confidenceScore;
    private String source;
}
