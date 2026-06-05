package com.invoiceai.service;

import com.invoiceai.domain.Feedback;
import com.invoiceai.domain.Invoice;
import com.invoiceai.domain.User;
import com.invoiceai.dto.response.UserDataExportResponse;
import com.invoiceai.exception.ResourceNotFoundException;
import com.invoiceai.repository.FeedbackRepository;
import com.invoiceai.repository.InvoiceRepository;
import com.invoiceai.repository.UserRepository;
import com.invoiceai.security.SecurityUtils;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDataExportService {

    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final FeedbackRepository feedbackRepository;

    @Transactional(readOnly = true)
    public UserDataExportResponse exportCurrentUser() {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ResourceNotFoundException("Utilisateur introuvable");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        List<Invoice> invoices = invoiceRepository.findAllWithItemsByUserId(userId);
        List<Feedback> feedback = feedbackRepository.findByUser_Id(userId);

        return UserDataExportResponse.from(user, invoices, feedback);
    }
}
