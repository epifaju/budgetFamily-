package com.invoiceai.service;

import com.invoiceai.domain.Feedback;
import com.invoiceai.domain.Item;
import com.invoiceai.domain.User;
import com.invoiceai.dto.request.FeedbackRequest;
import com.invoiceai.exception.ResourceNotFoundException;
import com.invoiceai.repository.FeedbackRepository;
import com.invoiceai.repository.ItemRepository;
import com.invoiceai.repository.UserRepository;
import com.invoiceai.security.SecurityUtils;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Transactional
    public void submitFeedback(UUID itemId, FeedbackRequest request) {
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Article introuvable"));

        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        item.setCategory(request.getCorrectedCategory());
        item.setIsCorrected(true);
        itemRepository.save(item);

        Feedback feedback = Feedback.builder()
            .item(item)
            .user(user)
            .originalCategory(item.getCategory())
            .correctedCategory(request.getCorrectedCategory())
            .build();
        feedbackRepository.save(feedback);
    }
}



