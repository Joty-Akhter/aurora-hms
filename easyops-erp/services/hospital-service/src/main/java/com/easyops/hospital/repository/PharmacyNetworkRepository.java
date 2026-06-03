package com.easyops.hospital.repository;

import com.easyops.hospital.entity.PharmacyNetwork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PharmacyNetworkRepository extends JpaRepository<PharmacyNetwork, UUID> {
    
    /**
     * Find all active networks
     */
    List<PharmacyNetwork> findByIsActiveTrueOrderByNetworkName();
    
    /**
     * Find default network
     */
    Optional<PharmacyNetwork> findByIsDefaultTrueAndIsActiveTrue();
    
    /**
     * Find networks by organization
     */
    List<PharmacyNetwork> findByOrganizationIdAndIsActiveTrueOrderByNetworkName(UUID organizationId);
    
    /**
     * Find network by name
     */
    Optional<PharmacyNetwork> findByNetworkNameAndIsActiveTrue(String networkName);

    /**
     * Convenience finder used by services – delegates to active-only lookup.
     */
    default Optional<PharmacyNetwork> findByNetworkName(String networkName) {
        return findByNetworkNameAndIsActiveTrue(networkName);
    }
    
    /**
     * Find networks by type
     */
    List<PharmacyNetwork> findByNetworkTypeAndIsActiveTrue(PharmacyNetwork.NetworkType networkType);
    
    /**
     * Find networks that support prescription transmission
     */
    @Query("SELECT n FROM PharmacyNetwork n WHERE n.isActive = true " +
           "AND n.supportsPrescriptionTransmission = true " +
           "ORDER BY n.isDefault DESC, n.networkName")
    List<PharmacyNetwork> findNetworksSupportingPrescriptionTransmission();
    
    /**
     * Find networks that support fill status updates
     */
    @Query("SELECT n FROM PharmacyNetwork n WHERE n.isActive = true " +
           "AND n.supportsFillStatusUpdates = true " +
           "ORDER BY n.isDefault DESC, n.networkName")
    List<PharmacyNetwork> findNetworksSupportingFillStatusUpdates();
}
