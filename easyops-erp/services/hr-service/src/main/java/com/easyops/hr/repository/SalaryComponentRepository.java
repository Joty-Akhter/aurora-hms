package com.easyops.hr.repository;

import com.easyops.hr.entity.SalaryComponent;
import com.easyops.hr.entity.SalaryComponentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalaryComponentRepository extends JpaRepository<SalaryComponent, UUID> {

    List<SalaryComponent> findByOrganizationId(UUID organizationId);

    /** List by org ordered by display order (SC-05). */
    List<SalaryComponent> findByOrganizationIdOrderByDisplayOrderAsc(UUID organizationId);

    List<SalaryComponent> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);

    List<SalaryComponent> findByOrganizationIdAndComponentType(UUID organizationId, String componentType);

    /** SC-04: Components effective on the given date (active only); ordered by displayOrder for payroll/payslips. */
    @Query("SELECT c FROM SalaryComponent c WHERE c.organizationId = :organizationId AND c.isActive = true"
        + " AND (c.effectiveFrom IS NULL OR c.effectiveFrom <= :effectiveDate)"
        + " AND (c.effectiveTo IS NULL OR c.effectiveTo >= :effectiveDate)"
        + " ORDER BY c.displayOrder ASC NULLS LAST")
    List<SalaryComponent> findByOrganizationIdAndEffectiveDateOrderByDisplayOrderAsc(
        @Param("organizationId") UUID organizationId,
        @Param("effectiveDate") LocalDate effectiveDate);

    /** Components effective on the given date (active only); same as above without ordering. */
    default List<SalaryComponent> findByOrganizationIdAndEffectiveDate(UUID organizationId, LocalDate effectiveDate) {
        return findByOrganizationIdAndEffectiveDateOrderByDisplayOrderAsc(organizationId, effectiveDate);
    }

    /** SC-46: Components effective on the given date (any active state); ordered by displayOrder. For list with includeInactive. */
    @Query("SELECT c FROM SalaryComponent c WHERE c.organizationId = :organizationId"
        + " AND (c.effectiveFrom IS NULL OR c.effectiveFrom <= :effectiveDate)"
        + " AND (c.effectiveTo IS NULL OR c.effectiveTo >= :effectiveDate)"
        + " ORDER BY c.displayOrder ASC NULLS LAST")
    List<SalaryComponent> findByOrganizationIdAndEffectiveDateOrderByDisplayOrderAscIncludeInactive(
        @Param("organizationId") UUID organizationId,
        @Param("effectiveDate") LocalDate effectiveDate);

    /** SC-06: Code unique per organization; used to validate on create and for get-by-code. */
    boolean existsByOrganizationIdAndCode(UUID organizationId, String code);

    Optional<SalaryComponent> findByOrganizationIdAndCode(UUID organizationId, String code);

    /** SC-02, SC-42: List by organization and category. */
    List<SalaryComponent> findByOrganizationIdAndCategory(UUID organizationId, SalaryComponentCategory category);

    /** SC-19, SC-29: Count active earning components with category Basic (at least one required per org). */
    long countByOrganizationIdAndComponentTypeAndCategoryAndIsActive(
        UUID organizationId, String componentType, SalaryComponentCategory category, Boolean isActive);

    /** Components that use the given code as base (for circular ref / dependency checks). */
    List<SalaryComponent> findByOrganizationIdAndBaseComponentCode(UUID organizationId, String baseComponentCode);

    /** SC-22: Components that have the given statutory tag (e.g. PF_WAGE for PF wage base). */
    @Query("SELECT c FROM SalaryComponent c JOIN c.statutoryTags t WHERE c.organizationId = :organizationId AND c.isActive = true AND t = :tag")
    List<SalaryComponent> findByOrganizationIdAndIsActiveTrueAndStatutoryTag(
        @Param("organizationId") UUID organizationId, @Param("tag") String tag);
}

