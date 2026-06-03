package com.easyops.hospitalbilling.domain.adjustment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdjustmentRepository extends JpaRepository<Adjustment, UUID> {

    List<Adjustment> findByInvoiceId(UUID invoiceId);
}

