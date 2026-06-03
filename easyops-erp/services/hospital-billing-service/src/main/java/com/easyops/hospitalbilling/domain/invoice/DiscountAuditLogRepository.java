package com.easyops.hospitalbilling.domain.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiscountAuditLogRepository extends JpaRepository<DiscountAuditLog, UUID> {

    List<DiscountAuditLog> findByInvoiceId(UUID invoiceId);
}
