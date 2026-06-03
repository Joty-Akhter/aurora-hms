package com.easyops.inventory.service;

import com.easyops.inventory.entity.Warehouse;
import com.easyops.inventory.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseService {
    
    private final WarehouseRepository warehouseRepository;
    
    @Transactional(readOnly = true)
    @Cacheable(value = "warehouses", key = "#organizationId")
    public List<Warehouse> getAllWarehouses(UUID organizationId) {
        log.debug("Fetching all warehouses for organization: {}", organizationId);
        return warehouseRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<Warehouse> getActiveWarehouses(UUID organizationId) {
        log.debug("Fetching active warehouses for organization: {}", organizationId);
        return warehouseRepository.findByOrganizationIdAndIsActive(organizationId, true);
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "warehouse", key = "#id")
    public Warehouse getWarehouseById(UUID id) {
        log.debug("Fetching warehouse by ID: {}", id);
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found with ID: " + id));
    }
    
    @Transactional(readOnly = true)
    public Warehouse getWarehouseByCode(UUID organizationId, String code) {
        log.debug("Fetching warehouse by code: {} for organization: {}", code, organizationId);
        return warehouseRepository.findByOrganizationIdAndCode(organizationId, code)
                .orElseThrow(() -> new RuntimeException("Warehouse not found with code: " + code));
    }
    
    @Transactional
    @CacheEvict(value = {"warehouses", "warehouse"}, allEntries = true)
    public Warehouse createWarehouse(Warehouse warehouse) {
        log.info("Creating new warehouse: {}", warehouse.getCode());
        
        if (warehouseRepository.existsByOrganizationIdAndCode(warehouse.getOrganizationId(), warehouse.getCode())) {
            throw new RuntimeException("Warehouse with code already exists: " + warehouse.getCode());
        }
        
        return warehouseRepository.save(warehouse);
    }
    
    @Transactional
    @CacheEvict(value = {"warehouses", "warehouse"}, allEntries = true)
    public Warehouse updateWarehouse(UUID id, Warehouse warehouseDetails) {
        log.info("Updating warehouse: {}", id);
        
        Warehouse warehouse = getWarehouseById(id);
        
        warehouse.setName(warehouseDetails.getName());
        warehouse.setWarehouseType(warehouseDetails.getWarehouseType());
        warehouse.setAddressLine1(warehouseDetails.getAddressLine1());
        warehouse.setCity(warehouseDetails.getCity());
        warehouse.setState(warehouseDetails.getState());
        warehouse.setCountry(warehouseDetails.getCountry());
        warehouse.setPhone(warehouseDetails.getPhone());
        warehouse.setEmail(warehouseDetails.getEmail());
        warehouse.setIsActive(warehouseDetails.getIsActive());
        warehouse.setStatus(warehouseDetails.getStatus());
        warehouse.setUpdatedBy(warehouseDetails.getUpdatedBy());
        
        return warehouseRepository.save(warehouse);
    }
    
    @Transactional
    @CacheEvict(value = {"warehouses", "warehouse"}, allEntries = true)
    public void deleteWarehouse(UUID id) {
        log.info("Deleting warehouse: {}", id);
        warehouseRepository.deleteById(id);
    }
}

