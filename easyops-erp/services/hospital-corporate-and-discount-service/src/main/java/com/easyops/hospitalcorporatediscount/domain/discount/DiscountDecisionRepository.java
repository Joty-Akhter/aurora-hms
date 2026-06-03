package com.easyops.hospitalcorporatediscount.domain.discount;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface DiscountDecisionRepository extends JpaRepository<DiscountDecision, UUID> {

    List<DiscountDecision> findByBillContextId(String billContextId);

    List<DiscountDecision> findByPatientIdOrderByCreatedAtDesc(UUID patientId, org.springframework.data.domain.Pageable pageable);

    List<DiscountDecision> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    long countByCorporateClientIdAndCreatedAtBetween(UUID corporateClientId, OffsetDateTime from, OffsetDateTime to);

    /** Returns [corporateClientId, count] per row. */
    @Query("SELECT d.corporateClientId, COUNT(d) FROM DiscountDecision d WHERE d.createdAt BETWEEN :from AND :to AND d.corporateClientId IS NOT NULL GROUP BY d.corporateClientId")
    List<Object[]> countByCorporateAndCreatedAtBetween(OffsetDateTime from, OffsetDateTime to);

    /** Returns [discountSchemeId, sum(discountAmount), count] per row. */
    @Query("SELECT d.discountSchemeId, SUM(d.discountAmount), COUNT(d) FROM DiscountDecision d WHERE d.createdAt BETWEEN :from AND :to GROUP BY d.discountSchemeId")
    List<Object[]> sumAmountAndCountBySchemeAndCreatedAtBetween(OffsetDateTime from, OffsetDateTime to);

    @Query("SELECT SUM(d.discountAmount) FROM DiscountDecision d WHERE d.discountSchemeId = :schemeId AND d.createdAt BETWEEN :from AND :to")
    BigDecimal sumDiscountAmountByDiscountSchemeIdAndCreatedAtBetween(UUID schemeId, OffsetDateTime from, OffsetDateTime to);

    long countByDiscountSchemeIdAndCreatedAtBetween(UUID schemeId, OffsetDateTime from, OffsetDateTime to);
}
