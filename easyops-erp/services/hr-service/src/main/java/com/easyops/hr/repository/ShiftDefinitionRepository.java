package com.easyops.hr.repository;

import com.easyops.hr.entity.ShiftDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShiftDefinitionRepository extends JpaRepository<ShiftDefinition, UUID> {

    List<ShiftDefinition> findByOrganizationIdOrderByCodeAsc(UUID organizationId);

    List<ShiftDefinition> findByOrganizationIdAndIsActiveOrderByCodeAsc(UUID organizationId, Boolean isActive);

    Optional<ShiftDefinition> findByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
}
