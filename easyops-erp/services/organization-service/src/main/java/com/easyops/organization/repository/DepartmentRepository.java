package com.easyops.organization.repository;

import com.easyops.organization.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Department Repository
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    List<Department> findByOrganizationId(UUID organizationId);

    List<Department> findByOrganizationIdAndStatus(UUID organizationId, String status);

    Optional<Department> findByOrganizationIdAndCode(UUID organizationId, String code);

    List<Department> findByParentDepartmentId(UUID parentDepartmentId);

    List<Department> findByOrganizationIdAndParentDepartmentIdIsNull(UUID organizationId);

    @Query("SELECT d FROM Department d WHERE d.organizationId = :orgId AND d.managerId = :managerId")
    List<Department> findByOrganizationIdAndManagerId(UUID orgId, UUID managerId);

    boolean existsByOrganizationIdAndCode(UUID organizationId, String code);
}

