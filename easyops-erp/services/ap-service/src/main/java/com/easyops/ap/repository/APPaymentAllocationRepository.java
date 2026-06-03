package com.easyops.ap.repository;

import com.easyops.ap.entity.APPaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface APPaymentAllocationRepository extends JpaRepository<APPaymentAllocation, UUID> {
    List<APPaymentAllocation> findByPaymentId(UUID paymentId);
    List<APPaymentAllocation> findByBillId(UUID billId);
    
    @Query("SELECT SUM(a.allocatedAmount) FROM APPaymentAllocation a WHERE a.bill.id = :billId")
    BigDecimal sumAllocatedByBillId(@Param("billId") UUID billId);
    
    void deleteByPaymentId(UUID paymentId);
}

