package com.easyops.monitoring.repository;

import com.easyops.monitoring.entity.Metric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MetricRepository extends JpaRepository<Metric, UUID> {
    
    List<Metric> findByServiceNameAndRecordedAtAfter(String serviceName, LocalDateTime after);
    
    @Query("SELECT m FROM Metric m WHERE m.serviceName = :serviceName AND m.metricName = :metricName AND m.recordedAt >= :startTime ORDER BY m.recordedAt DESC")
    List<Metric> findMetricsByServiceAndName(
        @Param("serviceName") String serviceName, 
        @Param("metricName") String metricName, 
        @Param("startTime") LocalDateTime startTime
    );
    
    void deleteByRecordedAtBefore(LocalDateTime cutoffDate);
}

