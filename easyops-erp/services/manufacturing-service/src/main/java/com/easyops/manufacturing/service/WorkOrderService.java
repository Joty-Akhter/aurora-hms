package com.easyops.manufacturing.service;

import com.easyops.manufacturing.entity.*;
import com.easyops.manufacturing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderOperationRepository workOrderOperationRepository;
    private final WorkOrderMaterialRepository workOrderMaterialRepository;
    private final BomService bomService;
    private final ProductRoutingService routingService;
    
    // Integration Services
    private final com.easyops.manufacturing.integration.InventoryIntegrationService inventoryIntegration;
    private final com.easyops.manufacturing.integration.AccountingIntegrationService accountingIntegration;
    private final com.easyops.manufacturing.integration.SalesIntegrationService salesIntegration;
    private final com.easyops.manufacturing.integration.PurchaseIntegrationService purchaseIntegration;

    // ==================== Work Order CRUD ====================

    @Transactional(readOnly = true)
    public List<WorkOrder> getAllWorkOrders(UUID organizationId) {
        return workOrderRepository.findByOrganizationId(organizationId);
    }

    @Transactional(readOnly = true)
    public Optional<WorkOrder> getWorkOrderById(UUID workOrderId) {
        return workOrderRepository.findById(workOrderId);
    }

    @Transactional(readOnly = true)
    public UUID getOrganizationIdForWorkOrder(UUID workOrderId) {
        return workOrderRepository.findById(workOrderId)
                .map(WorkOrder::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work order not found"));
    }

    @Transactional(readOnly = true)
    public UUID getOrganizationIdForWorkOrderMaterial(UUID materialId) {
        return workOrderMaterialRepository.findById(materialId)
                .map(WorkOrderMaterial::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work order material not found"));
    }

    @Transactional(readOnly = true)
    public UUID getOrganizationIdForWorkOrderOperation(UUID operationId) {
        return workOrderOperationRepository.findById(operationId)
                .map(WorkOrderOperation::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work order operation not found"));
    }

    @Transactional(readOnly = true)
    public Optional<WorkOrder> getWorkOrderByNumber(UUID organizationId, String workOrderNumber) {
        return workOrderRepository.findByOrganizationIdAndWorkOrderNumber(organizationId, workOrderNumber);
    }

    @Transactional(readOnly = true)
    public List<WorkOrder> getWorkOrdersByStatus(UUID organizationId, String status) {
        return workOrderRepository.findByOrganizationIdAndStatus(organizationId, status);
    }

    @Transactional(readOnly = true)
    public List<WorkOrder> getActiveWorkOrders(UUID organizationId) {
        return workOrderRepository.findActiveWorkOrders(organizationId);
    }

    @Transactional(readOnly = true)
    public List<WorkOrder> getOverdueWorkOrders(UUID organizationId) {
        return workOrderRepository.findOverdueWorkOrders(organizationId, LocalDateTime.now());
    }

    @Transactional
    public WorkOrder createWorkOrder(WorkOrder workOrder) {
        // Auto-generate work order number if not provided
        if (workOrder.getWorkOrderNumber() == null || workOrder.getWorkOrderNumber().isEmpty()) {
            workOrder.setWorkOrderNumber(generateWorkOrderNumber(workOrder.getOrganizationId()));
        }
        
        // Set defaults
        if (workOrder.getStatus() == null) {
            workOrder.setStatus("CREATED");
        }
        if (workOrder.getOrderType() == null) {
            workOrder.setOrderType("PRODUCTION");
        }
        if (workOrder.getPriority() == null) {
            workOrder.setPriority("MEDIUM");
        }
        
        WorkOrder saved = workOrderRepository.save(workOrder);
        
        // Generate operations from routing if BOM is specified
        if (saved.getBomId() != null) {
            generateOperationsFromRouting(saved);
            generateMaterialsFromBOM(saved);
        }
        
        log.info("Created work order: {} for product: {}", saved.getWorkOrderNumber(), saved.getProductId());
        return saved;
    }

    @Transactional
    public WorkOrder updateWorkOrder(UUID workOrderId, WorkOrder workOrder) {
        WorkOrder existing = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Work Order not found: " + workOrderId));
        
        existing.setProductCode(workOrder.getProductCode());
        existing.setProductName(workOrder.getProductName());
        existing.setQuantityPlanned(workOrder.getQuantityPlanned());
        existing.setPriority(workOrder.getPriority());
        existing.setPlannedStartDate(workOrder.getPlannedStartDate());
        existing.setPlannedEndDate(workOrder.getPlannedEndDate());
        existing.setSourceWarehouseId(workOrder.getSourceWarehouseId());
        existing.setTargetWarehouseId(workOrder.getTargetWarehouseId());
        existing.setNotes(workOrder.getNotes());
        existing.setSpecialInstructions(workOrder.getSpecialInstructions());
        existing.setUpdatedBy(workOrder.getUpdatedBy());
        
        WorkOrder updated = workOrderRepository.save(existing);
        log.info("Updated work order: {}", updated.getWorkOrderNumber());
        return updated;
    }

    @Transactional
    public void deleteWorkOrder(UUID workOrderId) {
        workOrderRepository.deleteById(workOrderId);
        log.info("Deleted work order: {}", workOrderId);
    }

    // ==================== Work Order Lifecycle ====================

    @Transactional
    public WorkOrder releaseWorkOrder(UUID workOrderId, UUID releasedBy) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Work Order not found: " + workOrderId));
        
        if (!"CREATED".equals(wo.getStatus())) {
            throw new RuntimeException("Only CREATED work orders can be released");
        }
        
        // Reserve materials
        reserveMaterials(workOrderId);
        
        wo.setStatus("RELEASED");
        wo.setReleasedBy(releasedBy);
        wo.setReleaseDate(LocalDateTime.now());
        
        WorkOrder released = workOrderRepository.save(wo);
        log.info("Released work order: {}", released.getWorkOrderNumber());
        return released;
    }

    @Transactional
    public WorkOrder startWorkOrder(UUID workOrderId, UUID userId) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Work Order not found: " + workOrderId));
        
        if (!"RELEASED".equals(wo.getStatus())) {
            throw new RuntimeException("Only RELEASED work orders can be started");
        }
        
        wo.setStatus("IN_PROGRESS");
        wo.setActualStartDate(LocalDateTime.now());
        
        WorkOrder started = workOrderRepository.save(wo);
        log.info("Started work order: {}", started.getWorkOrderNumber());
        return started;
    }

    @Transactional
    public WorkOrder completeWorkOrder(UUID workOrderId, UUID completedBy) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Work Order not found: " + workOrderId));
        
        if (!"IN_PROGRESS".equals(wo.getStatus())) {
            throw new RuntimeException("Only IN_PROGRESS work orders can be completed");
        }
        
        // Backflush materials if needed
        backflushMaterials(workOrderId);
        
        // Calculate final costs
        recalculateWorkOrderCosts(workOrderId);
        
        // Reload to get updated costs
        wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Work Order not found: " + workOrderId));
        
        // Integration: Receive finished goods to inventory
        if (wo.getQuantityCompleted() != null && wo.getQuantityCompleted().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal unitCost = wo.getQuantityCompleted().compareTo(BigDecimal.ZERO) > 0
                    ? wo.getTotalCost().divide(wo.getQuantityCompleted(), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            
            inventoryIntegration.receiveFinishedGoods(
                wo.getProductId(),
                wo.getQuantityCompleted(),
                wo.getTargetWarehouseId(),
                wo.getWorkOrderNumber(),
                unitCost,
                completedBy
            );
            
            log.info("Received {} units of finished goods to inventory", wo.getQuantityCompleted());
        }
        
        // Integration: Post finished goods completion to accounting
        accountingIntegration.postFinishedGoodsCompletion(
            wo.getWorkOrderNumber(),
            wo.getProductId(),
            wo.getQuantityCompleted() != null ? wo.getQuantityCompleted() : BigDecimal.ZERO,
            wo.getTotalCost() != null ? wo.getTotalCost() : BigDecimal.ZERO,
            wo.getOrganizationId()
        );
        
        // Integration: Post scrap cost if any
        if (wo.getQuantityScrapped() != null && wo.getQuantityScrapped().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal scrapCost = wo.getTotalCost()
                    .multiply(wo.getQuantityScrapped())
                    .divide(wo.getQuantityPlanned(), 2, RoundingMode.HALF_UP);
            
            accountingIntegration.postScrapCost(
                wo.getWorkOrderNumber(),
                wo.getProductId(),
                wo.getQuantityScrapped(),
                scrapCost,
                wo.getOrganizationId()
            );
        }
        
        // Integration: Notify sales if this was a make-to-order
        if ("SALES_ORDER".equals(wo.getSourceType()) && wo.getSourceReference() != null) {
            salesIntegration.notifyProductionComplete(
                wo.getWorkOrderNumber(),
                wo.getProductId(),
                wo.getQuantityCompleted() != null ? wo.getQuantityCompleted().doubleValue() : 0
            );
        }
        
        wo.setStatus("COMPLETED");
        wo.setActualEndDate(LocalDateTime.now());
        wo.setCompletedBy(completedBy);
        wo.setCompletionPercentage(BigDecimal.valueOf(100));
        
        WorkOrder completed = workOrderRepository.save(wo);
        log.info("Completed work order: {} with full integration", completed.getWorkOrderNumber());
        return completed;
    }

    @Transactional
    public WorkOrder closeWorkOrder(UUID workOrderId) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Work Order not found: " + workOrderId));
        
        if (!"COMPLETED".equals(wo.getStatus())) {
            throw new RuntimeException("Only COMPLETED work orders can be closed");
        }
        
        wo.setStatus("CLOSED");
        
        WorkOrder closed = workOrderRepository.save(wo);
        log.info("Closed work order: {}", closed.getWorkOrderNumber());
        return closed;
    }

    @Transactional
    public WorkOrder cancelWorkOrder(UUID workOrderId, String reason) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Work Order not found: " + workOrderId));
        
        // Release reserved materials
        unreserveMaterials(workOrderId);
        
        wo.setStatus("CANCELLED");
        wo.setNotes((wo.getNotes() != null ? wo.getNotes() + "\n" : "") + "Cancelled: " + reason);
        
        WorkOrder cancelled = workOrderRepository.save(wo);
        log.info("Cancelled work order: {}", cancelled.getWorkOrderNumber());
        return cancelled;
    }

    // ==================== Material Management ====================

    @Transactional
    public void reserveMaterials(UUID workOrderId) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Work Order not found: " + workOrderId));
        
        List<WorkOrderMaterial> materials = workOrderMaterialRepository.findByWorkOrderId(workOrderId);
        
        for (WorkOrderMaterial material : materials) {
            if ("PLANNED".equals(material.getStatus())) {
                // Integration: Check material availability in inventory
                boolean available = inventoryIntegration.checkMaterialAvailability(
                    material.getComponentId(),
                    material.getQuantityRequired(),
                    wo.getSourceWarehouseId()
                );
                
                if (available) {
                    // Integration: Reserve material in inventory
                    inventoryIntegration.reserveMaterial(
                        material.getComponentId(),
                        material.getQuantityRequired(),
                        wo.getSourceWarehouseId(),
                        "WORK_ORDER",
                        wo.getWorkOrderNumber()
                    );
                    
                    material.setQuantityReserved(material.getQuantityRequired());
                    material.setReservedDate(LocalDateTime.now());
                    material.setStatus("RESERVED");
                } else {
                    // Integration: Create purchase requisition for shortage (MRP)
                    log.warn("Material {} not available - creating purchase requisition", material.getComponentCode());
                    purchaseIntegration.createPurchaseRequisition(
                        material.getComponentId(),
                        material.getComponentCode(),
                        material.getQuantityRequired(),
                        material.getUom(),
                        wo.getPlannedStartDate(),
                        wo.getWorkOrderNumber(),
                        wo.getOrganizationId(),
                        wo.getCreatedBy()
                    );
                    
                    material.setStatus("SHORTAGE");
                }
                
                workOrderMaterialRepository.save(material);
            }
        }
        
        log.info("Reserved materials for work order: {}", workOrderId);
    }

    @Transactional
    public void unreserveMaterials(UUID workOrderId) {
        List<WorkOrderMaterial> materials = workOrderMaterialRepository.findByWorkOrderIdAndStatus(workOrderId, "RESERVED");
        
        for (WorkOrderMaterial material : materials) {
            material.setQuantityReserved(BigDecimal.ZERO);
            material.setReservedDate(null);
            material.setStatus("PLANNED");
            workOrderMaterialRepository.save(material);
        }
        
        log.info("Unreserved materials for work order: {}", workOrderId);
    }

    @Transactional
    public WorkOrderMaterial issueMaterial(UUID materialId, BigDecimal quantity, UUID issuedBy) {
        WorkOrderMaterial material = workOrderMaterialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Material not found: " + materialId));
        
        if (!"RESERVED".equals(material.getStatus())) {
            throw new RuntimeException("Only RESERVED materials can be issued");
        }
        
        WorkOrder wo = material.getWorkOrder();
        
        // Integration: Issue material from inventory (reduce stock)
        inventoryIntegration.issueMaterial(
            material.getComponentId(),
            quantity,
            wo.getSourceWarehouseId(),
            "WORK_ORDER",
            wo.getWorkOrderNumber(),
            issuedBy
        );
        
        // Integration: Post material issuance to accounting
        if (material.getUnitCost() != null) {
            accountingIntegration.postMaterialIssuance(
                wo.getWorkOrderNumber(),
                material.getComponentId(),
                quantity,
                material.getUnitCost(),
                wo.getOrganizationId()
            );
        }
        
        material.setQuantityIssued(material.getQuantityIssued().add(quantity));
        material.setIssuedDate(LocalDateTime.now());
        material.setIssuedBy(issuedBy);
        material.setStatus("ISSUED");
        
        WorkOrderMaterial issued = workOrderMaterialRepository.save(material);
        log.info("Issued material: {} quantity: {} with inventory integration", issued.getComponentCode(), quantity);
        return issued;
    }

    @Transactional
    public WorkOrderMaterial consumeMaterial(UUID materialId, BigDecimal quantity) {
        WorkOrderMaterial material = workOrderMaterialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Material not found: " + materialId));
        
        material.setQuantityConsumed(material.getQuantityConsumed().add(quantity));
        material.setConsumedDate(LocalDateTime.now());
        material.setStatus("CONSUMED");
        
        // Update total cost
        if (material.getUnitCost() != null) {
            material.setTotalCost(material.getQuantityConsumed().multiply(material.getUnitCost()));
        }
        
        WorkOrderMaterial consumed = workOrderMaterialRepository.save(material);
        
        // Update work order material cost
        recalculateWorkOrderCosts(material.getWorkOrder().getWorkOrderId());
        
        log.info("Consumed material: {} quantity: {}", consumed.getComponentCode(), quantity);
        return consumed;
    }

    @Transactional
    public void backflushMaterials(UUID workOrderId) {
        List<WorkOrderMaterial> materials = workOrderMaterialRepository.findByWorkOrderId(workOrderId);
        
        for (WorkOrderMaterial material : materials) {
            if (Boolean.TRUE.equals(material.getBackflush()) && "ISSUED".equals(material.getStatus())) {
                consumeMaterial(material.getMaterialId(), material.getQuantityIssued());
            }
        }
        
        log.info("Backflushed materials for work order: {}", workOrderId);
    }

    // ==================== Operation Management ====================

    @Transactional(readOnly = true)
    public List<WorkOrderOperation> getWorkOrderOperations(UUID workOrderId) {
        return workOrderOperationRepository.findByWorkOrderId(workOrderId);
    }

    @Transactional
    public WorkOrderOperation startOperation(UUID operationId, UUID userId) {
        WorkOrderOperation operation = workOrderOperationRepository.findById(operationId)
                .orElseThrow(() -> new RuntimeException("Operation not found: " + operationId));
        
        operation.setStatus("IN_PROGRESS");
        operation.setActualStart(LocalDateTime.now());
        operation.setAssignedTo(userId);
        
        WorkOrderOperation started = workOrderOperationRepository.save(operation);
        
        // Update work order progress
        updateWorkOrderProgress(operation.getWorkOrder().getWorkOrderId());
        
        log.info("Started operation: {} for work order: {}", 
                started.getOperationCode(), started.getWorkOrder().getWorkOrderNumber());
        return started;
    }

    @Transactional
    public WorkOrderOperation completeOperation(UUID operationId, BigDecimal quantityCompleted, UUID completedBy) {
        WorkOrderOperation operation = workOrderOperationRepository.findById(operationId)
                .orElseThrow(() -> new RuntimeException("Operation not found: " + operationId));
        
        operation.setStatus("COMPLETED");
        operation.setActualEnd(LocalDateTime.now());
        operation.setQuantityCompleted(quantityCompleted);
        operation.setCompletedBy(completedBy);
        
        // Calculate actual time
        if (operation.getActualStart() != null && operation.getActualEnd() != null) {
            long minutes = java.time.Duration.between(operation.getActualStart(), operation.getActualEnd()).toMinutes();
            operation.setTotalTimeActual(BigDecimal.valueOf(minutes));
        }
        
        WorkOrderOperation completed = workOrderOperationRepository.save(operation);
        
        // Update work order progress
        updateWorkOrderProgress(operation.getWorkOrder().getWorkOrderId());
        
        log.info("Completed operation: {}", completed.getOperationCode());
        return completed;
    }

    // ==================== Progress & Cost Calculations ====================

    @Transactional
    public void updateWorkOrderProgress(UUID workOrderId) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Work Order not found: " + workOrderId));
        
        long totalOps = workOrderOperationRepository.countByWorkOrderId(workOrderId);
        long completedOps = workOrderOperationRepository.countCompletedOperations(workOrderId);
        
        wo.setTotalOperations((int) totalOps);
        wo.setOperationsCompleted((int) completedOps);
        
        if (totalOps > 0) {
            BigDecimal percentage = BigDecimal.valueOf(completedOps)
                    .divide(BigDecimal.valueOf(totalOps), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            wo.setCompletionPercentage(percentage);
        }
        
        workOrderRepository.save(wo);
    }

    @Transactional
    public void recalculateWorkOrderCosts(UUID workOrderId) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Work Order not found: " + workOrderId));
        
        // Material Cost
        BigDecimal materialCost = workOrderMaterialRepository.calculateTotalMaterialCost(workOrderId);
        wo.setMaterialCost(materialCost != null ? materialCost : BigDecimal.ZERO);
        
        // Labor Cost (sum from operations)
        List<WorkOrderOperation> operations = workOrderOperationRepository.findByWorkOrderId(workOrderId);
        BigDecimal laborCost = operations.stream()
                .map(op -> op.getLaborCost() != null ? op.getLaborCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        wo.setLaborCost(laborCost);
        
        // Overhead Cost (sum from operations)
        BigDecimal overheadCost = operations.stream()
                .map(op -> op.getOverheadCost() != null ? op.getOverheadCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        wo.setOverheadCost(overheadCost);
        
        // Total Cost
        wo.setTotalCost(wo.getMaterialCost().add(wo.getLaborCost()).add(wo.getOverheadCost()));
        
        workOrderRepository.save(wo);
    }

    // ==================== Helper Methods ====================

    private void generateOperationsFromRouting(WorkOrder workOrder) {
        List<ProductRouting> routings = routingService.getActiveRoutingsByProduct(workOrder.getProductId());
        
        for (ProductRouting routing : routings) {
            WorkOrderOperation operation = WorkOrderOperation.builder()
                    .workOrder(workOrder)
                    .organizationId(workOrder.getOrganizationId())
                    .routingOperationId(routing.getRoutingId())
                    .operationSequence(routing.getOperationSequence())
                    .operationCode(routing.getOperationCode())
                    .operationName(routing.getOperationName())
                    .workCenterCode(routing.getWorkCenterCode())
                    .workCenterName(routing.getWorkCenterName())
                    .setupTimePlanned(routing.getSetupTime())
                    .runTimePlanned(routing.getRunTimePerUnit().multiply(workOrder.getQuantityPlanned()))
                    .teardownTimePlanned(routing.getTeardownTime())
                    .qualityCheckRequired(routing.getQualityCheckRequired())
                    .instructions(routing.getInstructions())
                    .status("PENDING")
                    .build();
            
            workOrderOperationRepository.save(operation);
        }
    }

    private void generateMaterialsFromBOM(WorkOrder workOrder) {
        Map<String, Object> bomExplosion = bomService.explodeBom(workOrder.getBomId(), workOrder.getQuantityPlanned());
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> components = (List<Map<String, Object>>) bomExplosion.get("components");
        
        for (Map<String, Object> component : components) {
            WorkOrderMaterial material = WorkOrderMaterial.builder()
                    .workOrder(workOrder)
                    .organizationId(workOrder.getOrganizationId())
                    .componentId((UUID) component.get("componentId"))
                    .componentCode((String) component.get("componentCode"))
                    .componentName((String) component.get("componentName"))
                    .quantityRequired((BigDecimal) component.get("requiredQuantity"))
                    .uom((String) component.get("uom"))
                    .unitCost((BigDecimal) component.get("unitCost"))
                    .status("PLANNED")
                    .backflush(false)
                    .build();
            
            if (material.getUnitCost() != null) {
                material.setTotalCost(material.getQuantityRequired().multiply(material.getUnitCost()));
            }
            
            workOrderMaterialRepository.save(material);
        }
    }

    private String generateWorkOrderNumber(UUID organizationId) {
        long count = workOrderRepository.countByOrganization(organizationId);
        return String.format("WO-%06d", count + 1);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats(UUID organizationId) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalWorkOrders", workOrderRepository.countByOrganization(organizationId));
        stats.put("createdWorkOrders", workOrderRepository.countByOrganizationAndStatus(organizationId, "CREATED"));
        stats.put("releasedWorkOrders", workOrderRepository.countByOrganizationAndStatus(organizationId, "RELEASED"));
        stats.put("inProgressWorkOrders", workOrderRepository.countByOrganizationAndStatus(organizationId, "IN_PROGRESS"));
        stats.put("completedWorkOrders", workOrderRepository.countByOrganizationAndStatus(organizationId, "COMPLETED"));
        stats.put("overdueWorkOrders", workOrderRepository.findOverdueWorkOrders(organizationId, LocalDateTime.now()).size());
        
        return stats;
    }
}

