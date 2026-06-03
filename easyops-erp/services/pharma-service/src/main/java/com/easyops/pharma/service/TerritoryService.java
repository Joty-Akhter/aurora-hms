package com.easyops.pharma.service;

import com.easyops.pharma.client.InventoryClient;
import com.easyops.pharma.entity.*;
import com.easyops.pharma.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TerritoryService {
    
    private final DivisionRepository divisionRepository;
    private final RegionRepository regionRepository;
    private final TerritoryRepository territoryRepository;
    private final AreaRepository areaRepository;
    private final InventoryClient inventoryClient;
    
    // Division Operations
    @Transactional(readOnly = true)
    @Cacheable(value = "divisions", key = "#organizationId")
    public List<Division> getAllDivisions(UUID organizationId) {
        log.debug("Fetching all divisions for organization: {}", organizationId);
        return divisionRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<Division> getActiveDivisions(UUID organizationId) {
        log.debug("Fetching active divisions for organization: {}", organizationId);
        return divisionRepository.findByOrganizationIdAndIsActive(organizationId, true);
    }
    
    @Transactional(readOnly = true)
    public Division getDivisionById(UUID id) {
        log.debug("Fetching division by ID: {}", id);
        return divisionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Division not found with ID: " + id));
    }
    
    @Transactional
    @CacheEvict(value = "divisions", allEntries = true)
    public Division createDivision(Division division) {
        log.info("Creating new division: {}", division.getName());
        
        if (division.getCode() != null && divisionRepository.existsByOrganizationIdAndCode(division.getOrganizationId(), division.getCode())) {
            throw new RuntimeException("Division with code already exists: " + division.getCode());
        }
        
        if (divisionRepository.existsByOrganizationIdAndName(division.getOrganizationId(), division.getName())) {
            throw new RuntimeException("Division with name already exists: " + division.getName());
        }
        
        return divisionRepository.save(division);
    }
    
    @Transactional
    @CacheEvict(value = "divisions", allEntries = true)
    public Division updateDivision(UUID id, Division division) {
        log.info("Updating division: {}", id);
        Division existing = getDivisionById(id);
        
        if (division.getCode() != null && !division.getCode().equals(existing.getCode())) {
            if (divisionRepository.existsByOrganizationIdAndCode(division.getOrganizationId(), division.getCode())) {
                throw new RuntimeException("Division with code already exists: " + division.getCode());
            }
        }
        
        if (!division.getName().equals(existing.getName())) {
            if (divisionRepository.existsByOrganizationIdAndName(division.getOrganizationId(), division.getName())) {
                throw new RuntimeException("Division with name already exists: " + division.getName());
            }
        }
        
        existing.setName(division.getName());
        existing.setCode(division.getCode());
        existing.setDescription(division.getDescription());
        existing.setStatus(division.getStatus());
        existing.setIsActive(division.getIsActive());
        existing.setUpdatedBy(division.getUpdatedBy());
        
        return divisionRepository.save(existing);
    }
    
    @Transactional
    @CacheEvict(value = "divisions", allEntries = true)
    public void deleteDivision(UUID id) {
        log.info("Deleting division: {}", id);
        Division division = getDivisionById(id);
        
        // Check if division has regions
        if (!regionRepository.findByDivisionId(division.getId()).isEmpty()) {
            throw new RuntimeException("Cannot delete division with existing regions");
        }
        
        divisionRepository.delete(division);
    }
    
    // Region Operations
    @Transactional(readOnly = true)
    public List<Region> getRegionsByDivision(UUID divisionId) {
        log.debug("Fetching regions for division: {}", divisionId);
        return regionRepository.findByDivisionId(divisionId);
    }
    
    @Transactional(readOnly = true)
    public Region getRegionById(UUID id) {
        log.debug("Fetching region by ID: {}", id);
        return regionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Region not found with ID: " + id));
    }
    
    @Transactional
    @CacheEvict(value = {"divisions", "regions"}, allEntries = true)
    public Region createRegion(Region region) {
        log.info("Creating new region: {}", region.getName());
        
        // Validate division exists
        getDivisionById(region.getDivisionId());
        
        if (regionRepository.existsByDivisionIdAndName(region.getDivisionId(), region.getName())) {
            throw new RuntimeException("Region with name already exists in division: " + region.getName());
        }
        
        return regionRepository.save(region);
    }
    
    @Transactional
    @CacheEvict(value = {"divisions", "regions"}, allEntries = true)
    public Region updateRegion(UUID id, Region region) {
        log.info("Updating region: {}", id);
        Region existing = getRegionById(id);
        
        if (!region.getName().equals(existing.getName())) {
            if (regionRepository.existsByDivisionIdAndName(region.getDivisionId(), region.getName())) {
                throw new RuntimeException("Region with name already exists in division: " + region.getName());
            }
        }
        
        existing.setName(region.getName());
        existing.setCode(region.getCode());
        existing.setDescription(region.getDescription());
        existing.setStatus(region.getStatus());
        existing.setIsActive(region.getIsActive());
        existing.setUpdatedBy(region.getUpdatedBy());
        
        return regionRepository.save(existing);
    }
    
    @Transactional
    @CacheEvict(value = {"divisions", "regions"}, allEntries = true)
    public void deleteRegion(UUID id) {
        log.info("Deleting region: {}", id);
        Region region = getRegionById(id);
        
        // Check if region has areas (territories are under areas)
        if (!areaRepository.findByRegionId(region.getId()).isEmpty()) {
            throw new RuntimeException("Cannot delete region with existing areas");
        }
        
        regionRepository.delete(region);
    }
    
    // Territory Operations
    @Transactional(readOnly = true)
    public List<Territory> getTerritoriesByRegion(UUID regionId) {
        log.debug("Fetching territories for region: {}", regionId);
        return territoryRepository.findByRegionId(regionId);
    }
    
    @Transactional(readOnly = true)
    public List<Territory> getTerritoriesByArea(UUID areaId) {
        log.debug("Fetching territories for area: {}", areaId);
        return territoryRepository.findByAreaId(areaId);
    }
    
    @Transactional(readOnly = true)
    public Territory getTerritoryById(UUID id) {
        log.debug("Fetching territory by ID: {}", id);
        return territoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Territory not found with ID: " + id));
    }
    
    @Transactional
    @CacheEvict(value = {"divisions", "regions", "territories"}, allEntries = true)
    public Territory createTerritory(Territory territory) {
        log.info("Creating new territory: {}", territory.getName());
        
        // Validate area exists
        Area area = getAreaById(territory.getAreaId());
        getRegionById(territory.getRegionId());
        
        if (territoryRepository.existsByAreaIdAndName(territory.getAreaId(), territory.getName())) {
            throw new RuntimeException("Territory with name already exists in area: " + territory.getName());
        }
        
        // Auto-create warehouse for territory (Phase 3: Warehouse per territory)
        try {
            String warehouseCode = territory.getCode() != null && !territory.getCode().isEmpty()
                    ? territory.getCode()
                    : territory.getName().replaceAll("[^A-Za-z0-9]", "-").toUpperCase();
            String warehouseName = territory.getName() + "-Warehouse";
            
            Map<String, Object> warehouseData = new HashMap<>();
            warehouseData.put("organizationId", territory.getOrganizationId());
            warehouseData.put("code", warehouseCode);
            warehouseData.put("name", warehouseName);
            warehouseData.put("warehouseType", "DISTRIBUTION");
            warehouseData.put("description", "Auto-created warehouse for territory: " + territory.getName());
            warehouseData.put("isActive", territory.getIsActive() != null ? territory.getIsActive() : true);
            warehouseData.put("status", "OPERATIONAL");
            if (territory.getCreatedBy() != null) {
                warehouseData.put("createdBy", territory.getCreatedBy());
            }
            
            Map<String, Object> createdWarehouse = inventoryClient.createWarehouse(warehouseData);
            UUID warehouseId = UUID.fromString(createdWarehouse.get("id").toString());
            territory.setWarehouseId(warehouseId);
            
            log.info("Successfully created warehouse {} for territory: {}", warehouseId, territory.getName());
        } catch (Exception e) {
            log.error("Failed to create warehouse for territory: {}", territory.getName(), e);
            throw new RuntimeException("Failed to create warehouse for territory: " + e.getMessage(), e);
        }
        
        return territoryRepository.save(territory);
    }
    
    @Transactional
    @CacheEvict(value = {"divisions", "regions", "territories"}, allEntries = true)
    public Territory updateTerritory(UUID id, Territory territory) {
        log.info("Updating territory: {}", id);
        Territory existing = getTerritoryById(id);
        
        if (!territory.getName().equals(existing.getName())) {
            if (territoryRepository.existsByAreaIdAndName(territory.getAreaId(), territory.getName())) {
                throw new RuntimeException("Territory with name already exists in area: " + territory.getName());
            }
        }
        
        existing.setName(territory.getName());
        existing.setCode(territory.getCode());
        existing.setDescription(territory.getDescription());
        existing.setStatus(territory.getStatus());
        existing.setIsActive(territory.getIsActive());
        existing.setUpdatedBy(territory.getUpdatedBy());
        
        return territoryRepository.save(existing);
    }
    
    @Transactional
    @CacheEvict(value = {"divisions", "regions", "territories"}, allEntries = true)
    public void deleteTerritory(UUID id) {
        log.info("Deleting territory: {}", id);
        Territory territory = getTerritoryById(id);
        
        // Soft-deactivate warehouse when territory is deleted (Phase 3: warehouse retention)
        if (territory.getWarehouseId() != null) {
            try {
                Map<String, Object> existing = inventoryClient.getWarehouse(territory.getWarehouseId());
                Map<String, Object> warehouseUpdate = new HashMap<>(existing);
                warehouseUpdate.put("isActive", false);
                warehouseUpdate.put("status", "INACTIVE");
                inventoryClient.updateWarehouse(territory.getWarehouseId(), warehouseUpdate);
                log.info("Soft-deactivated warehouse {} for deleted territory: {}", territory.getWarehouseId(), territory.getName());
            } catch (Exception e) {
                log.warn("Could not deactivate warehouse {} for territory: {}", territory.getWarehouseId(), territory.getName(), e);
            }
        }
        
        territoryRepository.delete(territory);
    }
    
    // Area Operations
    @Transactional(readOnly = true)
    public List<Area> getAreasByTerritory(UUID territoryId) {
        log.debug("Fetching area for territory: {}", territoryId);
        Territory territory = getTerritoryById(territoryId);
        return territory.getAreaId() != null
                ? List.of(getAreaById(territory.getAreaId()))
                : List.of();
    }
    
    @Transactional(readOnly = true)
    public List<Area> getAreasByRegion(UUID regionId) {
        log.debug("Fetching areas for region: {}", regionId);
        return areaRepository.findByRegionId(regionId);
    }
    
    @Transactional(readOnly = true)
    public List<Area> getAllActiveAreas(UUID organizationId) {
        log.debug("Fetching all active areas for organization: {}", organizationId);
        return areaRepository.findAllActiveAreasByOrganization(organizationId);
    }
    
    @Transactional(readOnly = true)
    public List<Area> getAllAreasByOrganization(UUID organizationId) {
        log.debug("Fetching all areas (including inactive) for organization: {}", organizationId);
        return areaRepository.findByOrganizationId(organizationId);
    }
    
    @Transactional(readOnly = true)
    public Area getAreaById(UUID id) {
        log.debug("Fetching area by ID: {}", id);
        return areaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Area not found with ID: " + id));
    }
    
    @Transactional
    @CacheEvict(value = {"divisions", "regions", "territories", "areas"}, allEntries = true)
    public Area createArea(Area area) {
        log.info("Creating new area: {}", area.getName());
        
        // Validate region exists (Phase 3: Areas are under regions, no warehouse)
        getRegionById(area.getRegionId());
        
        if (areaRepository.existsByRegionIdAndName(area.getRegionId(), area.getName())) {
            throw new RuntimeException("Area with name already exists in region: " + area.getName());
        }
        
        return areaRepository.save(area);
    }
    
    @Transactional
    @CacheEvict(value = {"divisions", "regions", "territories", "areas"}, allEntries = true)
    public Area updateArea(UUID id, Area area) {
        log.info("Updating area: {}", id);
        Area existing = getAreaById(id);
        
        if (!area.getName().equals(existing.getName())) {
            if (areaRepository.existsByRegionIdAndName(area.getRegionId(), area.getName())) {
                throw new RuntimeException("Area with name already exists in region: " + area.getName());
            }
        }
        
        existing.setName(area.getName());
        existing.setCode(area.getCode());
        existing.setDescription(area.getDescription());
        existing.setStatus(area.getStatus());
        existing.setIsActive(area.getIsActive());
        existing.setUpdatedBy(area.getUpdatedBy());
        
        return areaRepository.save(existing);
    }
    
    @Transactional
    @CacheEvict(value = {"divisions", "regions", "territories", "areas"}, allEntries = true)
    public void deleteArea(UUID id) {
        log.info("Deleting area: {}", id);
        Area area = getAreaById(id);
        
        // Check if area has territories
        if (!territoryRepository.findByAreaId(area.getId()).isEmpty()) {
            throw new RuntimeException("Cannot delete area with existing territories");
        }
        
        areaRepository.delete(area);
    }
}

