package com.easyops.hospitalbilling.domain.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

    List<Refund> findByOriginalPaymentId(UUID originalPaymentId);

    List<Refund> findByInvoiceId(UUID invoiceId);
}

