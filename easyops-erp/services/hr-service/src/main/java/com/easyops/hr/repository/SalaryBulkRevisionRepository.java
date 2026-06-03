package com.easyops.hr.repository;

import com.easyops.hr.entity.SalaryBulkRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SalaryBulkRevisionRepository extends JpaRepository<SalaryBulkRevision, UUID> {

    List<SalaryBulkRevision> findByOrganizationIdOrderByRequestedAtDesc(UUID organizationId);

    List<SalaryBulkRevision> findByOrganizationIdAndStatusOrderByRequestedAtDesc(UUID organizationId, String status);
}
