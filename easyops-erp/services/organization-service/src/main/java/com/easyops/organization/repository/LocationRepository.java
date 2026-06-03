package com.easyops.organization.repository;

import com.easyops.organization.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Location Repository
 */
@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {

    List<Location> findByOrganizationId(UUID organizationId);

    List<Location> findByOrganizationIdAndStatus(UUID organizationId, String status);

    Optional<Location> findByOrganizationIdAndCode(UUID organizationId, String code);

    List<Location> findByOrganizationIdAndType(UUID organizationId, String type);

    @Query("SELECT l FROM Location l WHERE l.organizationId = :orgId AND l.managerId = :managerId")
    List<Location> findByOrganizationIdAndManagerId(UUID orgId, UUID managerId);

    boolean existsByOrganizationIdAndCode(UUID organizationId, String code);
}

