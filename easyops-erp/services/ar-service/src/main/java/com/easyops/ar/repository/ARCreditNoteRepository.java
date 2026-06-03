package com.easyops.ar.repository;

import com.easyops.ar.entity.ARCreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ARCreditNoteRepository extends JpaRepository<ARCreditNote, UUID> {
    
    List<ARCreditNote> findByOrganizationId(UUID organizationId);
    
    List<ARCreditNote> findByOrganizationIdAndStatus(UUID organizationId, ARCreditNote.CreditNoteStatus status);
    
    List<ARCreditNote> findByCustomerId(UUID customerId);
    
    List<ARCreditNote> findByInvoiceId(UUID invoiceId);
    
    List<ARCreditNote> findByCustomerIdAndCreditNoteDateBetween(UUID customerId, LocalDate startDate, LocalDate endDate);
    
    List<ARCreditNote> findByCustomerIdAndCreditNoteDateBefore(UUID customerId, LocalDate asOfDate);

    @Query("SELECT DISTINCT c FROM ARCreditNote c LEFT JOIN FETCH c.lines WHERE c.id = :id")
    Optional<ARCreditNote> findByIdWithLines(@Param("id") UUID id);
}

