package com.easyops.hospitalbilling.domain.charge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChargeLineRepository extends JpaRepository<ChargeLine, UUID>, JpaSpecificationExecutor<ChargeLine> {

    Page<ChargeLine> findByPatientId(UUID patientId, Pageable pageable);

    Page<ChargeLine> findByPatientIdAndStatusIn(UUID patientId, List<String> statuses, Pageable pageable);

    List<ChargeLine> findByInvoiceId(UUID invoiceId);

    Optional<ChargeLine> findByIdempotencyKey(String idempotencyKey);

    List<ChargeLine> findBySourceServiceAndSourceReferenceId(String sourceService, String sourceReferenceId);
}

