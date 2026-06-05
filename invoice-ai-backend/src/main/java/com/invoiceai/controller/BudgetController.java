package com.invoiceai.controller;

import com.invoiceai.dto.request.CreateBudgetRequest;
import com.invoiceai.dto.request.UpdateBudgetRequest;
import com.invoiceai.dto.response.BudgetResponse;
import com.invoiceai.dto.response.BudgetStatusResponse;
import com.invoiceai.security.SecurityUtils;
import com.invoiceai.service.BudgetService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> listBudgets() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(budgetService.findAllByUserId(userId));
    }

    @GetMapping("/status")
    public ResponseEntity<List<BudgetStatusResponse>> getBudgetStatus() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(budgetService.getStatus(userId));
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(@Valid @RequestBody CreateBudgetRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        BudgetResponse response = budgetService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(
        @PathVariable("id") UUID id,
        @Valid @RequestBody UpdateBudgetRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        BudgetResponse response = budgetService.update(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable("id") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        budgetService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
