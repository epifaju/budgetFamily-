package com.invoiceai.repository;

import com.invoiceai.domain.Budget;
import com.invoiceai.domain.enums.BudgetPeriod;
import com.invoiceai.domain.enums.CategoryType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByUserIdOrderByCategoryAsc(UUID userId);

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    Optional<Budget> findByUserIdAndCategoryAndPeriod(UUID userId, CategoryType category, BudgetPeriod period);

    boolean existsByUserIdAndCategoryAndPeriodAndIdNot(
        UUID userId,
        CategoryType category,
        BudgetPeriod period,
        UUID id
    );
}
