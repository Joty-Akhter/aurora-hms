package com.easyops.hr.repository;

import com.easyops.hr.entity.SalaryBand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SalaryBandRepository extends JpaRepository<SalaryBand, UUID> {

    /** List bands by grade, ordered by display order (SS-17). */
    List<SalaryBand> findBySalaryGradeIdOrderByDisplayOrderAsc(UUID salaryGradeId);

    /** Bands effective on a given date (for payroll/summary). */
    @Query("""
        SELECT b FROM SalaryBand b
        WHERE b.salaryGradeId = :gradeId
        AND (:effectiveDate IS NULL OR (b.effectiveFrom IS NULL OR b.effectiveFrom <= :effectiveDate)
            AND (b.effectiveTo IS NULL OR b.effectiveTo >= :effectiveDate))
        ORDER BY b.displayOrder ASC
        """)
    List<SalaryBand> findBySalaryGradeIdAndEffectiveDate(
        @Param("gradeId") UUID gradeId,
        @Param("effectiveDate") LocalDate effectiveDate
    );
}
