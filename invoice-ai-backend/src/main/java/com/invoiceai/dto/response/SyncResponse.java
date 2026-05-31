package com.invoiceai.dto.response;

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
public class SyncResponse {

    private int synced;
    private int failed;
    private List<InvoiceResponse> invoices;
    private List<String> errors;
}




