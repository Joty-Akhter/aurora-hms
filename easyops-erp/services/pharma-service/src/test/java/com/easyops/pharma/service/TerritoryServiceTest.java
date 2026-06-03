package com.easyops.pharma.service;

import com.easyops.pharma.client.InventoryClient;
import com.easyops.pharma.entity.Area;
import com.easyops.pharma.entity.Region;
import com.easyops.pharma.entity.Territory;
import com.easyops.pharma.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Phase 3: Unit tests for TerritoryService - warehouse auto-creation and rollback.
 */
@ExtendWith(MockitoExtension.class)
class TerritoryServiceTest {

    @Mock
    private DivisionRepository divisionRepository;
    @Mock
    private RegionRepository regionRepository;
    @Mock
    private TerritoryRepository territoryRepository;
    @Mock
    private AreaRepository areaRepository;
    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private TerritoryService territoryService;

    private UUID orgId;
    private UUID divisionId;
    private UUID regionId;
    private UUID areaId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        divisionId = UUID.randomUUID();
        regionId = UUID.randomUUID();
        areaId = UUID.randomUUID();
    }

    @Test
    void createTerritory_createsWarehouseAndTerritory() {
        Area area = new Area();
        area.setId(areaId);
        area.setOrganizationId(orgId);
        area.setRegionId(regionId);
        area.setDivisionId(divisionId);
        area.setName("Test Area");

        Territory input = new Territory();
        input.setOrganizationId(orgId);
        input.setDivisionId(divisionId);
        input.setRegionId(regionId);
        input.setAreaId(areaId);
        input.setName("Dhanmondi");
        input.setCode("Dhanmondi-001");

        when(areaRepository.findById(areaId)).thenReturn(Optional.of(area));
        when(regionRepository.findById(regionId)).thenReturn(Optional.of(new Region()));
        when(territoryRepository.existsByAreaIdAndName(areaId, "Dhanmondi")).thenReturn(false);

        UUID warehouseId = UUID.randomUUID();
        Map<String, Object> warehouseResponse = new HashMap<>();
        warehouseResponse.put("id", warehouseId.toString());
        when(inventoryClient.createWarehouse(any())).thenReturn(warehouseResponse);

        Territory saved = new Territory();
        saved.setId(UUID.randomUUID());
        saved.setWarehouseId(warehouseId);
        saved.setName(input.getName());
        when(territoryRepository.save(any(Territory.class))).thenAnswer(inv -> {
            Territory t = inv.getArgument(0);
            t.setId(saved.getId());
            return t;
        });

        Territory result = territoryService.createTerritory(input);

        ArgumentCaptor<Map<String, Object>> warehouseCaptor = ArgumentCaptor.forClass(Map.class);
        verify(inventoryClient).createWarehouse(warehouseCaptor.capture());
        Map<String, Object> warehouseData = warehouseCaptor.getValue();
        assertThat(warehouseData.get("name")).isEqualTo("Dhanmondi-Warehouse");
        assertThat(warehouseData.get("code")).isEqualTo("Dhanmondi-001");
        assertThat(warehouseData.get("warehouseType")).isEqualTo("DISTRIBUTION");
        assertThat(warehouseData.get("status")).isEqualTo("OPERATIONAL");

        assertThat(result.getWarehouseId()).isEqualTo(warehouseId);
        verify(territoryRepository).save(any(Territory.class));
    }

    @Test
    void createTerritory_rollbackOnWarehouseFailure() {
        Area area = new Area();
        area.setId(areaId);
        area.setOrganizationId(orgId);
        area.setRegionId(regionId);
        area.setDivisionId(divisionId);

        Territory input = new Territory();
        input.setOrganizationId(orgId);
        input.setDivisionId(divisionId);
        input.setRegionId(regionId);
        input.setAreaId(areaId);
        input.setName("Test Territory");
        input.setCode("T-001");

        when(areaRepository.findById(areaId)).thenReturn(Optional.of(area));
        when(regionRepository.findById(regionId)).thenReturn(Optional.of(new Region()));
        when(territoryRepository.existsByAreaIdAndName(areaId, "Test Territory")).thenReturn(false);
        when(inventoryClient.createWarehouse(any())).thenThrow(new RuntimeException("Inventory service unavailable"));

        assertThatThrownBy(() -> territoryService.createTerritory(input))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create warehouse");

        verify(territoryRepository, never()).save(any());
    }

    @Test
    void createTerritory_throwsWhenAreaNotFound() {
        Territory input = new Territory();
        input.setOrganizationId(orgId);
        input.setAreaId(areaId);
        input.setRegionId(regionId);
        input.setDivisionId(divisionId);
        input.setName("Test");

        when(areaRepository.findById(areaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> territoryService.createTerritory(input))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Area not found");
        verify(inventoryClient, never()).createWarehouse(any());
    }

    @Test
    void createTerritory_throwsWhenDuplicateNameInArea() {
        Area area = new Area();
        area.setId(areaId);
        when(areaRepository.findById(areaId)).thenReturn(Optional.of(area));
        when(regionRepository.findById(regionId)).thenReturn(Optional.of(new Region()));
        when(territoryRepository.existsByAreaIdAndName(areaId, "Duplicate")).thenReturn(true);

        Territory input = new Territory();
        input.setOrganizationId(orgId);
        input.setAreaId(areaId);
        input.setRegionId(regionId);
        input.setDivisionId(divisionId);
        input.setName("Duplicate");

        assertThatThrownBy(() -> territoryService.createTerritory(input))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Territory with name already exists");
        verify(inventoryClient, never()).createWarehouse(any());
    }
}
