package com.easyops.inventory.repository;

import com.easyops.inventory.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {
    
    List<Warehouse> findByOrganizationId(UUID organizationId);
    
    List<Warehouse> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    
    Optional<Warehouse> findByOrganizationIdAndCode(UUID organizationId, String code);
    
    List<Warehouse> findByOrganizationIdAndWarehouseType(UUID organizationId, String warehouseType);
    
    boolean existsByOrganizationIdAndCode(UUID organizationId, String code);
}

