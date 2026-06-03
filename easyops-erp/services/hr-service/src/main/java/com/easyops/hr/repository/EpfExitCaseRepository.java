package com.easyops.hr.repository;

import com.easyops.hr.entity.EpfExitCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EpfExitCaseRepository extends JpaRepository<EpfExitCase, UUID> {
    List<EpfExitCase> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<EpfExitCase> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);
}
