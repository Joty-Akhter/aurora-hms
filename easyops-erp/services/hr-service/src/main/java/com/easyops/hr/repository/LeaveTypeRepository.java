package com.easyops.hr.repository;

import com.easyops.hr.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, UUID> {
    
    List<LeaveType> findByOrganizationId(UUID organizationId);
    
    List<LeaveType> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    
    Optional<LeaveType> findByOrganizationIdAndTypeName(UUID organizationId, String typeName);
}

