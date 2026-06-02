package com.invoiceai.dto.response;

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
public class SyncBatchItemResult {

    private String clientLocalId;
    private boolean success;
    private InvoiceResponse invoice;
    private String error;
}
