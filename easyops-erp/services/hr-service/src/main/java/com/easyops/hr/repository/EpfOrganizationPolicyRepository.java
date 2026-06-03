package com.easyops.hr.repository;

import com.easyops.hr.entity.EpfOrganizationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EpfOrganizationPolicyRepository extends JpaRepository<EpfOrganizationPolicy, UUID> {

    Optional<EpfOrganizationPolicy> findByOrganizationId(UUID organizationId);
}
