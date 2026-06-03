package com.easyops.ap.repository;

import com.easyops.ap.entity.APBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface APBillRepository extends JpaRepository<APBill, UUID> {
    List<APBill> findByOrganizationId(UUID organizationId);
    List<APBill> findByOrganizationIdAndStatus(UUID organizationId, String status);
    List<APBill> findByOrganizationIdAndPaymentStatus(UUID organizationId, String paymentStatus);
    Optional<APBill> findByBillNumber(String billNumber);
    List<APBill> findByVendorId(UUID vendorId);
    
    @Query("SELECT b FROM APBill b WHERE b.organizationId = :orgId AND b.status = 'POSTED' AND b.balanceDue > 0")
    List<APBill> findOutstandingBills(@Param("orgId") UUID organizationId);
    
    @Query("SELECT b FROM APBill b WHERE b.organizationId = :orgId AND b.dueDate < :date AND b.balanceDue > 0")
    List<APBill> findOverdueBills(@Param("orgId") UUID organizationId, @Param("date") LocalDate date);
    
    @Query("SELECT b FROM APBill b WHERE b.vendor.id = :vendorId AND b.billDate BETWEEN :startDate AND :endDate ORDER BY b.billDate")
    List<APBill> findByVendorIdAndBillDateBetween(@Param("vendorId") UUID vendorId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT b FROM APBill b WHERE b.vendor.id = :vendorId AND b.billDate < :asOfDate ORDER BY b.billDate")
    List<APBill> findByVendorIdAndBillDateBefore(@Param("vendorId") UUID vendorId, @Param("asOfDate") LocalDate asOfDate);
    
    boolean existsByBillNumber(String billNumber);

    @Query("SELECT DISTINCT b FROM APBill b LEFT JOIN FETCH b.lines WHERE b.id = :id")
    Optional<APBill> findByIdWithLines(@Param("id") UUID id);
}

