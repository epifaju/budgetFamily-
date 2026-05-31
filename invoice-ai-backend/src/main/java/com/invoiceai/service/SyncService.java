package com.invoiceai.service;

import com.invoiceai.dto.request.SyncBatchRequest;
import com.invoiceai.dto.response.SyncResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SyncService {

    private final InvoiceService invoiceService;

    public SyncResponse syncInvoices(SyncBatchRequest request, UUID userId) {
        return invoiceService.syncBatch(request, userId);
    }
}




