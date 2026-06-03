package com.easyops.hospitalbilling.domain.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvoiceDiscountLineRepository extends JpaRepository<InvoiceDiscountLine, UUID> {

    List<InvoiceDiscountLine> findByInvoiceId(UUID invoiceId);
}
