package com.easyops.ap.repository;

import com.easyops.ap.entity.APPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface APPaymentRepository extends JpaRepository<APPayment, UUID> {
    List<APPayment> findByOrganizationId(UUID organizationId);
    List<APPayment> findByOrganizationIdAndStatus(UUID organizationId, String status);
    Optional<APPayment> findByPaymentNumber(String paymentNumber);
    List<APPayment> findByVendorId(UUID vendorId);
    
    @Query("SELECT p FROM APPayment p WHERE p.organizationId = :orgId AND p.status = 'POSTED'")
    List<APPayment> findPostedPayments(@Param("orgId") UUID organizationId);
    
    @Query("SELECT p FROM APPayment p WHERE p.vendor.id = :vendorId AND p.paymentDate BETWEEN :startDate AND :endDate ORDER BY p.paymentDate")
    List<APPayment> findByVendorIdAndPaymentDateBetween(@Param("vendorId") UUID vendorId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT p FROM APPayment p WHERE p.vendor.id = :vendorId AND p.paymentDate < :asOfDate ORDER BY p.paymentDate")
    List<APPayment> findByVendorIdAndPaymentDateBefore(@Param("vendorId") UUID vendorId, @Param("asOfDate") LocalDate asOfDate);
    
    boolean existsByPaymentNumber(String paymentNumber);
}

