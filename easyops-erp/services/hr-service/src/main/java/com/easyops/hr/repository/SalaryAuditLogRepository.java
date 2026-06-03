package com.easyops.hr.repository;

import com.easyops.hr.entity.SalaryAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SalaryAuditLogRepository extends JpaRepository<SalaryAuditLog, UUID> {

    List<SalaryAuditLog> findByOrganizationIdAndEntityTypeAndEntityIdOrderByPerformedAtDesc(
        UUID organizationId, String entityType, UUID entityId);
}
