package com.easyops.inventory.repository;

import com.easyops.inventory.entity.SerialNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SerialNumberRepository extends JpaRepository<SerialNumber, UUID> {
    
    List<SerialNumber> findByOrganizationId(UUID organizationId);
    
    List<SerialNumber> findByOrganizationIdAndProductId(UUID organizationId, UUID productId);
    
    List<SerialNumber> findByOrganizationIdAndStatus(UUID organizationId, String status);
    
    Optional<SerialNumber> findByOrganizationIdAndProductIdAndSerialNumber(UUID organizationId, UUID productId, String serialNumber);
    
    @Query("SELECT s FROM SerialNumber s WHERE s.organizationId = :orgId AND s.warehouseId = :warehouseId AND s.status = 'IN_STOCK'")
    List<SerialNumber> findAvailableSerials(@Param("orgId") UUID organizationId, @Param("warehouseId") UUID warehouseId);
    
    @Query("SELECT s FROM SerialNumber s WHERE s.organizationId = :orgId AND s.customerId = :customerId")
    List<SerialNumber> findByCustomer(@Param("orgId") UUID organizationId, @Param("customerId") UUID customerId);
    
    boolean existsByOrganizationIdAndProductIdAndSerialNumber(UUID organizationId, UUID productId, String serialNumber);
}

