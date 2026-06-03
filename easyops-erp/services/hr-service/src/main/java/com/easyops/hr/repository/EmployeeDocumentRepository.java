package com.easyops.hr.repository;

import com.easyops.hr.entity.EmployeeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, UUID> {
    
    List<EmployeeDocument> findByEmployeeId(UUID employeeId);
    
    List<EmployeeDocument> findByEmployeeIdAndStatus(UUID employeeId, String status);
    
    List<EmployeeDocument> findByEmployeeIdAndDocumentType(UUID employeeId, String documentType);
    
    List<EmployeeDocument> findByOrganizationId(UUID organizationId);
}

