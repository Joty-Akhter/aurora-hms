package com.easyops.hospitalcard.domain.account;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardTransactionRepository extends JpaRepository<CardTransaction, UUID>, JpaSpecificationExecutor<CardTransaction> {

    Page<CardTransaction> findByCardAccountIdOrderByCreatedAtDesc(UUID cardAccountId, Pageable pageable);

    Optional<CardTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<CardTransaction> findByExternalReferenceIdAndSourceSystem(String externalReferenceId, String sourceSystem);

    /**
     * Aggregate committed transactions by source_system in date range.
     */
    @Query("SELECT t.sourceSystem AS sourceSystem, SUM(t.amount) AS totalAmount, COUNT(t) AS transactionCount " +
        "FROM CardTransaction t WHERE t.status = 'COMMITTED' AND t.sourceSystem IS NOT NULL " +
        "AND (:from IS NULL OR t.postedAt >= :from) AND (:to IS NULL OR t.postedAt <= :to) " +
        "AND (:sourceSystem IS NULL OR :sourceSystem = '' OR t.sourceSystem = :sourceSystem) " +
        "GROUP BY t.sourceSystem")
    List<UsageByDomainProjection> usageByDomain(
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to,
        @Param("sourceSystem") String sourceSystem);

    /**
     * Balance as at date: sum of committed transaction amounts per account where posted_at (or created_at) <= asOf.
     * Returns only accounts with balance > 0. Used for point-in-time liabilities report.
     */
    @Query("SELECT t.cardAccountId, SUM(t.amount) FROM CardTransaction t WHERE t.status = 'COMMITTED' " +
        "AND ((t.postedAt IS NOT NULL AND t.postedAt <= :asOf) OR (t.postedAt IS NULL AND t.createdAt <= :asOf)) " +
        "GROUP BY t.cardAccountId HAVING SUM(t.amount) > 0")
    List<Object[]> findBalanceAsAtByAccount(@Param("asOf") OffsetDateTime asOf);
}
