package com.easyops.hr.repository;

import com.easyops.hr.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PositionRepository extends JpaRepository<Position, UUID> {
    
    List<Position> findByOrganizationId(UUID organizationId);
    
    List<Position> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    
    List<Position> findByOrganizationIdAndDepartmentId(UUID organizationId, UUID departmentId);
    
    Optional<Position> findByOrganizationIdAndTitle(UUID organizationId, String title);

    /** SS-22: Check if any position uses this grade as default (prevent delete if true). */
    boolean existsByDefaultSalaryGradeId(UUID defaultSalaryGradeId);

    /** SS-22: Check if any position uses this band as default (prevent delete if true). */
    boolean existsByDefaultSalaryBandId(UUID defaultSalaryBandId);
}

