package com.easyops.hr.repository;

import com.easyops.hr.entity.Reimbursement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReimbursementRepository extends JpaRepository<Reimbursement, UUID> {
    
    List<Reimbursement> findByOrganizationId(UUID organizationId);
    
    List<Reimbursement> findByEmployeeIdAndOrganizationId(UUID employeeId, UUID organizationId);
    
    List<Reimbursement> findByOrganizationIdAndStatus(UUID organizationId, String status);
}

