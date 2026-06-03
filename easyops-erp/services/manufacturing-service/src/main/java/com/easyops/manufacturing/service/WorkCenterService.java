package com.easyops.manufacturing.service;

import com.easyops.manufacturing.entity.EquipmentMaintenance;
import com.easyops.manufacturing.entity.WorkCenter;
import com.easyops.manufacturing.repository.EquipmentMaintenanceRepository;
import com.easyops.manufacturing.repository.WorkCenterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkCenterService {

    private final WorkCenterRepository workCenterRepository;
    private final EquipmentMaintenanceRepository equipmentMaintenanceRepository;

    // ==================== Work Center Operations ====================

    @Transactional(readOnly = true)
    public List<WorkCenter> getAllWorkCenters(UUID organizationId) {
        return workCenterRepository.findByOrganizationId(organizationId);
    }

    @Transactional(readOnly = true)
    public Optional<WorkCenter> getWorkCenterById(UUID workCenterId) {
        return workCenterRepository.findById(workCenterId);
    }

    @Transactional(readOnly = true)
    public Optional<WorkCenter> getWorkCenterByCode(UUID organizationId, String workCenterCode) {
        return workCenterRepository.findByOrganizationIdAndWorkCenterCode(organizationId, workCenterCode);
    }

    @Transactional(readOnly = true)
    public List<WorkCenter> getActiveWorkCenters(UUID organizationId) {
        return workCenterRepository.findActiveWorkCenters(organizationId);
    }

    @Transactional(readOnly = true)
    public List<WorkCenter> getWorkCentersByType(UUID organizationId, String type) {
        return workCenterRepository.findByOrganizationIdAndWorkCenterType(organizationId, type);
    }

    @Transactional
    public WorkCenter createWorkCenter(WorkCenter workCenter) {
        WorkCenter saved = workCenterRepository.save(workCenter);
        log.info("Created work center: {}", saved.getWorkCenterCode());
        return saved;
    }

    @Transactional
    public WorkCenter updateWorkCenter(UUID workCenterId, WorkCenter workCenter) {
        WorkCenter existing = workCenterRepository.findById(workCenterId)
                .orElseThrow(() -> new RuntimeException("Work center not found: " + workCenterId));
        
        existing.setWorkCenterName(workCenter.getWorkCenterName());
        existing.setWorkCenterType(workCenter.getWorkCenterType());
        existing.setCategory(workCenter.getCategory());
        existing.setLocation(workCenter.getLocation());
        existing.setDepartment(workCenter.getDepartment());
        existing.setCapacityPerHour(workCenter.getCapacityPerHour());
        existing.setCapacityUom(workCenter.getCapacityUom());
        existing.setNumberOfMachines(workCenter.getNumberOfMachines());
        existing.setNumberOfOperators(workCenter.getNumberOfOperators());
        existing.setShiftsPerDay(workCenter.getShiftsPerDay());
        existing.setHoursPerShift(workCenter.getHoursPerShift());
        existing.setWorkingDaysPerWeek(workCenter.getWorkingDaysPerWeek());
        existing.setEfficiencyPercentage(workCenter.getEfficiencyPercentage());
        existing.setUtilizationPercentage(workCenter.getUtilizationPercentage());
        existing.setOeeTarget(workCenter.getOeeTarget());
        existing.setStatus(workCenter.getStatus());
        existing.setCostPerHour(workCenter.getCostPerHour());
        existing.setSetupCost(workCenter.getSetupCost());
        existing.setOverheadRate(workCenter.getOverheadRate());
        existing.setDescription(workCenter.getDescription());
        existing.setNotes(workCenter.getNotes());
        existing.setUpdatedBy(workCenter.getUpdatedBy());
        
        WorkCenter updated = workCenterRepository.save(existing);
        log.info("Updated work center: {}", updated.getWorkCenterCode());
        return updated;
    }

    @Transactional
    public void deleteWorkCenter(UUID workCenterId) {
        workCenterRepository.deleteById(workCenterId);
        log.info("Deleted work center: {}", workCenterId);
    }

    @Transactional
    public WorkCenter updateWorkCenterStatus(UUID workCenterId, String status) {
        WorkCenter wc = workCenterRepository.findById(workCenterId)
                .orElseThrow(() -> new RuntimeException("Work center not found: " + workCenterId));
        
        wc.setStatus(status);
        
        WorkCenter updated = workCenterRepository.save(wc);
        log.info("Updated work center: {} status to: {}", updated.getWorkCenterCode(), status);
        return updated;
    }

    // ==================== Equipment Maintenance Operations ====================

    @Transactional(readOnly = true)
    public List<EquipmentMaintenance> getAllMaintenance(UUID organizationId) {
        return equipmentMaintenanceRepository.findByOrganizationId(organizationId);
    }

    @Transactional(readOnly = true)
    public Optional<EquipmentMaintenance> getMaintenanceById(UUID maintenanceId) {
        return equipmentMaintenanceRepository.findById(maintenanceId);
    }

    @Transactional(readOnly = true)
    public List<EquipmentMaintenance> getMaintenanceByWorkCenter(UUID workCenterId) {
        return equipmentMaintenanceRepository.findByWorkCenterId(workCenterId);
    }

    @Transactional(readOnly = true)
    public List<EquipmentMaintenance> getOverdueMaintenance() {
        return equipmentMaintenanceRepository.findOverdueMaintenance(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<EquipmentMaintenance> getOverdueMaintenance(UUID organizationId) {
        return equipmentMaintenanceRepository.findOverdueMaintenanceByOrganization(organizationId, LocalDateTime.now());
    }

    @Transactional
    public EquipmentMaintenance createMaintenance(EquipmentMaintenance maintenance) {
        if (maintenance.getMaintenanceNumber() == null || maintenance.getMaintenanceNumber().isEmpty()) {
            maintenance.setMaintenanceNumber(generateMaintenanceNumber(maintenance.getOrganizationId()));
        }
        
        if (maintenance.getStatus() == null) {
            maintenance.setStatus("SCHEDULED");
        }
        
        if (maintenance.getPriority() == null) {
            maintenance.setPriority("MEDIUM");
        }
        
        EquipmentMaintenance saved = equipmentMaintenanceRepository.save(maintenance);
        log.info("Created maintenance: {} for work center: {}", 
                saved.getMaintenanceNumber(), saved.getWorkCenterId());
        return saved;
    }

    @Transactional
    public EquipmentMaintenance updateMaintenance(UUID maintenanceId, EquipmentMaintenance maintenance) {
        EquipmentMaintenance existing = equipmentMaintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new RuntimeException("Maintenance not found: " + maintenanceId));
        
        existing.setMaintenanceType(maintenance.getMaintenanceType());
        existing.setPriority(maintenance.getPriority());
        existing.setStatus(maintenance.getStatus());
        existing.setScheduledDate(maintenance.getScheduledDate());
        existing.setScheduledDurationHours(maintenance.getScheduledDurationHours());
        existing.setDescription(maintenance.getDescription());
        existing.setWorkPerformed(maintenance.getWorkPerformed());
        existing.setPartsReplaced(maintenance.getPartsReplaced());
        existing.setFindings(maintenance.getFindings());
        existing.setRecommendations(maintenance.getRecommendations());
        existing.setTechnicianName(maintenance.getTechnicianName());
        existing.setLaborCost(maintenance.getLaborCost());
        existing.setPartsCost(maintenance.getPartsCost());
        existing.setDowntimeHours(maintenance.getDowntimeHours());
        existing.setProductionLoss(maintenance.getProductionLoss());
        existing.setFollowUpRequired(maintenance.getFollowUpRequired());
        existing.setFollowUpNotes(maintenance.getFollowUpNotes());
        existing.setUpdatedBy(maintenance.getUpdatedBy());
        
        // Calculate total cost
        if (existing.getLaborCost() != null || existing.getPartsCost() != null) {
            existing.setTotalCost(
                (existing.getLaborCost() != null ? existing.getLaborCost() : BigDecimal.ZERO)
                .add(existing.getPartsCost() != null ? existing.getPartsCost() : BigDecimal.ZERO)
            );
        }
        
        EquipmentMaintenance updated = equipmentMaintenanceRepository.save(existing);
        log.info("Updated maintenance: {}", updated.getMaintenanceNumber());
        return updated;
    }

    @Transactional
    public void deleteMaintenance(UUID maintenanceId) {
        equipmentMaintenanceRepository.deleteById(maintenanceId);
        log.info("Deleted maintenance: {}", maintenanceId);
    }

    @Transactional
    public EquipmentMaintenance startMaintenance(UUID maintenanceId, UUID userId) {
        EquipmentMaintenance maintenance = equipmentMaintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new RuntimeException("Maintenance not found: " + maintenanceId));
        
        maintenance.setStatus("IN_PROGRESS");
        maintenance.setActualStartDate(LocalDateTime.now());
        maintenance.setAssignedTo(userId);
        
        // Update work center status to MAINTENANCE
        WorkCenter wc = workCenterRepository.findById(maintenance.getWorkCenterId())
                .orElseThrow(() -> new RuntimeException("Work center not found"));
        wc.setStatus("MAINTENANCE");
        workCenterRepository.save(wc);
        
        EquipmentMaintenance started = equipmentMaintenanceRepository.save(maintenance);
        log.info("Started maintenance: {}", started.getMaintenanceNumber());
        return started;
    }

    @Transactional
    public EquipmentMaintenance completeMaintenance(UUID maintenanceId, UUID completedBy) {
        EquipmentMaintenance maintenance = equipmentMaintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new RuntimeException("Maintenance not found: " + maintenanceId));
        
        maintenance.setStatus("COMPLETED");
        maintenance.setActualEndDate(LocalDateTime.now());
        maintenance.setCompletedBy(completedBy);
        
        // Calculate actual duration
        if (maintenance.getActualStartDate() != null && maintenance.getActualEndDate() != null) {
            long hours = java.time.Duration.between(
                maintenance.getActualStartDate(), 
                maintenance.getActualEndDate()
            ).toHours();
            maintenance.setActualDurationHours(BigDecimal.valueOf(hours));
        }
        
        // Update work center - back to AVAILABLE
        WorkCenter wc = workCenterRepository.findById(maintenance.getWorkCenterId())
                .orElseThrow(() -> new RuntimeException("Work center not found"));
        wc.setStatus("AVAILABLE");
        wc.setLastMaintenanceDate(LocalDateTime.now());
        
        // Schedule next maintenance if frequency is set
        if (wc.getMaintenanceFrequencyDays() != null) {
            wc.setNextMaintenanceDate(LocalDateTime.now().plusDays(wc.getMaintenanceFrequencyDays()));
        }
        
        workCenterRepository.save(wc);
        
        EquipmentMaintenance completed = equipmentMaintenanceRepository.save(maintenance);
        log.info("Completed maintenance: {}", completed.getMaintenanceNumber());
        return completed;
    }

    private String generateMaintenanceNumber(UUID organizationId) {
        long count = equipmentMaintenanceRepository.countByOrganization(organizationId);
        return String.format("MAINT-%06d", count + 1);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getWorkCenterDashboardStats(UUID organizationId) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalWorkCenters", workCenterRepository.countByOrganization(organizationId));
        stats.put("availableWorkCenters", workCenterRepository.countAvailable(organizationId));
        stats.put("maintenanceDue", workCenterRepository.findMaintenanceDue(organizationId, LocalDateTime.now()).size());
        stats.put("overdueMaintenance", equipmentMaintenanceRepository.findOverdueMaintenance(LocalDateTime.now()).size());
        
        return stats;
    }
}

