package com.easyops.organization.repository;

import com.easyops.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Organization Repository
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID>, JpaSpecificationExecutor<Organization> {

    Optional<Organization> findByCode(String code);

    boolean existsByCode(String code);

    List<Organization> findByStatus(String status);

    List<Organization> findBySubscriptionStatus(String subscriptionStatus);

    List<Organization> findByIsActive(Boolean isActive);
}

