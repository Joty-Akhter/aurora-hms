package com.easyops.sales.repository;

import com.easyops.sales.entity.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, UUID> {
    
    @Query("SELECT DISTINCT q FROM Quotation q LEFT JOIN FETCH q.lines WHERE q.organizationId = :orgId ORDER BY q.quotationDate DESC")
    List<Quotation> findByOrganizationIdOrderByQuotationDateDesc(@Param("orgId") UUID organizationId);
    
    @Query("SELECT DISTINCT q FROM Quotation q LEFT JOIN FETCH q.lines WHERE q.organizationId = :orgId AND q.status = :status")
    List<Quotation> findByOrganizationIdAndStatus(@Param("orgId") UUID organizationId, @Param("status") String status);
    
    @Query("SELECT DISTINCT q FROM Quotation q LEFT JOIN FETCH q.lines WHERE q.organizationId = :orgId AND q.customerId = :customerId")
    List<Quotation> findByOrganizationIdAndCustomerId(@Param("orgId") UUID organizationId, @Param("customerId") UUID customerId);
    
    @Query("SELECT q FROM Quotation q LEFT JOIN FETCH q.lines WHERE q.id = :id")
    Optional<Quotation> findByIdWithLines(@Param("id") UUID id);
    
    Optional<Quotation> findByOrganizationIdAndQuotationNumber(UUID organizationId, String quotationNumber);
    
    @Query("SELECT q FROM Quotation q WHERE q.organizationId = :orgId AND q.validUntil < :date AND q.status = 'SENT'")
    List<Quotation> findExpiredQuotations(@Param("orgId") UUID organizationId, @Param("date") LocalDate date);
    
    @Query("SELECT q FROM Quotation q WHERE q.organizationId = :orgId AND q.quotationDate BETWEEN :startDate AND :endDate ORDER BY q.quotationDate DESC")
    List<Quotation> findByDateRange(@Param("orgId") UUID organizationId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(q.quotationNumber, LENGTH(:prefix) + 1) AS INTEGER)), 0) FROM Quotation q WHERE q.organizationId = :orgId AND q.quotationNumber LIKE CONCAT(:prefix, '%')")
    Integer findMaxQuotationNumber(@Param("orgId") UUID organizationId, @Param("prefix") String prefix);
    
    boolean existsByOrganizationIdAndQuotationNumber(UUID organizationId, String quotationNumber);
}

