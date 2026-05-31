package com.invoiceai.repository;

import com.invoiceai.domain.Invoice;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Page<Invoice> findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

    Optional<Invoice> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
        SELECT DISTINCT invoice
        FROM Invoice invoice
        LEFT JOIN FETCH invoice.items
        WHERE invoice.user.id = :userId
          AND invoice.deletedAt IS NULL
        """)
    List<Invoice> findAllWithItemsByUserId(@Param("userId") UUID userId);
}




