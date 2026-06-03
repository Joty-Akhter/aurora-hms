package com.easyops.ar.repository;

import com.easyops.ar.entity.ARInvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ARInvoiceLineRepository extends JpaRepository<ARInvoiceLine, UUID> {
    
    List<ARInvoiceLine> findByInvoiceId(UUID invoiceId);
    
    void deleteByInvoiceId(UUID invoiceId);
}

