package com.easyops.hr.repository;

import com.easyops.hr.entity.LoanApplication;
import com.easyops.hr.entity.LoanApplicationStatus;
import com.easyops.hr.entity.LoanCategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, UUID> {

    Optional<LoanApplication> findByApplicationIdAndOrganizationId(UUID applicationId, UUID organizationId);

    @Query("""
            SELECT a FROM LoanApplication a
            WHERE a.organizationId = :orgId
            AND a.employeeId = :empId
            AND a.status IN :statuses
            AND (:excludeId IS NULL OR a.applicationId <> :excludeId)
            """)
    List<LoanApplication> findBlockingApplications(
            @Param("orgId") UUID orgId,
            @Param("empId") UUID empId,
            @Param("statuses") List<LoanApplicationStatus> statuses,
            @Param("excludeId") UUID excludeId);

    @Query("""
            SELECT a FROM LoanApplication a INNER JOIN LoanCategory c ON a.categoryId = c.categoryId
            WHERE a.organizationId = :orgId
            AND c.organizationId = :orgId
            AND (:employeeId IS NULL OR a.employeeId = :employeeId)
            AND (:status IS NULL OR a.status = :status)
            AND (:categoryType IS NULL OR c.categoryType = :categoryType)
            ORDER BY a.submittedAt DESC NULLS LAST, a.createdAt DESC
            """)
    List<LoanApplication> search(
            @Param("orgId") UUID orgId,
            @Param("employeeId") UUID employeeId,
            @Param("status") LoanApplicationStatus status,
            @Param("categoryType") LoanCategoryType categoryType);
}
