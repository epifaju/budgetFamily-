package com.invoiceai.service;

import com.invoiceai.domain.Invoice;
import com.invoiceai.domain.Item;
import com.invoiceai.domain.enums.CategoryType;
import com.invoiceai.repository.InvoiceRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceExportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private static final Map<CategoryType, String> CATEGORY_LABELS = Map.of(
        CategoryType.ALIMENTAIRE, "Alimentaire",
        CategoryType.SANTE, "Santé",
        CategoryType.TRANSPORT, "Transport",
        CategoryType.VETEMENTS, "Vêtements",
        CategoryType.AUTRES, "Autres"
    );

    private final InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public byte[] exportCsv(UUID userId, LocalDate fromDate, LocalDate toDate) {
        List<Invoice> invoices = loadInvoices(userId, fromDate, toDate);
        StringBuilder csv = new StringBuilder();
        csv.append("Date;Marchand;Article;Catégorie;Quantité;Prix unitaire;Montant ligne;Total facture;Paiement\n");

        for (Invoice invoice : invoices) {
            appendInvoiceRows(csv, invoice);
        }

        byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[UTF8_BOM.length + content.length];
        System.arraycopy(UTF8_BOM, 0, withBom, 0, UTF8_BOM.length);
        System.arraycopy(content, 0, withBom, UTF8_BOM.length, content.length);
        return withBom;
    }

    @Transactional(readOnly = true)
    public byte[] exportPdf(UUID userId, LocalDate fromDate, LocalDate toDate) {
        List<Invoice> invoices = loadInvoices(userId, fromDate, toDate);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            document.add(new Paragraph("InvoiceAI — Export des dépenses", titleFont));
            document.add(new Paragraph(buildPeriodLabel(fromDate, toDate), metaFont));
            document.add(new Paragraph(
                "Généré le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                metaFont
            ));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            table.setWidths(new float[] {10f, 14f, 22f, 10f, 8f, 10f, 10f, 10f, 10f});
            addHeaderCell(table, "Date", headerFont);
            addHeaderCell(table, "Marchand", headerFont);
            addHeaderCell(table, "Article", headerFont);
            addHeaderCell(table, "Catégorie", headerFont);
            addHeaderCell(table, "Qté", headerFont);
            addHeaderCell(table, "Prix unit.", headerFont);
            addHeaderCell(table, "Montant", headerFont);
            addHeaderCell(table, "Total fact.", headerFont);
            addHeaderCell(table, "Paiement", headerFont);

            for (Invoice invoice : invoices) {
                addInvoiceRows(table, invoice, cellFont);
            }

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Synthèse par catégorie", titleFont));
            document.add(buildCategorySummaryTable(invoices, headerFont, cellFont));

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException exception) {
            throw new IllegalStateException("Impossible de générer le PDF", exception);
        }
    }

    public String buildCsvFilename(LocalDate fromDate, LocalDate toDate) {
        return "invoiceai-export-" + FILE_DATE_FORMAT.format(LocalDate.now()) + buildRangeSuffix(fromDate, toDate) + ".csv";
    }

    public String buildPdfFilename(LocalDate fromDate, LocalDate toDate) {
        return "invoiceai-export-" + FILE_DATE_FORMAT.format(LocalDate.now()) + buildRangeSuffix(fromDate, toDate) + ".pdf";
    }

    private List<Invoice> loadInvoices(UUID userId, LocalDate fromDate, LocalDate toDate) {
        List<Invoice> invoices = new ArrayList<>(invoiceRepository.findAllWithItemsByUserId(userId));
        invoices.removeIf(invoice -> !matchesDateRange(invoice, fromDate, toDate));
        invoices.sort(Comparator.comparing(Invoice::getDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return invoices;
    }

    private boolean matchesDateRange(Invoice invoice, LocalDate fromDate, LocalDate toDate) {
        if (invoice.getDate() == null) {
            return false;
        }
        if (fromDate != null && invoice.getDate().isBefore(fromDate)) {
            return false;
        }
        if (toDate != null && invoice.getDate().isAfter(toDate)) {
            return false;
        }
        return true;
    }

    private void appendInvoiceRows(StringBuilder csv, Invoice invoice) {
        String date = formatDate(invoice.getDate());
        String merchant = escapeCsv(invoice.getMerchant());
        String payment = escapeCsv(invoice.getPaymentMethod() != null ? invoice.getPaymentMethod().name() : "");
        String invoiceTotal = formatAmount(invoice.getTotal());

        if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
            csv.append(date).append(';')
                .append(merchant).append(';')
                .append(";;")
                .append("1;;")
                .append(invoiceTotal).append(';')
                .append(invoiceTotal).append(';')
                .append(payment).append('\n');
            return;
        }

        for (Item item : invoice.getItems()) {
            csv.append(date).append(';')
                .append(merchant).append(';')
                .append(escapeCsv(item.getName())).append(';')
                .append(escapeCsv(categoryLabel(item.getCategory()))).append(';')
                .append(formatQuantity(item.getQuantity())).append(';')
                .append(formatAmount(item.getUnitPrice())).append(';')
                .append(formatAmount(item.getTotalPrice())).append(';')
                .append(invoiceTotal).append(';')
                .append(payment).append('\n');
        }
    }

    private void addInvoiceRows(PdfPTable table, Invoice invoice, Font cellFont) {
        String date = formatDate(invoice.getDate());
        String merchant = safeText(invoice.getMerchant());
        String payment = invoice.getPaymentMethod() != null ? invoice.getPaymentMethod().name() : "";
        String invoiceTotal = formatAmount(invoice.getTotal());

        if (invoice.getItems() == null || invoice.getItems().isEmpty()) {
            addCell(table, date, cellFont);
            addCell(table, merchant, cellFont);
            addCell(table, "", cellFont);
            addCell(table, "", cellFont);
            addCell(table, "1", cellFont);
            addCell(table, "", cellFont);
            addCell(table, invoiceTotal, cellFont);
            addCell(table, invoiceTotal, cellFont);
            addCell(table, payment, cellFont);
            return;
        }

        for (Item item : invoice.getItems()) {
            addCell(table, date, cellFont);
            addCell(table, merchant, cellFont);
            addCell(table, safeText(item.getName()), cellFont);
            addCell(table, categoryLabel(item.getCategory()), cellFont);
            addCell(table, formatQuantity(item.getQuantity()), cellFont);
            addCell(table, formatAmount(item.getUnitPrice()), cellFont);
            addCell(table, formatAmount(item.getTotalPrice()), cellFont);
            addCell(table, invoiceTotal, cellFont);
            addCell(table, payment, cellFont);
        }
    }

    private PdfPTable buildCategorySummaryTable(List<Invoice> invoices, Font headerFont, Font cellFont)
        throws DocumentException {
        Map<CategoryType, BigDecimal> totals = new EnumMap<>(CategoryType.class);
        for (CategoryType category : CategoryType.values()) {
            totals.put(category, BigDecimal.ZERO);
        }

        for (Invoice invoice : invoices) {
            if (invoice.getItems() == null) {
                continue;
            }
            for (Item item : invoice.getItems()) {
                CategoryType category = item.getCategory() != null ? item.getCategory() : CategoryType.AUTRES;
                BigDecimal amount = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
                totals.merge(category, amount, BigDecimal::add);
            }
        }

        PdfPTable summary = new PdfPTable(2);
        summary.setWidthPercentage(40);
        addHeaderCell(summary, "Catégorie", headerFont);
        addHeaderCell(summary, "Total", headerFont);

        for (CategoryType category : CategoryType.values()) {
            BigDecimal total = totals.get(category);
            if (total.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            addCell(summary, categoryLabel(category), cellFont);
            addCell(summary, formatAmount(total) + " €", cellFont);
        }

        return summary;
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new java.awt.Color(230, 230, 230));
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Font font) {
        table.addCell(new Phrase(text, font));
    }

    private String buildPeriodLabel(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null && toDate == null) {
            return "Période : toutes les factures";
        }
        if (fromDate != null && toDate != null) {
            return "Période : du " + DATE_FORMAT.format(fromDate) + " au " + DATE_FORMAT.format(toDate);
        }
        if (fromDate != null) {
            return "Période : à partir du " + DATE_FORMAT.format(fromDate);
        }
        return "Période : jusqu'au " + DATE_FORMAT.format(toDate);
    }

    private String buildRangeSuffix(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null && toDate == null) {
            return "";
        }
        StringBuilder suffix = new StringBuilder();
        if (fromDate != null) {
            suffix.append("-from-").append(fromDate);
        }
        if (toDate != null) {
            suffix.append("-to-").append(toDate);
        }
        return suffix.toString();
    }

    private String categoryLabel(CategoryType category) {
        if (category == null) {
            return CATEGORY_LABELS.get(CategoryType.AUTRES);
        }
        return CATEGORY_LABELS.getOrDefault(category, category.name());
    }

    private String formatDate(LocalDate date) {
        return date != null ? DATE_FORMAT.format(date) : "";
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private String formatQuantity(BigDecimal quantity) {
        if (quantity == null) {
            return "1";
        }
        return quantity.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private String escapeCsv(String value) {
        String safe = value != null ? value.replace("\"", "\"\"") : "";
        if (safe.contains(";") || safe.contains("\"") || safe.contains("\n")) {
            return "\"" + safe + "\"";
        }
        return safe;
    }

    private String safeText(String value) {
        return value != null ? value.trim() : "";
    }
}
