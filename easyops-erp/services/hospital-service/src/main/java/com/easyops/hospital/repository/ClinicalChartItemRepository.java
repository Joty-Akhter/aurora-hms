package com.easyops.hospital.repository;

import com.easyops.hospital.entity.ClinicalChartItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicalChartItemRepository extends JpaRepository<ClinicalChartItem, UUID> {

    @Query("""
           SELECT c FROM ClinicalChartItem c
           WHERE c.statusLegacy = 1
             AND (
                   FALSE = :investigationsOnly
                   OR (
                         c.subDeptName IS NOT NULL
                         AND LOWER(TRIM(c.subDeptName)) IN ('diagnostic', 'radiology', 'labtest')
                   )
             )
             AND (
                   :containsPattern IS NULL
                   OR LOWER(c.description) LIKE :containsPattern ESCAPE '!'
                   OR LOWER(COALESCE(c.pcode, '')) LIKE :containsPattern ESCAPE '!'
                   OR LOWER(COALESCE(c.deptName, '')) LIKE :containsPattern ESCAPE '!'
                   OR LOWER(COALESCE(c.subDeptName, '')) LIKE :containsPattern ESCAPE '!'
                   OR LOWER(COALESCE(c.subSubDeptName, '')) LIKE :containsPattern ESCAPE '!'
                   OR LOWER(COALESCE(c.reportGroupName, '')) LIKE :containsPattern ESCAPE '!'
             )
           ORDER BY c.deptName ASC NULLS LAST, c.description ASC
           """)
    Page<ClinicalChartItem> searchCatalog(
            @Param("containsPattern") String containsPattern,
            @Param("investigationsOnly") boolean investigationsOnly,
            Pageable pageable);

    /**
     * Investigations/tests only: legacy SubDeptName is Diagnostic, Radiology, or LabTest (case-insensitive).
     */
    @Query("""
           SELECT c FROM ClinicalChartItem c
           WHERE c.statusLegacy = 1
             AND c.subDeptName IS NOT NULL
             AND LOWER(TRIM(c.subDeptName)) IN ('diagnostic', 'radiology', 'labtest')
             AND (
                   :containsPattern IS NULL
                   OR LOWER(c.description) LIKE :containsPattern ESCAPE '!'
                   OR LOWER(COALESCE(c.pcode, '')) LIKE :containsPattern ESCAPE '!'
                   OR LOWER(COALESCE(c.deptName, '')) LIKE :containsPattern ESCAPE '!'
                   OR LOWER(COALESCE(c.subDeptName, '')) LIKE :containsPattern ESCAPE '!'
                   OR LOWER(COALESCE(c.subSubDeptName, '')) LIKE :containsPattern ESCAPE '!'
                   OR LOWER(COALESCE(c.reportGroupName, '')) LIKE :containsPattern ESCAPE '!'
             )
           ORDER BY c.description ASC
           """)
    Page<ClinicalChartItem> searchInvestigations(@Param("containsPattern") String containsPattern, Pageable pageable);

    Optional<ClinicalChartItem> findTopByOrderByLegacyRowIdDesc();
}
