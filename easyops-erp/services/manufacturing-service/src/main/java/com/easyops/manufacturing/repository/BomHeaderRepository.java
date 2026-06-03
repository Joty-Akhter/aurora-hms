package com.easyops.manufacturing.repository;

import com.easyops.manufacturing.entity.BomHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BomHeaderRepository extends JpaRepository<BomHeader, UUID> {

    List<BomHeader> findByOrganizationId(UUID organizationId);

    List<BomHeader> findByOrganizationIdAndStatus(UUID organizationId, String status);

    Optional<BomHeader> findByOrganizationIdAndBomNumber(UUID organizationId, String bomNumber);

    List<BomHeader> findByOrganizationIdAndProductId(UUID organizationId, UUID productId);

    Optional<BomHeader> findByOrganizationIdAndProductIdAndVersionNumber(
            UUID organizationId, UUID productId, Integer versionNumber);

    List<BomHeader> findByOrganizationIdAndBomType(UUID organizationId, String bomType);

    @Query("SELECT b FROM BomHeader b WHERE b.organizationId = :orgId AND b.status = 'ACTIVE' " +
           "AND (b.effectiveTo IS NULL OR b.effectiveTo >= CURRENT_DATE)")
    List<BomHeader> findActiveBoms(@Param("orgId") UUID organizationId);

    @Query("SELECT b FROM BomHeader b WHERE b.productId = :productId AND b.status = 'ACTIVE' " +
           "ORDER BY b.versionNumber DESC")
    Optional<BomHeader> findLatestActiveBomForProduct(@Param("productId") UUID productId);

    @Query("SELECT COUNT(b) FROM BomHeader b WHERE b.organizationId = :orgId")
    long countByOrganization(@Param("orgId") UUID organizationId);

    @Query("SELECT COUNT(b) FROM BomHeader b WHERE b.organizationId = :orgId AND b.status = :status")
    long countByOrganizationAndStatus(@Param("orgId") UUID organizationId, @Param("status") String status);
}

