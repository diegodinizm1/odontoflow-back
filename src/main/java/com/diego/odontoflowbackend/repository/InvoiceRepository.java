package com.diego.odontoflowbackend.repository;

import com.diego.odontoflowbackend.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    List<Invoice> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<Invoice> findByExternalInvoiceId(String externalInvoiceId);
}
