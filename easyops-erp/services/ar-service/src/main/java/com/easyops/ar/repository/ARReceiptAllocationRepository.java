package com.easyops.ar.repository;

import com.easyops.ar.entity.ARReceiptAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ARReceiptAllocationRepository extends JpaRepository<ARReceiptAllocation, UUID> {
    
    List<ARReceiptAllocation> findByReceiptId(UUID receiptId);
    
    List<ARReceiptAllocation> findByInvoiceId(UUID invoiceId);
    
    @Query("SELECT SUM(a.allocatedAmount) FROM ARReceiptAllocation a WHERE a.invoice.id = :invoiceId")
    BigDecimal sumAllocatedByInvoiceId(@Param("invoiceId") UUID invoiceId);
    
    void deleteByReceiptId(UUID receiptId);
}

