package com.invoiceai.service;

import com.invoiceai.domain.Budget;
import com.invoiceai.domain.Invoice;
import com.invoiceai.domain.User;
import com.invoiceai.domain.enums.BudgetPeriod;
import com.invoiceai.domain.enums.CategoryType;
import com.invoiceai.dto.request.CreateBudgetRequest;
import com.invoiceai.dto.request.UpdateBudgetRequest;
import com.invoiceai.dto.response.BudgetResponse;
import com.invoiceai.dto.response.BudgetStatusResponse;
import com.invoiceai.exception.ResourceNotFoundException;
import com.invoiceai.exception.ValidationException;
import com.invoiceai.repository.BudgetRepository;
import com.invoiceai.repository.InvoiceRepository;
import com.invoiceai.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private static final int WARNING_THRESHOLD_PERCENT = 80;

    private final BudgetRepository budgetRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<BudgetResponse> findAllByUserId(UUID userId) {
        return budgetRepository.findByUserIdOrderByCategoryAsc(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<BudgetStatusResponse> getStatus(UUID userId) {
        List<Budget> budgets = budgetRepository.findByUserIdOrderByCategoryAsc(userId);
        List<Invoice> invoices = invoiceRepository.findAllWithItemsByUserId(userId);

        return budgets.stream()
            .map(budget -> toStatusResponse(budget, invoices))
            .toList();
    }

    public BudgetResponse create(CreateBudgetRequest request, UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        if (budgetRepository.findByUserIdAndCategoryAndPeriod(userId, request.getCategory(), request.getPeriod()).isPresent()) {
            throw new ValidationException(
                "Un budget existe déjà pour cette catégorie et cette période"
            );
        }

        LocalDate startDate = request.getStartDate() != null
            ? request.getStartDate()
            : LocalDate.now().withDayOfMonth(1);

        Budget budget = Budget.builder()
            .user(user)
            .category(request.getCategory())
            .amount(request.getAmount())
            .period(request.getPeriod())
            .startDate(startDate)
            .build();

        return toResponse(budgetRepository.save(budget));
    }

    public BudgetResponse update(UUID budgetId, UpdateBudgetRequest request, UUID userId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Budget introuvable"));

        if (request.getAmount() != null) {
            budget.setAmount(request.getAmount());
        }
        if (request.getStartDate() != null) {
            budget.setStartDate(request.getStartDate());
        }

        return toResponse(budgetRepository.save(budget));
    }

    public void delete(UUID budgetId, UUID userId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Budget introuvable"));
        budgetRepository.delete(budget);
    }

    private BudgetStatusResponse toStatusResponse(Budget budget, List<Invoice> invoices) {
        LocalDate periodStart = resolvePeriodStart(budget.getPeriod());
        LocalDate periodEnd = resolvePeriodEnd(budget.getPeriod());
        BigDecimal spent = computeSpent(invoices, budget.getCategory(), periodStart, periodEnd);
        BigDecimal limit = budget.getAmount() != null ? budget.getAmount() : BigDecimal.ZERO;

        int percentUsed = 0;
        if (limit.compareTo(BigDecimal.ZERO) > 0) {
            percentUsed = spent.multiply(BigDecimal.valueOf(100))
                .divide(limit, 0, RoundingMode.HALF_UP)
                .intValue();
        }

        String alertLevel = resolveAlertLevel(percentUsed);

        return BudgetStatusResponse.builder()
            .id(budget.getId())
            .category(budget.getCategory())
            .period(budget.getPeriod())
            .amount(limit)
            .spent(spent)
            .percentUsed(percentUsed)
            .alertLevel(alertLevel)
            .periodStart(periodStart)
            .periodEnd(periodEnd)
            .build();
    }

    private String resolveAlertLevel(int percentUsed) {
        if (percentUsed >= 100) {
            return "EXCEEDED";
        }
        if (percentUsed >= WARNING_THRESHOLD_PERCENT) {
            return "WARNING";
        }
        return "OK";
    }

    private BigDecimal computeSpent(
        List<Invoice> invoices,
        CategoryType category,
        LocalDate periodStart,
        LocalDate periodEnd
    ) {
        BigDecimal spent = BigDecimal.ZERO;

        for (Invoice invoice : invoices) {
            if (invoice.getDate() == null) {
                continue;
            }
            if (invoice.getDate().isBefore(periodStart) || invoice.getDate().isAfter(periodEnd)) {
                continue;
            }
            for (var item : invoice.getItems()) {
                CategoryType itemCategory = item.getCategory() != null ? item.getCategory() : CategoryType.AUTRES;
                if (itemCategory != category) {
                    continue;
                }
                BigDecimal amount = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
                spent = spent.add(amount);
            }
        }

        return spent;
    }

    private LocalDate resolvePeriodStart(BudgetPeriod period) {
        LocalDate today = LocalDate.now();
        if (period == BudgetPeriod.WEEKLY) {
            return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        return today.withDayOfMonth(1);
    }

    private LocalDate resolvePeriodEnd(BudgetPeriod period) {
        LocalDate today = LocalDate.now();
        if (period == BudgetPeriod.WEEKLY) {
            return today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        }
        return today.withDayOfMonth(today.lengthOfMonth());
    }

    private BudgetResponse toResponse(Budget budget) {
        return BudgetResponse.builder()
            .id(budget.getId())
            .category(budget.getCategory())
            .amount(budget.getAmount())
            .period(budget.getPeriod())
            .startDate(budget.getStartDate())
            .createdAt(budget.getCreatedAt())
            .build();
    }
}
