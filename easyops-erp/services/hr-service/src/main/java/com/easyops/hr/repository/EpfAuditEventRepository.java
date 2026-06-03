package com.easyops.hr.repository;

import com.easyops.hr.entity.EpfAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EpfAuditEventRepository extends JpaRepository<EpfAuditEvent, UUID> {
    List<EpfAuditEvent> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<EpfAuditEvent> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);
}
