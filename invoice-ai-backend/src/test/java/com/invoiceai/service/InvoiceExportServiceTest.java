package com.invoiceai.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.invoiceai.domain.Invoice;
import com.invoiceai.domain.Item;
import com.invoiceai.domain.enums.CategoryType;
import com.invoiceai.domain.enums.PaymentMethod;
import com.invoiceai.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceExportServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceExportService invoiceExportService;

    @Test
    void exportCsvContainsHeadersAndSemicolonSeparator() {
        Invoice invoice = Invoice.builder()
            .merchant("Carrefour")
            .date(LocalDate.of(2026, 3, 1))
            .total(new BigDecimal("12.50"))
            .paymentMethod(PaymentMethod.CARD)
            .items(Set.of(
                Item.builder()
                    .name("Pain")
                    .category(CategoryType.ALIMENTAIRE)
                    .quantity(BigDecimal.ONE)
                    .totalPrice(new BigDecimal("12.50"))
                    .build()
            ))
            .build();

        UUID userId = UUID.randomUUID();
        when(invoiceRepository.findAllWithItemsByUserId(userId)).thenReturn(List.of(invoice));

        byte[] csv = invoiceExportService.exportCsv(userId, null, null);
        String content = new String(csv, StandardCharsets.UTF_8);

        assertTrue(content.contains("Date;Marchand;Article;Catégorie"));
        assertTrue(content.contains("Carrefour"));
        assertTrue(content.contains("Pain"));
    }

    @Test
    void exportPdfStartsWithPdfHeader() {
        UUID userId = UUID.randomUUID();
        when(invoiceRepository.findAllWithItemsByUserId(userId)).thenReturn(List.of());

        byte[] pdf = invoiceExportService.exportPdf(userId, null, null);
        assertTrue(pdf.length > 4);
        assertTrue(pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F');
    }
}
