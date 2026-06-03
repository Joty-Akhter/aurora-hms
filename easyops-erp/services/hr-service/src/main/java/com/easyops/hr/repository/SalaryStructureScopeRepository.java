package com.easyops.hr.repository;

import com.easyops.hr.entity.SalaryStructureScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalaryStructureScopeRepository extends JpaRepository<SalaryStructureScope, UUID> {

    List<SalaryStructureScope> findByOrganizationId(UUID organizationId);

    Optional<SalaryStructureScope> findByOrganizationIdAndDepartmentIdAndLocationId(
            UUID organizationId, UUID departmentId, UUID locationId);
}
