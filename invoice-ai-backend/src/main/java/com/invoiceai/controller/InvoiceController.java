package com.invoiceai.controller;

import com.invoiceai.dto.request.CreateInvoiceRequest;
import com.invoiceai.dto.request.SyncBatchRequest;
import com.invoiceai.dto.request.UpdateInvoiceRequest;
import com.invoiceai.dto.response.InvoiceResponse;
import com.invoiceai.dto.response.SyncResponse;
import com.invoiceai.security.SecurityUtils;
import com.invoiceai.service.InvoiceService;
import com.invoiceai.service.SyncService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final SyncService syncService;

    @GetMapping
    public ResponseEntity<Page<InvoiceResponse>> listInvoices(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "date,desc") String sort
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<InvoiceResponse> response = invoiceService.findAllByUserId(userId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(invoiceService.findById(id, userId));
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        InvoiceResponse response = invoiceService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceResponse> updateInvoice(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateInvoiceRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        InvoiceResponse response = invoiceService.update(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        invoiceService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch")
    public ResponseEntity<SyncResponse> syncBatch(@Valid @RequestBody SyncBatchRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        SyncResponse response = syncService.syncInvoices(request, userId);
        return ResponseEntity.ok(response);
    }

    private Sort parseSort(String sort) {
        String[] sortParts = sort.split(",");
        if (sortParts.length == 2 && sortParts[1].equalsIgnoreCase("desc")) {
            return Sort.by(sortParts[0]).descending();
        }
        return Sort.by(sortParts[0]).ascending();
    }
}




