package com.easyops.hospitalbilling.domain.invoice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    Page<Invoice> findByPatientId(UUID patientId, Pageable pageable);

    Page<Invoice> findByVisitId(UUID visitId, Pageable pageable);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}

