package com.easyops.hr.repository;

import com.easyops.hr.entity.PerformanceReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, UUID> {
    
    List<PerformanceReview> findByOrganizationId(UUID organizationId);
    
    List<PerformanceReview> findByEmployeeIdAndOrganizationId(UUID employeeId, UUID organizationId);
    
    List<PerformanceReview> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    List<PerformanceReview> findByCycleId(UUID cycleId);
    
    List<PerformanceReview> findByReviewerId(UUID reviewerId);
    
    @Query("SELECT pr FROM PerformanceReview pr WHERE pr.employeeId = :employeeId " +
           "ORDER BY pr.reviewDate DESC")
    List<PerformanceReview> findEmployeeReviewsOrderByDateDesc(@Param("employeeId") UUID employeeId);
}

