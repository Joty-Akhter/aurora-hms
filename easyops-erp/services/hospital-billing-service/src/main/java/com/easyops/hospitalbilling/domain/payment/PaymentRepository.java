package com.easyops.hospitalbilling.domain.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByInvoiceId(UUID invoiceId);

    List<Payment> findByPaymentDateBetween(OffsetDateTime from, OffsetDateTime to);
}

