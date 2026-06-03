package com.easyops.ar.repository;

import com.easyops.ar.entity.ReminderConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReminderConfigRepository extends JpaRepository<ReminderConfig, UUID> {
    
    Optional<ReminderConfig> findByOrganizationId(UUID organizationId);
}

