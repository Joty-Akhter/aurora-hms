package com.easyops.hr.repository;

import com.easyops.hr.entity.SalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, UUID> {

    List<SalaryStructure> findByOrganizationId(UUID organizationId);

    List<SalaryStructure> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);

    /** Structures effective on the given date (effectiveFrom <= date and (effectiveTo is null or effectiveTo >= date)). Active only. */
    @Query("""
        SELECT s FROM SalaryStructure s
        WHERE s.organizationId = :organizationId
        AND s.effectiveFrom <= :effectiveDate
        AND (s.effectiveTo IS NULL OR s.effectiveTo >= :effectiveDate)
        AND (s.isActive = true)
        ORDER BY s.effectiveFrom DESC
        """)
    List<SalaryStructure> findByOrganizationIdAndEffectiveDate(
        @Param("organizationId") UUID organizationId,
        @Param("effectiveDate") LocalDate effectiveDate
    );

    /** For get-by-code (one of possibly many versions). */
    Optional<SalaryStructure> findByOrganizationIdAndCode(UUID organizationId, String code);

    /**
     * Finds structures with the same organization and code whose effective period overlaps
     * [effectiveFrom, effectiveTo]. Null effectiveTo is treated as open-ended.
     * Exclude a structure id when checking for update (pass null for create).
     */
    @Query("""
        SELECT s FROM SalaryStructure s
        WHERE s.organizationId = :organizationId AND s.code = :code
        AND (:excludeId IS NULL OR s.salaryStructureId <> :excludeId)
        AND (s.effectiveTo IS NULL OR s.effectiveTo >= :effectiveFrom)
        AND (:effectiveTo IS NULL OR s.effectiveFrom <= :effectiveTo)
        """)
    List<SalaryStructure> findOverlappingByOrganizationAndCode(
        @Param("organizationId") UUID organizationId,
        @Param("code") String code,
        @Param("effectiveFrom") LocalDate effectiveFrom,
        @Param("effectiveTo") LocalDate effectiveTo,
        @Param("excludeId") UUID excludeId
    );

    /**
     * SS-41, SS-45: List structures by org with optional effective date and active filter.
     * When effectiveDate is set: structure is effective on that date (effectiveFrom <= date and (effectiveTo is null or effectiveTo >= date)).
     * When includeInactive is false: only isActive = true.
     */
    @Query("""
        SELECT s FROM SalaryStructure s
        WHERE s.organizationId = :organizationId
        AND (:effectiveDate IS NULL OR (s.effectiveFrom <= :effectiveDate AND (s.effectiveTo IS NULL OR s.effectiveTo >= :effectiveDate)))
        AND (:includeInactive = true OR s.isActive = true)
        ORDER BY s.effectiveFrom DESC
        """)
    List<SalaryStructure> findByOrganizationIdWithFilters(
        @Param("organizationId") UUID organizationId,
        @Param("effectiveDate") LocalDate effectiveDate,
        @Param("includeInactive") boolean includeInactive
    );
}

