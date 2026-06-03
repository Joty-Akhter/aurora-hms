package com.easyops.accounting.repository;

import com.easyops.accounting.entity.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {
    
    Page<JournalEntry> findByOrganizationIdOrderByJournalDateDesc(UUID organizationId, Pageable pageable);
    
    Optional<JournalEntry> findByJournalNumber(String journalNumber);
    
    List<JournalEntry> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    Page<JournalEntry> findByOrganizationIdAndPeriodId(UUID organizationId, UUID periodId, Pageable pageable);
    
    @Query("SELECT j FROM JournalEntry j WHERE j.organizationId = :orgId AND j.journalDate BETWEEN :startDate AND :endDate ORDER BY j.journalDate, j.journalNumber")
    List<JournalEntry> findByDateRange(@Param("orgId") UUID organizationId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT j FROM JournalEntry j WHERE j.organizationId = :orgId AND j.status = 'POSTED' AND j.periodId = :periodId")
    List<JournalEntry> findPostedEntriesForPeriod(@Param("orgId") UUID organizationId, @Param("periodId") UUID periodId);
    
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(j.journalNumber, 3) AS INTEGER)), 0) FROM JournalEntry j WHERE j.journalNumber LIKE :prefix%")
    Integer findMaxJournalNumber(@Param("prefix") String prefix);
    
    List<JournalEntry> findTop10ByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<JournalEntry> findTop10ByOrganizationIdAndStatusOrderByPostedAtDesc(
            UUID organizationId, String status);
}

