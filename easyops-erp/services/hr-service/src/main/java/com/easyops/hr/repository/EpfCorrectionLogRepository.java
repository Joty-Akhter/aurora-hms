package com.easyops.hr.repository;

import com.easyops.hr.entity.EpfCorrectionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EpfCorrectionLogRepository extends JpaRepository<EpfCorrectionLog, UUID> {
    List<EpfCorrectionLog> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<EpfCorrectionLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);
}
