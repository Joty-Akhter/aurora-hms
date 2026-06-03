package com.easyops.hr.repository;

import com.easyops.hr.entity.LoanAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoanAuditLogRepository extends JpaRepository<LoanAuditLog, UUID> {

    List<LoanAuditLog> findByOrganizationIdAndEntityTypeAndEntityIdOrderByPerformedAtDesc(
            UUID organizationId, String entityType, UUID entityId);

    /** RE-04: org-scoped loan config (COA replace, bulk AD-03 summary); {@code entity_id} is org id. */
    List<LoanAuditLog> findByOrganizationIdAndEntityTypeInOrderByPerformedAtDesc(
            UUID organizationId, Collection<String> entityTypes);
}
