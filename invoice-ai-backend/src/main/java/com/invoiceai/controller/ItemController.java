package com.invoiceai.controller;

import com.invoiceai.dto.request.FeedbackRequest;
import com.invoiceai.dto.response.ItemResponse;
import com.invoiceai.service.FeedbackService;
import com.invoiceai.service.ItemService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final FeedbackService feedbackService;

    @PutMapping("/{id}")
    public ResponseEntity<ItemResponse> updateItem(@PathVariable UUID id, @Valid @RequestBody ItemResponse payload) {
        ItemResponse response = itemService.updateItem(id, payload);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<Void> submitFeedback(@PathVariable UUID id, @Valid @RequestBody FeedbackRequest request) {
        feedbackService.submitFeedback(id, request);
        return ResponseEntity.noContent().build();
    }
}




