package com.easyops.accounting.repository;

import com.easyops.accounting.entity.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface JournalLineRepository extends JpaRepository<JournalLine, UUID> {
    
    List<JournalLine> findByJournalEntry_IdOrderByLineNumber(UUID journalEntryId);

    List<JournalLine> findByAccountIdOrderByJournalEntry_Id(UUID accountId);

    @Query("SELECT COALESCE(SUM(jl.debitAmount), 0) FROM JournalLine jl WHERE jl.journalEntry.id = :journalEntryId")
    BigDecimal sumDebitAmountByJournalEntryId(@Param("journalEntryId") UUID journalEntryId);

    @Query("""
            SELECT jl.journalEntry.id, COALESCE(SUM(jl.debitAmount), 0)
            FROM JournalLine jl
            WHERE jl.journalEntry.id IN :journalEntryIds
            GROUP BY jl.journalEntry.id
            """)
    List<Object[]> sumDebitAmountsGroupedByJournalEntryId(@Param("journalEntryIds") Collection<UUID> journalEntryIds);

    @Query("""
            SELECT CASE WHEN COUNT(jl) > 0 THEN true ELSE false END
            FROM JournalLine jl JOIN jl.journalEntry je
            WHERE jl.accountId = :accountId AND je.status IN ('POSTED', 'REVERSED')
            """)
    boolean existsPostedOrReversedLinesForAccount(@Param("accountId") UUID accountId);
    
    @Query("SELECT jl FROM JournalLine jl JOIN FETCH jl.journalEntry je WHERE jl.accountId = :accountId AND je.periodId = :periodId ORDER BY je.journalDate")
    List<JournalLine> findByAccountAndPeriod(@Param("accountId") UUID accountId, @Param("periodId") UUID periodId);
    
    @Query("SELECT jl FROM JournalLine jl JOIN FETCH jl.journalEntry je " +
           "WHERE jl.accountId = :accountId AND je.journalDate BETWEEN :startDate AND :endDate " +
           "ORDER BY je.journalDate, je.journalNumber, jl.lineNumber")
    List<JournalLine> findByAccountIdAndDateRange(@Param("accountId") UUID accountId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);
}

