package com.easyops.hr.repository;

import com.easyops.hr.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GoalRepository extends JpaRepository<Goal, UUID> {
    
    List<Goal> findByOrganizationId(UUID organizationId);
    
    List<Goal> findByEmployeeIdAndOrganizationId(UUID employeeId, UUID organizationId);
    
    List<Goal> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    List<Goal> findByCycleId(UUID cycleId);
    
    List<Goal> findByEmployeeIdAndCycleId(UUID employeeId, UUID cycleId);
    
    @Query("SELECT g FROM Goal g WHERE g.employeeId = :employeeId AND g.status = :status")
    List<Goal> findEmployeeGoalsByStatus(@Param("employeeId") UUID employeeId, 
                                         @Param("status") String status);
}

