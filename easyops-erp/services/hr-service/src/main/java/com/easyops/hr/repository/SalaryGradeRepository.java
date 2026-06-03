package com.easyops.hr.repository;

import com.easyops.hr.entity.SalaryGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalaryGradeRepository extends JpaRepository<SalaryGrade, UUID> {

    /** List grades by structure, ordered by display order (SS-10). */
    List<SalaryGrade> findBySalaryStructureIdOrderByDisplayOrderAsc(UUID salaryStructureId);

    /** Uniqueness check and get-by-code within structure (SS-09, SS-12). */
    Optional<SalaryGrade> findBySalaryStructureIdAndCode(UUID salaryStructureId, String code);

    boolean existsBySalaryStructureIdAndCode(UUID salaryStructureId, String code);

    /** SS-45: Grades effective on a given date (for payroll). Null effectiveTo = open-ended. */
    @Query("""
        SELECT g FROM SalaryGrade g
        WHERE g.salaryStructureId = :structureId
        AND (:effectiveDate IS NULL OR (g.effectiveFrom IS NULL OR g.effectiveFrom <= :effectiveDate)
            AND (g.effectiveTo IS NULL OR g.effectiveTo >= :effectiveDate))
        ORDER BY g.displayOrder ASC
        """)
    List<SalaryGrade> findBySalaryStructureIdAndEffectiveDate(
        @Param("structureId") UUID structureId,
        @Param("effectiveDate") LocalDate effectiveDate
    );
}
