package com.easyops.hr.repository;

import com.easyops.hr.entity.OneOnOneMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OneOnOneMeetingRepository extends JpaRepository<OneOnOneMeeting, UUID> {
    
    List<OneOnOneMeeting> findByOrganizationId(UUID organizationId);
    
    List<OneOnOneMeeting> findByEmployeeIdAndOrganizationId(UUID employeeId, UUID organizationId);
    
    List<OneOnOneMeeting> findByManagerId(UUID managerId);
    
    List<OneOnOneMeeting> findByEmployeeIdAndManagerId(UUID employeeId, UUID managerId);
    
    List<OneOnOneMeeting> findByOrganizationIdAndStatus(UUID organizationId, String status);
}

