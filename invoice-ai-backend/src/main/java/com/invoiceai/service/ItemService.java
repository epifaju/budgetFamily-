package com.invoiceai.service;

import com.invoiceai.domain.Item;
import com.invoiceai.dto.response.ItemResponse;
import com.invoiceai.exception.ResourceNotFoundException;
import com.invoiceai.repository.ItemRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    @Transactional
    public ItemResponse updateItem(UUID id, ItemResponse payload) {
        Item item = itemRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Article introuvable"));

        item.setName(payload.getName());
        item.setQuantity(payload.getQuantity());
        item.setUnitPrice(payload.getUnitPrice());
        item.setTotalPrice(payload.getTotalPrice());
        item.setCategory(payload.getCategory());
        item.setConfidenceScore(payload.getConfidenceScore());
        item.setIsCorrected(Boolean.TRUE.equals(payload.getIsCorrected()));

        Item saved = itemRepository.save(item);
        return toResponse(saved);
    }

    private ItemResponse toResponse(Item item) {
        return ItemResponse.builder()
            .id(item.getId())
            .name(item.getName())
            .quantity(item.getQuantity())
            .unitPrice(item.getUnitPrice())
            .totalPrice(item.getTotalPrice())
            .category(item.getCategory())
            .confidenceScore(item.getConfidenceScore())
            .isCorrected(item.getIsCorrected())
            .build();
    }
}




