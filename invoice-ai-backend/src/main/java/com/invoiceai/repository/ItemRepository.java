package com.invoiceai.repository;

import com.invoiceai.domain.Item;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, UUID> {
    List<Item> findByInvoiceId(UUID invoiceId);
}




