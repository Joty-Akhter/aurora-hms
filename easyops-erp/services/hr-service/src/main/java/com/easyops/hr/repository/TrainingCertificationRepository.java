package com.easyops.hr.repository;

import com.easyops.hr.entity.TrainingCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TrainingCertificationRepository extends JpaRepository<TrainingCertification, UUID> {
    
    List<TrainingCertification> findByOrganizationId(UUID organizationId);
    
    List<TrainingCertification> findByEmployeeIdAndOrganizationId(UUID employeeId, UUID organizationId);
    
    List<TrainingCertification> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    List<TrainingCertification> findByEmployeeIdAndStatus(UUID employeeId, String status);
}

