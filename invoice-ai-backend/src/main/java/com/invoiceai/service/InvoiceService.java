package com.invoiceai.service;

import com.invoiceai.domain.Invoice;
import com.invoiceai.domain.Item;
import com.invoiceai.domain.User;
import com.invoiceai.dto.request.CreateInvoiceRequest;
import com.invoiceai.dto.request.SyncBatchRequest;
import com.invoiceai.dto.request.UpdateInvoiceRequest;
import com.invoiceai.dto.response.InvoiceResponse;
import com.invoiceai.dto.response.ItemResponse;
import com.invoiceai.dto.response.SyncBatchItemResult;
import com.invoiceai.dto.response.SyncResponse;
import com.invoiceai.exception.ResourceNotFoundException;
import com.invoiceai.repository.InvoiceRepository;
import com.invoiceai.repository.ItemRepository;
import com.invoiceai.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ClassificationService classificationService;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> findAllByUserId(UUID userId, Pageable pageable) {
        return invoiceRepository.findByUserIdAndDeletedAtIsNull(userId, pageable)
            .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findById(UUID invoiceId, UUID userId) {
        Invoice invoice = invoiceRepository.findByIdAndUserId(invoiceId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));
        invoice.getItems().size(); // ensure lazy collection initialized
        return toResponse(invoice);
    }

    public InvoiceResponse create(CreateInvoiceRequest request, UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Invoice invoice = Invoice.builder()
            .user(user)
            .merchant(request.getMerchant())
            .date(request.getDate())
            .total(request.getTotal())
            .subtotal(request.getSubtotal())
            .tax(request.getTax())
            .paymentMethod(request.getPaymentMethod())
            .imageUrl(request.getImageUrl())
            .confidenceScore(request.getConfidenceScore())
            .items(new java.util.HashSet<>())
            .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);
        List<Item> items = mapItems(request, savedInvoice);
        items.forEach(itemRepository::save);
        entityManager.flush();
        entityManager.refresh(savedInvoice);

        return toResponse(savedInvoice);
    }

    public InvoiceResponse update(UUID invoiceId, UpdateInvoiceRequest request, UUID userId) {
        Invoice invoice = invoiceRepository.findByIdAndUserId(invoiceId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));

        if (request.getMerchant() != null) {
            invoice.setMerchant(request.getMerchant());
        }
        if (request.getDate() != null) {
            invoice.setDate(request.getDate());
        }
        if (request.getTotal() != null) {
            invoice.setTotal(request.getTotal());
        }
        if (request.getSubtotal() != null) {
            invoice.setSubtotal(request.getSubtotal());
        }
        if (request.getTax() != null) {
            invoice.setTax(request.getTax());
        }
        if (request.getPaymentMethod() != null) {
            invoice.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getImageUrl() != null) {
            invoice.setImageUrl(request.getImageUrl());
        }
        if (request.getConfidenceScore() != null) {
            invoice.setConfidenceScore(request.getConfidenceScore());
        }

        if (request.getItems() != null) {
            invoice.getItems().forEach(itemRepository::delete);
            invoice.getItems().clear();
            List<Item> newItems = mapItems(request, invoice);
            newItems.forEach(itemRepository::save);
        }

        Invoice updated = invoiceRepository.save(invoice);
        entityManager.flush();
        entityManager.refresh(updated);
        return toResponse(updated);
    }

    public void delete(UUID invoiceId, UUID userId) {
        Invoice invoice = invoiceRepository.findByIdAndUserId(invoiceId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Facture introuvable"));
        invoice.setDeletedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
    }

    public SyncResponse syncBatch(SyncBatchRequest request, UUID userId) {
        List<SyncBatchItemResult> results = new ArrayList<>();
        List<InvoiceResponse> syncedInvoices = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int synced = 0;
        int failed = 0;

        for (CreateInvoiceRequest invoiceRequest : request.getInvoices()) {
            SyncBatchItemResult.SyncBatchItemResultBuilder resultBuilder = SyncBatchItemResult.builder()
                .clientLocalId(invoiceRequest.getClientLocalId());
            try {
                InvoiceResponse created = create(invoiceRequest, userId);
                resultBuilder.success(true).invoice(created);
                syncedInvoices.add(created);
                synced += 1;
            } catch (Exception exception) {
                String message = exception.getMessage() != null
                    ? exception.getMessage()
                    : "Erreur lors de la synchronisation";
                resultBuilder.success(false).error(message);
                errors.add(message);
                failed += 1;
            }
            results.add(resultBuilder.build());
        }

        return SyncResponse.builder()
            .synced(synced)
            .failed(failed)
            .results(results)
            .invoices(syncedInvoices)
            .errors(errors)
            .build();
    }

    private List<Item> mapItems(CreateInvoiceRequest request, Invoice invoice) {
        List<Item> items = new ArrayList<>();
        request.getItems().forEach(itemRequest -> {
            Item item = Item.builder()
                .invoice(invoice)
                .name(itemRequest.getName())
                .quantity(itemRequest.getQuantity())
                .unitPrice(itemRequest.getUnitPrice())
                .totalPrice(itemRequest.getTotalPrice())
                .category(itemRequest.getCategory() != null ? itemRequest.getCategory()
                    : classificationService.classify(itemRequest.getName()))
                .confidenceScore(itemRequest.getConfidenceScore())
                .isCorrected(itemRequest.getIsCorrected())
                .build();
            invoice.getItems().add(item);
            items.add(item);
        });
        return items;
    }

    private List<Item> mapItems(UpdateInvoiceRequest request, Invoice invoice) {
        List<Item> items = new ArrayList<>();
        request.getItems().forEach(itemRequest -> {
            Item item = Item.builder()
                .invoice(invoice)
                .name(itemRequest.getName())
                .quantity(itemRequest.getQuantity())
                .unitPrice(itemRequest.getUnitPrice())
                .totalPrice(itemRequest.getTotalPrice())
                .category(itemRequest.getCategory() != null ? itemRequest.getCategory()
                    : classificationService.classify(itemRequest.getName()))
                .confidenceScore(itemRequest.getConfidenceScore())
                .isCorrected(itemRequest.getIsCorrected())
                .build();
            invoice.getItems().add(item);
            items.add(item);
        });
        return items;
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        List<ItemResponse> itemResponses = invoice.getItems().stream()
            .map(item -> ItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .category(item.getCategory())
                .confidenceScore(item.getConfidenceScore())
                .isCorrected(item.getIsCorrected())
                .build())
            .toList();

        return InvoiceResponse.builder()
            .id(invoice.getId())
            .merchant(invoice.getMerchant())
            .date(invoice.getDate())
            .total(invoice.getTotal())
            .subtotal(invoice.getSubtotal())
            .tax(invoice.getTax())
            .paymentMethod(invoice.getPaymentMethod())
            .imageUrl(invoice.getImageUrl())
            .confidenceScore(invoice.getConfidenceScore())
            .items(itemResponses)
            .createdAt(invoice.getCreatedAt())
            .updatedAt(invoice.getUpdatedAt())
            .build();
    }
}




