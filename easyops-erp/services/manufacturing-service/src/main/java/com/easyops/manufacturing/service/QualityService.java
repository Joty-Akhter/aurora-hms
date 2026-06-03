package com.easyops.manufacturing.service;

import com.easyops.manufacturing.entity.QualityInspection;
import com.easyops.manufacturing.entity.QualityInspectionItem;
import com.easyops.manufacturing.repository.QualityInspectionItemRepository;
import com.easyops.manufacturing.repository.QualityInspectionRepository;
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
public class QualityService {

    private final QualityInspectionRepository qualityInspectionRepository;
    private final QualityInspectionItemRepository qualityInspectionItemRepository;

    @Transactional(readOnly = true)
    public List<QualityInspection> getAllInspections(UUID organizationId) {
        return qualityInspectionRepository.findByOrganizationId(organizationId);
    }

    @Transactional(readOnly = true)
    public Optional<QualityInspection> getInspectionById(UUID inspectionId) {
        return qualityInspectionRepository.findById(inspectionId);
    }

    @Transactional(readOnly = true)
    public UUID getOrganizationIdForInspectionItem(UUID itemId) {
        return qualityInspectionItemRepository.findById(itemId)
                .map(QualityInspectionItem::getOrganizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inspection item not found"));
    }

    @Transactional(readOnly = true)
    public List<QualityInspection> getInspectionsByStatus(UUID organizationId, String status) {
        return qualityInspectionRepository.findByOrganizationIdAndStatus(organizationId, status);
    }

    @Transactional(readOnly = true)
    public List<QualityInspection> getInspectionsByProduct(UUID organizationId, UUID productId) {
        return qualityInspectionRepository.findByOrganizationIdAndProductId(organizationId, productId);
    }

    @Transactional(readOnly = true)
    public List<QualityInspection> getInspectionsByWorkOrder(UUID workOrderId) {
        return qualityInspectionRepository.findByWorkOrderId(workOrderId);
    }

    @Transactional(readOnly = true)
    public List<QualityInspection> getPendingInspections(UUID organizationId) {
        return qualityInspectionRepository.findPendingInspections(organizationId);
    }

    @Transactional
    public QualityInspection createInspection(QualityInspection inspection) {
        if (inspection.getInspectionNumber() == null || inspection.getInspectionNumber().isEmpty()) {
            inspection.setInspectionNumber(generateInspectionNumber(inspection.getOrganizationId()));
        }
        
        if (inspection.getStatus() == null) {
            inspection.setStatus("PENDING");
        }
        
        if (inspection.getInspectionDate() == null) {
            inspection.setInspectionDate(LocalDateTime.now());
        }
        
        QualityInspection saved = qualityInspectionRepository.save(inspection);
        log.info("Created quality inspection: {}", saved.getInspectionNumber());
        return saved;
    }

    @Transactional
    public QualityInspection updateInspection(UUID inspectionId, QualityInspection inspection) {
        QualityInspection existing = qualityInspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new RuntimeException("Inspection not found: " + inspectionId));
        
        existing.setInspectionType(inspection.getInspectionType());
        existing.setInspectionStage(inspection.getInspectionStage());
        existing.setInspectorName(inspection.getInspectorName());
        existing.setStatus(inspection.getStatus());
        existing.setOverallResult(inspection.getOverallResult());
        existing.setSampleSize(inspection.getSampleSize());
        existing.setSampleMethod(inspection.getSampleMethod());
        existing.setQuantityInspected(inspection.getQuantityInspected());
        existing.setQuantityPassed(inspection.getQuantityPassed());
        existing.setQuantityFailed(inspection.getQuantityFailed());
        existing.setInspectionCriteria(inspection.getInspectionCriteria());
        existing.setNotes(inspection.getNotes());
        existing.setCorrectiveActions(inspection.getCorrectiveActions());
        existing.setUpdatedBy(inspection.getUpdatedBy());
        
        // Recalculate metrics
        recalculateInspectionMetrics(existing);
        
        QualityInspection updated = qualityInspectionRepository.save(existing);
        log.info("Updated quality inspection: {}", updated.getInspectionNumber());
        return updated;
    }

    @Transactional
    public void deleteInspection(UUID inspectionId) {
        qualityInspectionRepository.deleteById(inspectionId);
        log.info("Deleted quality inspection: {}", inspectionId);
    }

    @Transactional
    public QualityInspection completeInspection(UUID inspectionId, String overallResult, UUID completedBy) {
        QualityInspection inspection = qualityInspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new RuntimeException("Inspection not found: " + inspectionId));
        
        inspection.setStatus("COMPLETED");
        inspection.setOverallResult(overallResult);
        inspection.setUpdatedBy(completedBy);
        
        // Recalculate pass rate and defect counts
        recalculateInspectionMetrics(inspection);
        
        QualityInspection completed = qualityInspectionRepository.save(inspection);
        log.info("Completed quality inspection: {} with result: {}", 
                completed.getInspectionNumber(), overallResult);
        return completed;
    }

    // Inspection Items
    @Transactional(readOnly = true)
    public List<QualityInspectionItem> getInspectionItems(UUID inspectionId) {
        return qualityInspectionItemRepository.findByInspectionId(inspectionId);
    }

    @Transactional
    public QualityInspectionItem addInspectionItem(QualityInspectionItem item) {
        // Calculate deviation if measured value is provided
        if (item.getMeasuredValue() != null && item.getSpecificationTarget() != null) {
            item.setDeviation(item.getMeasuredValue().subtract(item.getSpecificationTarget()));
            
            if (item.getSpecificationTarget().compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal deviationPct = item.getDeviation()
                        .divide(item.getSpecificationTarget(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                item.setDeviationPercentage(deviationPct);
            }
        }
        
        // Determine pass/fail
        if (item.getMeasuredValue() != null) {
            boolean pass = true;
            if (item.getSpecificationMin() != null && item.getMeasuredValue().compareTo(item.getSpecificationMin()) < 0) {
                pass = false;
            }
            if (item.getSpecificationMax() != null && item.getMeasuredValue().compareTo(item.getSpecificationMax()) > 0) {
                pass = false;
            }
            item.setPassFail(pass ? "PASS" : "FAIL");
        }
        
        QualityInspectionItem saved = qualityInspectionItemRepository.save(item);
        
        // Update inspection defect counts
        updateInspectionDefectCounts(item.getQualityInspection().getInspectionId());
        
        log.info("Added inspection item for parameter: {}", saved.getParameterName());
        return saved;
    }

    @Transactional
    public QualityInspectionItem updateInspectionItem(UUID itemId, QualityInspectionItem item) {
        QualityInspectionItem existing = qualityInspectionItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Inspection item not found: " + itemId));
        
        existing.setParameterName(item.getParameterName());
        existing.setParameterType(item.getParameterType());
        existing.setSpecificationMin(item.getSpecificationMin());
        existing.setSpecificationMax(item.getSpecificationMax());
        existing.setSpecificationTarget(item.getSpecificationTarget());
        existing.setMeasuredValue(item.getMeasuredValue());
        existing.setMeasuredText(item.getMeasuredText());
        existing.setPassFail(item.getPassFail());
        existing.setNotes(item.getNotes());
        
        QualityInspectionItem updated = qualityInspectionItemRepository.save(existing);
        
        // Update inspection defect counts
        updateInspectionDefectCounts(existing.getQualityInspection().getInspectionId());
        
        log.info("Updated inspection item: {}", updated.getItemId());
        return updated;
    }

    @Transactional
    public void deleteInspectionItem(UUID itemId) {
        QualityInspectionItem item = qualityInspectionItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Inspection item not found: " + itemId));
        
        UUID inspectionId = item.getQualityInspection().getInspectionId();
        qualityInspectionItemRepository.deleteById(itemId);
        
        // Update inspection defect counts
        updateInspectionDefectCounts(inspectionId);
        
        log.info("Deleted inspection item: {}", itemId);
    }

    // Helper Methods
    private void recalculateInspectionMetrics(QualityInspection inspection) {
        if (inspection.getQuantityInspected() != null && 
            inspection.getQuantityInspected().compareTo(BigDecimal.ZERO) > 0) {
            
            // Calculate pass rate
            if (inspection.getQuantityPassed() != null) {
                BigDecimal passRate = inspection.getQuantityPassed()
                        .divide(inspection.getQuantityInspected(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                inspection.setPassRate(passRate.setScale(2, RoundingMode.HALF_UP));
            }
            
            // Calculate defect rate
            if (inspection.getDefectsFound() != null) {
                BigDecimal defectRate = BigDecimal.valueOf(inspection.getDefectsFound())
                        .divide(inspection.getQuantityInspected(), 4, RoundingMode.HALF_UP);
                inspection.setDefectRate(defectRate.setScale(2, RoundingMode.HALF_UP));
            }
        }
    }

    private void updateInspectionDefectCounts(UUID inspectionId) {
        List<QualityInspectionItem> items = qualityInspectionItemRepository.findByInspectionId(inspectionId);
        
        int totalDefects = 0;
        int criticalDefects = 0;
        int majorDefects = 0;
        int minorDefects = 0;
        
        for (QualityInspectionItem item : items) {
            if ("FAIL".equals(item.getPassFail())) {
                totalDefects++;
                if (Boolean.TRUE.equals(item.getIsCritical())) {
                    criticalDefects++;
                } else if ("MAJOR".equalsIgnoreCase(item.getParameterType())) {
                    majorDefects++;
                } else {
                    minorDefects++;
                }
            }
        }
        
        QualityInspection inspection = qualityInspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new RuntimeException("Inspection not found: " + inspectionId));
        
        inspection.setDefectsFound(totalDefects);
        inspection.setCriticalDefects(criticalDefects);
        inspection.setMajorDefects(majorDefects);
        inspection.setMinorDefects(minorDefects);
        
        qualityInspectionRepository.save(inspection);
    }

    private String generateInspectionNumber(UUID organizationId) {
        long count = qualityInspectionRepository.countByOrganization(organizationId);
        return String.format("QI-%06d", count + 1);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getQualityDashboardStats(UUID organizationId) {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalInspections", qualityInspectionRepository.countByOrganization(organizationId));
        stats.put("passedInspections", qualityInspectionRepository.countByResult(organizationId, "PASS"));
        stats.put("failedInspections", qualityInspectionRepository.countByResult(organizationId, "FAIL"));
        stats.put("pendingInspections", qualityInspectionRepository.findPendingInspections(organizationId).size());
        
        return stats;
    }
}

