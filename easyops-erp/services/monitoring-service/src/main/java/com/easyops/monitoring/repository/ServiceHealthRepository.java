package com.easyops.monitoring.repository;

import com.easyops.monitoring.entity.ServiceHealth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceHealthRepository extends JpaRepository<ServiceHealth, UUID> {
    
    Optional<ServiceHealth> findByServiceName(String serviceName);
    
    @Query("SELECT COUNT(s) FROM ServiceHealth s WHERE s.status = 'UP'")
    Long countServicesUp();
    
    @Query("SELECT COUNT(s) FROM ServiceHealth s WHERE s.status = 'DOWN'")
    Long countServicesDown();
}

