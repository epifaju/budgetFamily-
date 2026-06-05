package com.invoiceai.dto.response;

import com.invoiceai.domain.Feedback;
import com.invoiceai.domain.Invoice;
import com.invoiceai.domain.Item;
import com.invoiceai.domain.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
public class UserDataExportResponse {

    private LocalDateTime exportedAt;
    private ProfileExport profile;
    private List<InvoiceExport> invoices;
    private List<FeedbackExport> classificationFeedback;

    public static UserDataExportResponse from(User user, List<Invoice> invoices, List<Feedback> feedback) {
        List<InvoiceExport> invoiceExports = new ArrayList<>();
        for (Invoice invoice : invoices) {
            List<ItemExport> items = new ArrayList<>();
            for (Item item : invoice.getItems()) {
                items.add(ItemExport.builder()
                    .id(item.getId())
                    .name(item.getName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .totalPrice(item.getTotalPrice())
                    .category(item.getCategory() != null ? item.getCategory().name() : null)
                    .confidenceScore(item.getConfidenceScore())
                    .isCorrected(item.getIsCorrected())
                    .build());
            }
            invoiceExports.add(InvoiceExport.builder()
                .id(invoice.getId())
                .merchant(invoice.getMerchant())
                .date(invoice.getDate())
                .total(invoice.getTotal())
                .subtotal(invoice.getSubtotal())
                .tax(invoice.getTax())
                .paymentMethod(invoice.getPaymentMethod() != null ? invoice.getPaymentMethod().name() : null)
                .imageUrl(invoice.getImageUrl())
                .confidenceScore(invoice.getConfidenceScore())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .items(items)
                .build());
        }

        List<FeedbackExport> feedbackExports = new ArrayList<>();
        for (Feedback entry : feedback) {
            feedbackExports.add(FeedbackExport.builder()
                .id(entry.getId())
                .itemId(entry.getItem() != null ? entry.getItem().getId() : null)
                .originalCategory(entry.getOriginalCategory() != null ? entry.getOriginalCategory().name() : null)
                .correctedCategory(entry.getCorrectedCategory().name())
                .createdAt(entry.getCreatedAt())
                .build());
        }

        return UserDataExportResponse.builder()
            .exportedAt(LocalDateTime.now())
            .profile(ProfileExport.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .privacyConsentAt(user.getPrivacyConsentAt())
                .lastLogin(user.getLastLogin())
                .build())
            .invoices(invoiceExports)
            .classificationFeedback(feedbackExports)
            .build();
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileExport {
        private UUID id;
        private String email;
        private String name;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime privacyConsentAt;
        private LocalDateTime lastLogin;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceExport {
        private UUID id;
        private String merchant;
        private LocalDate date;
        private BigDecimal total;
        private BigDecimal subtotal;
        private BigDecimal tax;
        private String paymentMethod;
        private String imageUrl;
        private BigDecimal confidenceScore;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<ItemExport> items;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemExport {
        private UUID id;
        private String name;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String category;
        private BigDecimal confidenceScore;
        private Boolean isCorrected;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackExport {
        private UUID id;
        private UUID itemId;
        private String originalCategory;
        private String correctedCategory;
        private LocalDateTime createdAt;
    }
}
