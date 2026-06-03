package com.easyops.hr.repository;

import com.easyops.hr.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {
    
    List<LeaveBalance> findByOrganizationId(UUID organizationId);
    
    List<LeaveBalance> findByEmployeeIdAndOrganizationId(UUID employeeId, UUID organizationId);
    
    List<LeaveBalance> findByEmployeeIdAndYear(UUID employeeId, Integer year);
    
    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeIdAndYear(
            UUID employeeId, UUID leaveTypeId, Integer year);
}

